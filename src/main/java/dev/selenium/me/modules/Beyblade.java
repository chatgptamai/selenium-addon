package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
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

public class Beyblade extends Module {
    private final SettingGroup sgGeneral = this.settings.createGroup("spin");
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("spin speed")
            .defaultValue(150.0)
            .min(1.0)
            .max(100_000_000.0)
            .sliderMin(1.0)
            .sliderMax(100_000_000.0)
            .build()
    );

    private final Setting<Boolean> clside = sgGeneral.add(new BoolSetting.Builder()
            .name("client-side")
            .description("rotates ur camera too")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnAction = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-action")
            .description("snaps rotation to your real crosshair when dropping, using, or attacking")
            .defaultValue(true)
            .build()
    );

    private float yaw = 0f;
    private int pauseTicks = 0;

    public Beyblade() {
        super(SeleniumAddon.CATEGORY, "beyblade", "spinnn");
    }

    @Override
    public void onActivate() {
        pauseTicks = 0;
        if (mc.player != null) {
            yaw = mc.player.getYaw();
        }
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
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround()
            ));
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (pauseOnAction.get()) {
            if (mc.options.dropKey.isPressed() || mc.options.useKey.isPressed() || mc.options.attackKey.isPressed()) {
                pauseTicks = 3;
            }

            if (pauseTicks > 0) {
                pauseTicks--;
                return;
            }
        }

        yaw += speed.get().floatValue();
        yaw %= 360f;

        if (!clside.get()) {
            Rotations.rotate(yaw, mc.player.getPitch());
        } else {
            mc.player.setYaw(yaw);
        }
    }
}
