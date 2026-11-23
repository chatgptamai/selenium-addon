package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

public class autoscrool extends Module {
    private final SettingGroup aaa = settings.createGroup("EHM");

    private final Setting<Integer> delay = aaa.add(new IntSetting.Builder()
            .name("delay")
            .description("delay")
            .defaultValue(2)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private int timer = 0;
    private int slot = 0;

    public autoscrool() {
        super(SeleniumAddon.CATEGORY, "auto-scrroooool", "rapidly cycles through your hotbar slots");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (mc.player == null || mc.world == null) return;
        eclink target = Modules.get().get(eclink.class);
        timer++;
        if (timer >= delay.get()) {
            timer = 0;
            slot = (slot + 1) % 9;
            mc.player.getInventory().selectedSlot = slot;
        }
    }
}
