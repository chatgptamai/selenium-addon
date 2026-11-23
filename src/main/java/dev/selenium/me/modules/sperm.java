package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class sperm extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("blowjob");

    private final Setting<Double> blowjobspeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("fucking speed")
            .defaultValue(2.5)
            .min(0.5)
            .sliderMax(10.0)
            .build()
    );

    private int originalFov = 70;
    private double wavvvvvvvvve = 0.0;

    public sperm() {
        super(SeleniumAddon.CATEGORY, "cameravro", "");
    }

    @Override
    public void onActivate() {
        if (mc.options != null) {
            originalFov = mc.options.getFov().getValue();
        }
        wavvvvvvvvve = 0.0;
    }

    @Override
    public void onDeactivate() {
        if (mc.options != null && mc.options.getFov().getValue() != originalFov) {
            mc.options.getFov().setValue(originalFov);
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.options == null) return;

        double dt = Math.min(event.frameTime, 0.05);
        wavvvvvvvvve += dt * blowjobspeed.get();

        double normalized = (Math.sin(wavvvvvvvvve) + 1.0) / 2.0;
        int newFov = 30 + (int) (normalized * 80);

        if (mc.options.getFov().getValue() != newFov) {
            mc.options.getFov().setValue(newFov);
        }
    }
}