package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;

public class DrunkWalk extends Module {
    private double wave = 0;

    public DrunkWalk() {
        super(SeleniumAddon.CATEGORY, "drunk", "makes your character wobble like you are drunk");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        wave += 0.25;
        float wobble = (float) Math.sin(wave) * 4.5f;
        mc.player.setYaw(mc.player.getYaw() + wobble);

        if (PlayerUtils.isMoving()) {
            mc.options.leftKey.setPressed(Math.sin(wave * 0.7) > 0.4);
            mc.options.rightKey.setPressed(Math.sin(wave * 0.7) < -0.4);
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.options != null) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }
    }
}
