#!/usr/bin/env python3
"""Fit approximate camera-ray -> reference-ground transforms offline, never app settings.

Requires desktop numpy/scipy. Input JSON:
{"length":5.2, "width":2.0, "source_lane_pixels":1280,
 "points":[{"camera":0,"uv":[0.4,0.6],"ground":[-0.8,3.0]}, ...]}

Roles: front/rear/left/right. UV is within an original uncorrected source lane.
Ground X is right, Y forward. A perspective-corrected factory phone photo can
provide APPROXIMATE visual reference coordinates. Its scale, the estimated lens
and camera-source assignment remain assumptions. Remove controls, false matches
and regions blended from a different camera. Keep owner images/results private.

python tools/fit_bird_eye_reference.py POINTS.json RESULT.json
"""
import argparse
import json
from pathlib import Path

import numpy as np
from scipy.optimize import least_squares

ROLES = ("front", "rear", "left", "right")


def rays_from_uv(uv, lens=(190, .5, .5)):
    xy = 2 * (np.asarray(uv, dtype=float) - lens[1:])
    radius = np.linalg.norm(xy, axis=1)
    angle = radius * np.deg2rad(lens[0] / 2)
    scale = np.sin(angle) / np.maximum(radius, 1e-12)
    return np.column_stack((xy * scale[:, None], np.cos(angle)))


def uv_from_rays(rays, lens=(190, .5, .5)):
    radial = np.linalg.norm(rays[:, :2], axis=1)
    radius = np.arctan2(radial, rays[:, 2]) / np.deg2rad(lens[0] / 2)
    return np.asarray(lens[1:]) + .5 * rays[:, :2] * (radius / np.maximum(radial, 1e-12))[:, None]


def transform(matrix, rays):
    points = rays @ matrix.T
    if np.any(np.abs(points[:, 2]) < 1e-8):
        raise ValueError("Transform crosses its projective horizon")
    return points[:, :2] / points[:, 2, None]


def solve_transform(rays, ground):
    """DLT followed by robust refinement. No measured camera pose is claimed."""
    rows = []
    for ray, (x, y) in zip(rays, ground):
        rows.extend((np.r_[ray, np.zeros(3), -x*ray], np.r_[np.zeros(3), ray, -y*ray]))
    system = np.array(rows)
    _, singular, vh = np.linalg.svd(system, full_matrices=True)
    if len(singular) < 8 or np.linalg.matrix_rank(system, tol=singular[0]*1e-6) < 8:
        raise ValueError("Degenerate correspondence layout")
    initial = vh[-1].reshape(3, 3)
    if abs(initial[2, 2]) < 1e-6:
        raise ValueError("Unstable transform normalization")
    initial /= initial[2, 2]

    def residual(parameters):
        matrix = np.r_[parameters, 1.].reshape(3, 3)
        points = rays @ matrix.T
        denominator = points[:, 2].copy()
        small = np.abs(denominator) < 1e-8
        denominator[small] = np.where(denominator[small] < 0, -1e-8, 1e-8)
        return (points[:, :2] / denominator[:, None] - ground).ravel()

    fit = least_squares(residual, initial.ravel()[:8], loss="soft_l1", f_scale=.02, max_nfev=500)
    matrix = np.r_[fit.x, 1.].reshape(3, 3)
    # Keep the training rays on their original hemisphere when mapping back.
    if np.median(rays @ matrix[2]) < 0:
        matrix = -matrix
    if not fit.success or np.linalg.cond(matrix) > 1e6:
        raise ValueError("Transform did not converge stably")
    return matrix


def default_ground(role, rays, length, width):
    x, y, yaw = ((0, length/2, 0), (0, -length/2, 180),
                 (-width/2, 0, 270), (width/2, 0, 90))[role]
    sy, co = np.sin(np.deg2rad(yaw)), np.cos(np.deg2rad(yaw))
    sp, cp = np.sin(np.deg2rad(35)), np.cos(np.deg2rad(35))
    # Baseline camera height is one metre, as in the Android defaults.
    right, down, forward = rays.T
    rx = right*co - down*sy*sp + forward*sy*cp
    ry = -right*sy - down*co*sp + forward*co*cp
    rz = -down*cp - forward*sp
    if np.any(rz >= -1e-8):
        raise ValueError("Baseline ray does not intersect the forward ground")
    return np.column_stack((x-rx/rz, y-ry/rz))


