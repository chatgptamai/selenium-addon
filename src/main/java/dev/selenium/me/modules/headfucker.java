package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;

public class headfucker extends Module {
    private final SettingGroup sgfuck = settings.createGroup("www");

    private final Setting<Integer> speed = sgfuck.add(new IntSetting.Builder()
            .name("speed")
            .description("speed(tick)")
            .defaultValue(2)
            .min(1)
            .sliderMax(10)
            .build()
    );

    private int timer = 0;
    private boolean up = false;

    public headfucker() {
        super(SeleniumAddon.CATEGORY, "headvro", "fucks ur head");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        timer++;
        if (timer >= speed.get()) {
            timer = 0;
            up = !up;
        }

        float targetPitch = up ? -90.0f : 90.0f;
        Rotations.rotate(mc.player.getYaw(), targetPitch);
    }
}
