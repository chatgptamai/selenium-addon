package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import dev.selenium.me.event.DeathEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;

public class DeathOPTotem extends Module {
    private int delay = 0;
    private boolean waiting = false;

    private final SettingGroup sgtotemvro = this.settings.getDefaultGroup();
    private final Setting<Integer> totem = sgtotemvro.add(new IntSetting.Builder()
            .name("howmuchtotem")
            .defaultValue(31)
            .build()
    );

    public DeathOPTotem() {
        super(SeleniumAddon.Extra, "op-totem", "only use if u got op");
    }

    @EventHandler
    private void onDeath(DeathEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(
                new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN)
        );

        waiting = true;
        delay = 5;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!waiting) return;

        if (mc.player == null || mc.getNetworkHandler() == null) {
            waiting = false;
            return;
        }

        if (delay > 0) {
            delay--;
            return;
        }

        mc.getNetworkHandler().sendChatCommand("give @s totem_of_undying " + totem.get());
        waiting = false;
    }
}