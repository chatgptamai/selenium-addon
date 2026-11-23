package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class autodoor extends Module {
    public autodoor() {
        super(SeleniumAddon.CATEGORY, "autodoor", "open and close doors");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        Vec3d eyePos = mc.player.getEyePos();
        BlockPos origin = mc.player.getBlockPos();
        int r = (int) Math.ceil(5.0) + 1;

        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (!(state.getBlock() instanceof DoorBlock)) continue;
                    if (state.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;

                    if (eyePos.distanceTo(Vec3d.ofCenter(pos)) <= 5.0) {
                        Vec3d center = Vec3d.ofCenter(pos);
                        BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos, false);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                    }
                }
            }
        }
    }
}