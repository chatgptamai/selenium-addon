package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;

import java.util.Random;

public class PanicMode extends Module {
    private final Random random = new Random();

    public PanicMode() {
        super(SeleniumAddon.CATEGORY, "panic-mode", "makes your character freak out randomly");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        float randomYaw = random.nextFloat() * 360.0f - 180.0f;
        float randomPitch = random.nextFloat() * 180.0f - 90.0f;

        Rotations.rotate(randomYaw, randomPitch);
        mc.player.swingHand(random.nextBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND);
    }
}
