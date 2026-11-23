package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;

public class BunnyHop extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("hop hop");

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .defaultValue(1.1)
            .min(1.0)
            .sliderMax(2.5)
            .build()
    );

    public BunnyHop() {
        super(SeleniumAddon.CATEGORY, "bunnyhop", "moves faster by bunny hopping");
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isSneaking()) return;

        if (PlayerUtils.isMoving()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            } else {
                double currentSpeed = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
                applyStrafe(Math.max(currentSpeed, 0.2873 * speed.get()));
            }
        }
    }

    private void applyStrafe(double targetSpeed) {
        float forward = mc.player.forwardSpeed;
        float strafe = mc.player.sidewaysSpeed;
        float yaw = mc.player.getYaw();

        if (forward == 0 && strafe == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
        }

        if (forward != 0) {
            if (strafe > 0) yaw += (forward > 0 ? -45 : 45);
            else if (strafe < 0) yaw += (forward > 0 ? 45 : -45);
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90.0f);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double velX = (forward * targetSpeed * cos) + (strafe * targetSpeed * sin);
        double velZ = (forward * targetSpeed * sin) - (strafe * targetSpeed * cos);

        mc.player.setVelocity(velX, mc.player.getVelocity().y, velZ);
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            mc.player.setJumping(false);
        }
    }
}
