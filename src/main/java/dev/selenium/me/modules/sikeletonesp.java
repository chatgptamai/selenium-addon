package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class sikeletonesp extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("suckmydih");
    private final SettingGroup sgRender = settings.createGroup("rrrrender");

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore self")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("max distance")
            .defaultValue(64.0)
            .min(4.0)
            .max(256.0)
            .sliderRange(8.0, 128.0)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line color")
            .defaultValue(new SettingColor(255, 255, 255, 220))
            .build()
    );

    private final Setting<SettingColor> friendsColor = sgRender.add(new ColorSetting.Builder()
            .name("friends color")
            .description("color used for players on your friends list")
            .defaultValue(new SettingColor(0, 255, 140, 220))
            .build()
    );

    public sikeletonesp() {
        super(SeleniumAddon.CATEGORY, "skeleton-esp", "see ppl's skeleton");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        double maxDistSq = maxDistance.get() * maxDistance.get();
        float tickDelta = event.tickDelta;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (ignoreSelf.get() && player == mc.player) continue;
            if (!player.isAlive() || player.isRemoved()) continue;
            if (mc.player.squaredDistanceTo(player) > maxDistSq) continue;

            SettingColor color = Friends.get().isFriend(player) ? friendsColor.get() : lineColor.get();
            drawSkeleton(event, player, tickDelta, color);
        }
    }

    private void drawSkeleton(Render3DEvent event, PlayerEntity player, float tickDelta, SettingColor c) {
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        float bodyYawRad = (float) Math.toRadians(MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw));
        double cos = Math.cos(bodyYawRad);
        double sin = Math.sin(bodyYawRad);

        boolean sneaking = player.isSneaking();
        double hipY = y + (sneaking ? 0.82 : 0.95);
        double shoulderY = y + (sneaking ? 1.25 : 1.42);
        double neckY = y + (sneaking ? 1.35 : 1.52);
        double headTopY = y + (sneaking ? 1.62 : 1.80);

        double hipHalfWidth = 0.16;
        double shoulderHalfWidth = 0.28;
        double armDrop = 0.52;
        double legDrop = hipY - y;

        // Omurga ve boyun/kafa ekseni
        Vec3d hipCenter = new Vec3d(x, hipY, z);
        Vec3d shoulderCenter = new Vec3d(x, shoulderY, z);
        Vec3d neck = new Vec3d(x, neckY, z);
        Vec3d headTop = new Vec3d(x, headTopY, z);

        // Sol ve sag omuz
        Vec3d leftShoulder = offsetBody(x, shoulderY, z, -shoulderHalfWidth, cos, sin);
        Vec3d rightShoulder = offsetBody(x, shoulderY, z, shoulderHalfWidth, cos, sin);

        // Sol ve sag kol (dirsek ve el ucu)
        Vec3d leftElbow = offsetBody(leftShoulder.x, shoulderY - armDrop * 0.5, leftShoulder.z, -0.04, cos, sin);
        Vec3d leftHand = offsetBody(leftShoulder.x, shoulderY - armDrop, leftShoulder.z, -0.08, cos, sin);

        Vec3d rightElbow = offsetBody(rightShoulder.x, shoulderY - armDrop * 0.5, rightShoulder.z, 0.04, cos, sin);
        Vec3d rightHand = offsetBody(rightShoulder.x, shoulderY - armDrop, rightShoulder.z, 0.08, cos, sin);

        // Sol ve sag kalca
        Vec3d leftHip = offsetBody(x, hipY, z, -hipHalfWidth, cos, sin);
        Vec3d rightHip = offsetBody(x, hipY, z, hipHalfWidth, cos, sin);

        // Sol ve sag bacak (diz ve ayak)
        double kneeOffsetZ = sneaking ? 0.16 : 0.02;
        Vec3d leftKnee = offsetLocal(leftHip.x, y + legDrop * 0.5, leftHip.z, 0.0, kneeOffsetZ, cos, sin);
        Vec3d leftFoot = new Vec3d(leftHip.x, y + 0.05, leftHip.z);

        Vec3d rightKnee = offsetLocal(rightHip.x, y + legDrop * 0.5, rightHip.z, 0.0, kneeOffsetZ, cos, sin);
        Vec3d rightFoot = new Vec3d(rightHip.x, y + 0.05, rightHip.z);

        // Omurga ve boyun
        line(event, hipCenter, shoulderCenter, c);
        line(event, shoulderCenter, neck, c);
        line(event, neck, headTop, c);

        // Omuz hattı ve kollar
        line(event, leftShoulder, rightShoulder, c);
        line(event, leftShoulder, leftElbow, c);
        line(event, leftElbow, leftHand, c);
        line(event, rightShoulder, rightElbow, c);
        line(event, rightElbow, rightHand, c);

        // Kalca hattı ve bacaklar
        line(event, leftHip, rightHip, c);
        line(event, leftHip, leftKnee, c);
        line(event, leftKnee, leftFoot, c);
        line(event, rightHip, rightKnee, c);
        line(event, rightKnee, rightFoot, c);
    }

    private Vec3d offsetBody(double x, double y, double z, double lateral, double cos, double sin) {
        return offsetLocal(x, y, z, lateral, 0.0, cos, sin);
    }

    private Vec3d offsetLocal(double x, double y, double z, double right, double forward, double cos, double sin) {
        double dx = right * cos - forward * sin;
        double dz = right * sin + forward * cos;
        return new Vec3d(x + dx, y, z + dz);
    }

    private void line(Render3DEvent event, Vec3d a, Vec3d b, SettingColor color) {
        event.renderer.line(a.x, a.y, a.z, b.x, b.y, b.z, color);
    }
}