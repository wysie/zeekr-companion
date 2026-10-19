package com.kooo.evcam.zeekr;

import java.util.Arrays;

/** Immutable shader parameters in raw composite coordinates; shared by settings and live windows. */
final class SideGpuPicture {
    final float[] lane, view, lens, turn, limit;
    final int mode;
    SideGpuPicture(CompositeStreamGeometry.Plan plan, int laneIndex, RearViewGeometry.Viewport viewport,
            int mode, float fov, float yaw, float pitch, int rotation, boolean mirrored, int width, int height) {
        CompositeStreamGeometry.Lane l = plan.lane(laneIndex);
        lane = new float[]{l.u0, l.v0, l.u1 - l.u0, l.v1 - l.v0};
        view = new float[]{viewport.x, viewport.y, viewport.width, viewport.height};
        float aspect = width / (float) Math.max(1, height);
        double roll = Math.toRadians(SideViewRotation.clamp(rotation));
        lens = new float[]{(float) Math.tan(Math.toRadians(fov) / 2), aspect, (float) Math.cos(roll), (float) Math.sin(roll)};
        double p = Math.toRadians(pitch), y = Math.toRadians(yaw) * (laneIndex == LaneCycle.RIGHT ? 1 : -1);
        turn = new float[]{(float) Math.cos(p), (float) Math.sin(p), (float) Math.cos(y), (float) Math.sin(y)};
        limit = new float[]{mirrored ? 1 : 0, mode == 0 ? SideViewRotation.coverScale(rotation, width, height) : 1,
                .5f / Math.max(1, l.width), .5f / Math.max(1, l.height)};
        this.mode = mode;
    }
    static SideGpuPicture side(CompositeStreamGeometry.Plan plan, int lane, int width, int height,
            boolean straighten, float fov, float yaw, float pitch, int roll, boolean mirrored, SideViewFraming framing) {
        RearViewGeometry.Viewport viewport = RearViewGeometry.Viewport.full();
        if (!straighten) {
            RearViewGeometry.Viewport aim = SideViewAim.viewport(lane, Math.round(180f / fov * 100),
                    Math.round(yaw / SideViewProjection.MAX_YAW_DEGREES * 100),
                    Math.round(pitch / SideViewProjection.MAX_PITCH_DEGREES * 100));
            RearViewGeometry.Viewport crop = RearViewGeometry.Viewport.forWindow(width, height, .5f);
            viewport = new RearViewGeometry.Viewport(aim.x + crop.x * aim.width, aim.y + crop.y * aim.height,
                    crop.width * aim.width, crop.height * aim.height);
        }
        return new SideGpuPicture(plan, lane, viewport, straighten ? 2 : 0,
                straighten ? framing.effectiveFovDegrees() : fov, yaw, pitch, roll, mirrored, width, height);
    }
    @Override public boolean equals(Object other) {
        if (!(other instanceof SideGpuPicture)) return false;
        SideGpuPicture p = (SideGpuPicture) other;
        return mode == p.mode && Arrays.equals(lane,p.lane) && Arrays.equals(view,p.view)
                && Arrays.equals(lens,p.lens) && Arrays.equals(turn,p.turn) && Arrays.equals(limit,p.limit);
    }
    @Override public int hashCode() { return 31 * Arrays.hashCode(lane) + Arrays.hashCode(lens) + mode; }
}