def fit_reference(data):
    length, width = float(data["length"]), float(data["width"])
    pixels = int(data["source_lane_pixels"])
    lens = np.array(data.get("estimated_lens", [190, .5, .5]), dtype=float)
    if (not (3 <= length <= 6.5 and 1.4 <= width <= 2.6 and 64 <= pixels <= 8192)
            or lens.shape != (3,) or not np.isfinite(lens).all()
            or not (120 <= lens[0] <= 220) or np.any(lens[1:] < .25) or np.any(lens[1:] > .75)):
        raise ValueError("Invalid geometry, source resolution or estimated lens")
    if not isinstance(data["points"], list) or any(
            type(p.get("camera")) is not int or not 0 <= p["camera"] <= 3 for p in data["points"]):
        raise ValueError("Invalid camera role")
    summaries = []
    rng = np.random.default_rng(360)
    for role, name in enumerate(ROLES):
        points = [p for p in data["points"] if p["camera"] == role]
        uv = np.array([p["uv"] for p in points], dtype=float).reshape(-1, 2)
        ground = np.array([p["ground"] for p in points], dtype=float).reshape(-1, 2)
        if (not np.isfinite(uv).all() or not np.isfinite(ground).all()
                or np.any(uv < 0) or np.any(uv > 1)):
            raise ValueError("Invalid correspondence points")
        # Repeated affine detections of one feature are not independent evidence.
        representatives = []
        for idx in range(len(points)):
            if not any(np.linalg.norm((uv[idx]-uv[j])*pixels) < 12 for j in representatives):
                representatives.append(idx)
        uv, ground = uv[representatives], ground[representatives]
        summary = {"role": name, "distinct_features": len(uv), "status": "insufficient_correspondences"}
        if len(uv) >= 8:
            spread = np.linalg.svd(uv-uv.mean(axis=0), compute_uv=False)
            if spread[-1] < .05 * spread[0]:
                summary["status"] = "degenerate_layout"
                summaries.append(summary)
                continue
            order = rng.permutation(len(uv))
            test, train = order[:max(2, len(uv)//5)], order[max(2, len(uv)//5):]
            rays = rays_from_uv(uv, lens)
            try:
                matrix = solve_transform(rays[train], ground[train])
                inverse = np.linalg.inv(matrix)
                predicted_rays = np.column_stack((ground[test], np.ones(len(test)))) @ inverse.T
                errors = np.linalg.norm((uv_from_rays(predicted_rays, lens)-uv[test])*pixels, axis=1)
                summary.update(status="candidate_only", camera_ray_to_reference_ground=matrix.tolist(),
                               heldout_features=len(test), heldout_source_median_px=float(np.median(errors)),
                               heldout_source_max_px=float(max(errors)))
                if np.median(errors) > 8 or max(errors) > 20:
                    summary["status"] = "failed_holdout"
                # Baseline/fitted reference-plane residuals: visual units, not measured distance error.
                try:
                    baseline = default_ground(role, rays[test], length, width)
                    summary["baseline_reference_median_units"] = float(np.median(np.linalg.norm(baseline-ground[test], axis=1)))
                except ValueError:
                    summary["baseline_reference_median_units"] = None
                summary["fitted_reference_median_units"] = float(np.median(np.linalg.norm(transform(matrix, rays[test])-ground[test], axis=1)))
            except (ValueError, np.linalg.LinAlgError) as error:
                summary.update(status="unstable_transform", reason=str(error))
        summaries.append(summary)
    return {"kind": "approximate_reference_ray_transforms", "installable": False,
            "note": "Same-photo holdouts are not independent scene validation. Estimated lens, "
                    "reference scale/perspective and sparse coverage limit extrapolation. "
                    "This is not an Android calibration profile or a recovered factory calibration.",
            "estimated_lens_fov_cx_cy": lens.tolist(), "cameras": summaries}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("points", type=Path)
    parser.add_argument("result", type=Path)
    args = parser.parse_args()
    report = fit_reference(json.loads(args.points.read_text()))
    args.result.write_text(json.dumps(report, indent=2, allow_nan=False) + "\n")
    for camera in report["cameras"]:
        print(camera["role"], camera["status"], camera.get("heldout_source_median_px", "-"))


if __name__ == "__main__":
    main()
