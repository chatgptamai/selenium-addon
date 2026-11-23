package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class eclink extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> openKey = sgGeneral.add(new KeybindSetting.Builder()
            .name("key")
            .defaultValue(Keybind.none())
            .build()
    );

    private int syncedId = -1;
    private ScreenHandler screenHandler = null;
    private boolean expectingEnderChest = false;
    private boolean linked = false;
    private boolean allowOpen = false;
    private boolean keyWasPressed = false;

    public eclink() {
        super(SeleniumAddon.CATEGORY, "eclink", "Keeps ender chest open server-side and hides the screen");
    }

    public boolean isLinked() {
        return linked && syncedId != -1 && screenHandler != null;
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        if (linked && mc.getNetworkHandler() != null && syncedId != -1) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncedId));
        }
        reset();
    }

    private void reset() {
        syncedId = -1;
        screenHandler = null;
        expectingEnderChest = false;
        linked = false;
        allowOpen = false;
        keyWasPressed = false;
    }

    public void showEnderChest() {
        if (mc.player == null || !isLinked() || !(screenHandler instanceof GenericContainerScreenHandler handler)) return;

        allowOpen = true;
        syncedId = handler.syncId;
        mc.player.currentScreenHandler = handler;
        mc.setScreen(new GenericContainerScreen(handler, mc.player.getInventory(), Text.literal("Ender Chest")));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        Keybind bind = openKey.get();
        boolean pressed = bind != null && bind.isPressed();
        if (pressed && !keyWasPressed) {
            showEnderChest();
        }
        keyWasPressed = pressed;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;

        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            BlockPos pos = packet.getBlockHitResult().getBlockPos();
            if (mc.world.getBlockState(pos).isOf(Blocks.ENDER_CHEST)) {
                expectingEnderChest = true;
            }
        } else if (event.packet instanceof CloseHandledScreenC2SPacket closePacket) {
            if (linked && (syncedId == -1 || closePacket.getSyncId() == syncedId)) {
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (event.packet instanceof OpenScreenS2CPacket packet && expectingEnderChest) {
            if (packet.getScreenHandlerType() == ScreenHandlerType.GENERIC_9X3) {
                syncedId = packet.getSyncId();
                expectingEnderChest = false;
            }
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (mc.player == null || syncedId == -1) return;

        if (allowOpen) {
            allowOpen = false;
            return;
        }

        if (event.screen instanceof GenericContainerScreen containerScreen) {
            screenHandler = containerScreen.getScreenHandler();
            if (screenHandler != null) {
                syncedId = screenHandler.syncId;
                mc.player.currentScreenHandler = screenHandler;
                linked = true;
            }
            mc.setScreen(null);
            event.cancel();
        }
    }
}