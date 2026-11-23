package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AntiPop extends Module {

    private final SettingGroup sgGeneral = settings.createGroup("EHM");
    private final Setting<Double> minFallDistance = this.sgGeneral.add(
            new DoubleSetting.Builder()
                    .name("min-fall-distance")
                    .description("Minimum fall distance before resetting fall damage.")
                    .defaultValue(2.5)
                    .min(0.5)
                    .max(15.0)
                    .sliderMin(1.0)
                    .sliderMax(10.0)
                    .build()
    );

    private final Setting<Boolean> spoofOffGround = this.sgGeneral.add(
            new BoolSetting.Builder()
                    .name("spoof-off-ground")
                    .description("Keeps movement packets marked as off-ground while falling.")
                    .defaultValue(true)
                    .build()
    );

    private final Setting<Boolean> resetFallDistance = this.sgGeneral.add(
            new BoolSetting.Builder()
                    .name("reset-fall-distance")
                    .description("Resets client fallDistance to 0.")
                    .defaultValue(true)
                    .build()
    );

    public AntiPop() {
        super(SeleniumAddon.CATEGORY, "antipop", "prevents you from touching ground (to avoid taking fall damage).");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.mc.player == null || this.mc.world == null) return;

        if (this.resetFallDistance.get() && this.mc.player.fallDistance >= this.minFallDistance.get()) {
            this.mc.player.fallDistance = 0.0f;
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!this.spoofOffGround.get() || this.mc.player == null) return;
        if (this.mc.player.fallDistance < this.minFallDistance.get()) return;

        if (event.packet instanceof PlayerMoveC2SPacket movePacket && movePacket.isOnGround()) {
            if (this.resetFallDistance.get()) {
                this.mc.player.fallDistance = 0.0f;
            }
        }
    }
}