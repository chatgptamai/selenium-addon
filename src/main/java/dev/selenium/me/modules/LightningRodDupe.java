package dev.selenium.me.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import dev.selenium.me.SeleniumAddon;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LightningRodDupe extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("dupe");


    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("")
            .defaultValue(5.5)
            .min(1)
            .max(6)
            .sliderMax(6)
            .build()
    );

    private final Setting<Integer> maxPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("max-packet-per-tick")
            .description("")
            .defaultValue(20)
            .min(1)
            .max(40)
            .sliderMax(40)
            .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing")
            .description("")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlyHotbar = sgGeneral.add(new BoolSetting.Builder()
            .name("only-hotbar")
            .description("")
            .defaultValue(true)
            .build()
    );

    public LightningRodDupe() {
        super(SeleniumAddon.CATEGORY, "lightning-rod-shulker", "Lightning rod dupe for nihilium");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        FindItemResult shulker = findShulker();
        if (!shulker.found()) return;


        boolean swapped = false;
        if (!shulker.isOffhand() && !shulker.isMainHand()) {
            InvUtils.swap(shulker.slot(), true);
            swapped = true;
        }

        Hand hand = shulker.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;


        List<BlockPos> rods = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        int r = (int) Math.ceil(range.get());

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() != Blocks.LIGHTNING_ROD) continue;

                    BlockPos above = pos.up();

                    if (mc.player.squaredDistanceTo(Vec3d.ofCenter(above)) <= range.get() * range.get()) {
                        rods.add(pos);
                    }
                }
            }
        }

        if (rods.isEmpty()) {
            if (swapped) InvUtils.swapBack();
            return;
        }

        rods.sort(Comparator.comparingDouble(pos -> mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))));

        int placed = 0;

        for (BlockPos rod : rods) {
            if (placed >= maxPerTick.get()) break;

            BlockPos placePos = rod.up();


            Vec3d hitVec = Vec3d.ofCenter(rod).add(0, 0.5, 0);
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, rod, false);

            mc.interactionManager.interactBlock(mc.player, hand, hit);

            if (swing.get()) {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }

            placed++;
        }

        if (swapped) {
            InvUtils.swapBack();
        }
    }

    private FindItemResult findShulker() {
        if (onlyHotbar.get()) {
            return InvUtils.findInHotbar(this::isShulker);
        }
        return InvUtils.find(this::isShulker);
    }

    private boolean isShulker(Item item) {
        return item == Items.SHULKER_BOX
                || item == Items.WHITE_SHULKER_BOX
                || item == Items.ORANGE_SHULKER_BOX
                || item == Items.MAGENTA_SHULKER_BOX
                || item == Items.LIGHT_BLUE_SHULKER_BOX
                || item == Items.YELLOW_SHULKER_BOX
                || item == Items.LIME_SHULKER_BOX
                || item == Items.PINK_SHULKER_BOX
                || item == Items.GRAY_SHULKER_BOX
                || item == Items.LIGHT_GRAY_SHULKER_BOX
                || item == Items.CYAN_SHULKER_BOX
                || item == Items.PURPLE_SHULKER_BOX
                || item ==Items.BLUE_SHULKER_BOX
                || item == Items.BROWN_SHULKER_BOX
                || item == Items.GREEN_SHULKER_BOX
                || item == Items.RED_SHULKER_BOX
                || item == Items.BLACK_SHULKER_BOX;
    }

    private boolean isShulker(net.minecraft.item.ItemStack stack) {
        return isShulker(stack.getItem());
    }
}