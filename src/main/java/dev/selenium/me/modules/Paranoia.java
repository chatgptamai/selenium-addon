package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;

import java.util.Random;

public class Paranoia extends Module {
    private final Random random = new Random();
    private int nextTrigger = 60;

    public Paranoia() {
        super(SeleniumAddon.CATEGORY, "paranoia", "eandomly turns around and swings at nothing");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        nextTrigger--;
        if (nextTrigger <= 0) {
            nextTrigger = 40 + random.nextInt(120);

            float fakeYaw = mc.player.getYaw() + 180.0f + (random.nextFloat() * 40f - 20f);
            float fakePitch = random.nextFloat() * 40f - 20f;

            Rotations.rotate(fakeYaw, fakePitch, () -> mc.player.swingHand(Hand.MAIN_HAND));
        }
    }
}
