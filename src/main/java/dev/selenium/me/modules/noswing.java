package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class noswing extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("seveing");

    private final Setting<Boolean> clientSide = sgGeneral.add(new BoolSetting.Builder()
            .name("client side")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> serverSide = sgGeneral.add(new BoolSetting.Builder()
            .name("server side")
            .defaultValue(true)
            .build()
    );

    public noswing() {
        super(SeleniumAddon.CATEGORY, "no swing", "nosiking");
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        resetSwing();
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        resetSwing();
    }

    private void resetSwing() {
        if (mc.player == null || !clientSide.get()) return;

        mc.player.handSwinging = false;
        mc.player.handSwingTicks = 0;
        mc.player.handSwingProgress = 0f;
        mc.player.lastHandSwingProgress = 0f;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || !serverSide.get()) return;

        if (event.packet instanceof HandSwingC2SPacket) {
            event.cancel();
        }
    }
}