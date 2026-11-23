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
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class craftlink extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> openKey = sgGeneral.add(new KeybindSetting.Builder()
            .name("key")
            .description("Opens cached crafting table")
            .defaultValue(Keybind.none())
            .build()
    );

    private int syncedId = -1;
    private ScreenHandler screenHandler = null;
    private boolean expectingCraftingTable = false;
    private boolean linked = false;
    private boolean allowOpen = false;
    private boolean keyWasPressed = false;

    public craftlink() {
        super(SeleniumAddon.CATEGORY, "craftlink", "Keeps crafting table open server-side and hides the screen");
    }

    public boolean isLinked() {
        return linked;
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
        expectingCraftingTable = false;
        linked = false;
        allowOpen = false;
        keyWasPressed = false;
    }

    public void showCraftingTable() {
        if (mc.player == null || !linked || !(screenHandler instanceof CraftingScreenHandler handler)) return;

        allowOpen = true;
        syncedId = handler.syncId;
        mc.player.currentScreenHandler = handler;
        mc.setScreen(new CraftingScreen(handler, mc.player.getInventory(), Text.literal("Crafting")));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        Keybind bind = openKey.get();
        boolean pressed = bind != null && bind.isPressed();
        if (pressed && !keyWasPressed) {
            showCraftingTable();
        }
        keyWasPressed = pressed;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;

        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            BlockPos pos = packet.getBlockHitResult().getBlockPos();
            if (mc.world.getBlockState(pos).isOf(Blocks.CRAFTING_TABLE)) {
                expectingCraftingTable = true;
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

        if (event.packet instanceof OpenScreenS2CPacket packet && expectingCraftingTable) {
            if (packet.getScreenHandlerType() == ScreenHandlerType.CRAFTING) {
                syncedId = packet.getSyncId();
                linked = true;
                expectingCraftingTable = false;
            }
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (mc.player == null || !linked) return;

        if (allowOpen) {
            allowOpen = false;
            return;
        }

        if (event.screen instanceof CraftingScreen craftingScreen) {
            screenHandler = craftingScreen.getScreenHandler();
            if (screenHandler != null) {
                syncedId = screenHandler.syncId;
                mc.player.currentScreenHandler = screenHandler;
            }
            mc.setScreen(null);
            event.cancel();
        }
    }
}