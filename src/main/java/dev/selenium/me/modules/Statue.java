package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class Statue extends Module {
    public Statue() {
        super(SeleniumAddon.CATEGORY, "statue", "freezes your arm animations to look like a moving statue");
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof HandSwingC2SPacket) {
            event.cancel();
        }
    }
}
