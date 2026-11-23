package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class BackHole extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("asshole");

    private final Setting<Boolean> pauseOnAction = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-action")
            .description("Snaps rotation to your real crosshair for 3 ticks when dropping, using, or attacking.")
            .defaultValue(true)
            .build()
    );

    private int pauseTicks = 0;

    public BackHole() {
        super(SeleniumAddon.CATEGORY, "back-hole", "ASshole protector.");
    }

    @Override
    public void onActivate() {
        pauseTicks = 0;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!pauseOnAction.get() || mc.player == null || mc.getNetworkHandler() == null) return;

        boolean isActionPacket = event.packet instanceof PlayerInteractItemC2SPacket
                || event.packet instanceof PlayerInteractBlockC2SPacket
                || event.packet instanceof PlayerInteractEntityC2SPacket
                || (event.packet instanceof PlayerActionC2SPacket action && (
                action.getAction() == PlayerActionC2SPacket.Action.DROP_ITEM ||
                        action.getAction() == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS ||
                        action.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
        ));

        if (isActionPacket) {
            pauseTicks = 3;
            // İşlem paketi sunucuya gitmeden HEMEN ÖNCE kameranın gerçek bakış açısını gönder
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround()
            ));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (pauseOnAction.get()) {
            if (mc.options.dropKey.isPressed() || mc.options.useKey.isPressed() || mc.options.attackKey.isPressed()) {
                pauseTicks = 3;
            }

            if (pauseTicks > 0) {
                pauseTicks--;
                // 3 tick boyunca kameranın baktığı gerçek yönde kal
                Rotations.rotate(mc.player.getYaw(), mc.player.getPitch());
                return;
            }
        }

        // Süre bittiğinde karakteri tekrar kameranın tam 180 derece tersine döndür
        float reverseYaw = mc.player.getYaw() + 180.0f;
        float pitch = mc.player.getPitch();

        Rotations.rotate(reverseYaw, pitch);
    }
}
