import unittest
import numpy as np
from fit_bird_eye_reference import fit_reference, rays_from_uv, solve_transform, transform, uv_from_rays


class ReferenceFitTest(unittest.TestCase):
    def scene(self):
        uv = np.array([(x, y) for x in np.linspace(.23, .77, 7) for y in np.linspace(.48, .72, 6)])
        known = np.array([[2, .2, -.5], [.3, 1.5, 3.1], [.1, .7, .6]])
        ground = transform(known, rays_from_uv(uv))
        return {"length": 5.2, "width": 2., "source_lane_pixels": 1280,
                "points": [{"camera": 0, "uv": u.tolist(), "ground": g.tolist()} for u, g in zip(uv, ground)]}

    def test_recovers_a_known_ray_transform_without_promoting_it_to_a_profile(self):
        result = fit_reference(self.scene())
        camera = result["cameras"][0]
        self.assertEqual("candidate_only", camera["status"])
        self.assertLess(camera["heldout_source_max_px"], .001)
        full = np.array(camera["full_data_camera_ray_to_reference_ground"])
        data = self.scene()
        uv = np.array([p["uv"] for p in data["points"]])
        expected = np.array([p["ground"] for p in data["points"]])
        np.testing.assert_allclose(transform(full, rays_from_uv(uv)), expected, atol=1e-8)
        self.assertFalse(result["installable"])
        self.assertEqual("insufficient_correspondences", result["cameras"][1]["status"])

    def test_duplicate_detections_cannot_create_enough_evidence(self):
        data = self.scene()
        data["points"] = [data["points"][0]] * 100
        self.assertEqual("insufficient_correspondences", fit_reference(data)["cameras"][0]["status"])

    def test_collinear_features_are_rejected(self):
        data = self.scene()
        data["points"] = [{"camera": 0, "uv": [u, .65], "ground": [u, 3.]} for u in np.linspace(.2, .8, 20)]
        self.assertEqual("degenerate_layout", fit_reference(data)["cameras"][0]["status"])

    def test_wildly_inconsistent_matches_do_not_pass_holdouts(self):
        data = self.scene()
        rng = np.random.default_rng(55)
        for p in data["points"]:
            p["ground"] = rng.uniform(-3, 3, 2).tolist()
        self.assertIn(fit_reference(data)["cameras"][0]["status"], ("failed_holdout", "unstable_transform"))

    def test_singular_transform_cannot_generate_coordinates(self):
        with self.assertRaises(ValueError):
            transform(np.zeros((3, 3)), np.array([[0., 0., 1.]]))

    def test_equivalent_lens_rays_round_trip(self):
        uv = np.array([[.5, .5], [.2, .6], [.7, .7], [.1, .4]])
        np.testing.assert_allclose(uv, uv_from_rays(rays_from_uv(uv)), atol=1e-10)

    def test_invalid_role_is_not_silently_ignored(self):
        data = self.scene(); data["points"][0]["camera"] = 4
        with self.assertRaises(ValueError):
            fit_reference(data)

    def test_nonfinite_point_is_rejected(self):
        data = self.scene(); data["points"][0]["uv"][0] = float("nan")
        with self.assertRaises(ValueError):
            fit_reference(data)

    def test_outside_source_pixel_is_rejected(self):
        data = self.scene(); data["points"][0]["uv"][0] = 1.1
        with self.assertRaises(ValueError):
            fit_reference(data)

    def test_degenerate_solve_is_rejected(self):
        with self.assertRaises(ValueError):
            solve_transform(np.tile([0., 0., 1.], (8, 1)), np.zeros((8, 2)))


if __name__ == "__main__":
    unittest.main()
