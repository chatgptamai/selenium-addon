package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

public class funvro extends Module {
    public enum ParticleStyle {
        EndRod,
        Flame,
        Heart,
        Note,
        Enchant
    }

    private final SettingGroup sgGeneral = settings.createGroup("FUNNN");

    private final Setting<ParticleStyle> style = sgGeneral.add(new EnumSetting.Builder<ParticleStyle>()
            .name("style")
            .defaultValue(ParticleStyle.EndRod)
            .build()
    );

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
            .name("radius")
            .defaultValue(1.4)
            .min(0.3)
            .max(5.0)
            .sliderRange(0.3, 4.0)
            .build()
    );

    private final Setting<Double> heightOffset = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .defaultValue(1.2)
            .min(0.1)
            .max(3.5)
            .sliderRange(0.1, 2.8)
            .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .defaultValue(0.25)
            .min(0.02)
            .max(2.0)
            .sliderRange(0.05, 1.5)
            .build()
    );

    private final Setting<Integer> points = sgGeneral.add(new IntSetting.Builder()
            .name("points")
            .defaultValue(6)
            .min(1)
            .max(24)
            .sliderRange(1, 16)
            .build()
    );

    private final Setting<Boolean> waveHeight = sgGeneral.add(new BoolSetting.Builder()
            .name("wave height")
            .defaultValue(true)
            .build()
    );

    private double angle = 0.0;

    public funvro() {
        super(SeleniumAddon.CATEGORY, "aurafarming", "clientside aurafarm");
    }

    @Override
    public void onActivate() {
        angle = 0.0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        double r = radius.get();
        int count = points.get();
        double baseY = mc.player.getY() + heightOffset.get();
        ParticleEffect particle = resolveParticle(style.get());

        for (int i = 0; i < count; i++) {
            double theta = angle + (2.0 * Math.PI * i) / count;
            double x = mc.player.getX() + Math.cos(theta) * r;
            double z = mc.player.getZ() + Math.sin(theta) * r;
            double y = waveHeight.get()
                    ? baseY + Math.sin(theta * 2.0 + angle) * 0.25
                    : baseY;

            mc.world.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }

        angle += speed.get();
        if (angle > Math.PI * 2.0) {
            angle -= Math.PI * 2.0;
        }
    }

    private ParticleEffect resolveParticle(ParticleStyle selected) {
        return switch (selected) {
            case EndRod -> ParticleTypes.END_ROD;
            case Flame -> ParticleTypes.FLAME;
            case Heart -> ParticleTypes.HEART;
            case Note -> ParticleTypes.NOTE;
            case Enchant -> ParticleTypes.ENCHANT;
        };
    }
}