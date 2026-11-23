package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TickShifter extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("tick tock");

    private final Setting<Integer> maxTicks = sgGeneral.add(new IntSetting.Builder()
            .name("max ticks")
            .defaultValue(20)
            .min(1)
            .sliderMax(100)
            .build()
    );

    private final Setting<Double> timerSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("timer speed")
            .defaultValue(2.0)
            .min(1.1)
            .sliderMax(5.0)
            .build()
    );

    private final Setting<Boolean> disableOnEmpty = sgGeneral.add(new BoolSetting.Builder()
            .name("disable on empty")
            .defaultValue(false)
            .build()
    );
    private int chargedTicks = 0;
    private boolean shifting = false;
    public TickShifter() {
        super(SeleniumAddon.CATEGORY, "tick-shift", "charges ticks while stationary and releases them as speed when moving");
    }

    @Override
    public void onActivate() {
        chargedTicks = 0;
        shifting = false;
    }

    @Override
    public void onDeactivate() {
        resetTimer();
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (PlayerUtils.isMoving()) {
            if (chargedTicks > 0) {
                shifting = true;
                Modules.get().get(Timer.class).setOverride(timerSpeed.get());
                chargedTicks--;
            } else {
                resetTimer();
                if (disableOnEmpty.get()) toggle();
            }
        } else {
            resetTimer();
            if (chargedTicks < maxTicks.get()) {
                chargedTicks++;
            }
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!shifting && !PlayerUtils.isMoving() && event.packet instanceof PlayerMoveC2SPacket) {
            event.cancel();
        }
    }

    private void resetTimer() {
        if (shifting) {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
            shifting = false;
        }
    }

    @Override
    public String getInfoString() {
        return String.valueOf(chargedTicks);
    }
}
