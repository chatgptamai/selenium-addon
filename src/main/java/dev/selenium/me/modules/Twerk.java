package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Twerk extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("makefriendbro");

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("twork delay")
            .defaultValue(2)
            .min(1)
            .sliderMax(20)
            .build()
    );

    private int timer = 0;
    private boolean sneaking = false;

    public Twerk() {
        super(SeleniumAddon.CATEGORY, "twerk", "make friends");
    }

    @Override
    public void onActivate() {
        timer = 0;
        sneaking = false;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        timer++;
        if (timer >= delay.get()) {
            timer = 0;
            sneaking = !sneaking;
            mc.options.sneakKey.setPressed(sneaking);
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.options != null) {
            mc.options.sneakKey.setPressed(false);
        }
        sneaking = false;
    }
}
