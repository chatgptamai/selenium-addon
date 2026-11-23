package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

public class gptroom extends Module {
    private static final double MAX_DISTANCE = 1000.0;

    private final SettingGroup room = settings.createGroup("bedroom");

    private final Setting<Integer> clearance = room.add(new IntSetting.Builder()
            .name("clearance")
            .defaultValue(2)
            .min(1)
            .max(4)
            .sliderMax(4)
            .build()
    );

    private final Setting<Integer> floorSearchDepth = room.add(new IntSetting.Builder()
            .name("floor depth")
            .defaultValue(32)
            .min(1)
            .max(128)
            .sliderMax(64)
            .build()
    );

    private final Setting<Boolean> verticalOnly = room.add(new BoolSetting.Builder()
            .name("vertical only")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> snapToFloor = room.add(new BoolSetting.Builder()
            .name("snap to floor")
            .defaultValue(true)
            .build()
    );

    public gptroom() {
        super(SeleniumAddon.CATEGORY, "room", "teleports into the detected pocket");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        Vec3d origin = mc.player.getPos();
        Vec3d dir = getScanDirection();
        boolean hitSolid = false;

        for (double dist = 0.5; dist <= MAX_DISTANCE; dist += 0.5) {
            Vec3d probe = verticalOnly.get()
                    ? new Vec3d(origin.x, origin.y + dir.y * dist, origin.z)
                    : origin.add(dir.multiply(dist));

            BlockPos checkPos = BlockPos.ofFloored(probe.x, probe.y, probe.z);

            if (!isPosLoaded(checkPos)) {
                break;
            }

            if (!hasClearance(checkPos)) {
                hitSolid = true;
                continue;
            }

            if (hitSolid) {
                double destX = verticalOnly.get() ? origin.x : probe.x;
                double destZ = verticalOnly.get() ? origin.z : probe.z;
                double targetY = snapToFloor.get() ? findFloorY(checkPos, probe.y) : probe.y;
                double deltaY = targetY - origin.y;

                BlockPos finalPos = BlockPos.ofFloored(destX, origin.y + deltaY, destZ);
                if (hasClearance(finalPos)) {
                    runVClip(destX, origin.y + deltaY, destZ);
                    break;
                }
            }
        }

        toggle();
    }

    private void runVClip(double x, double y, double z) {
        if (mc.player == null) return;
        mc.player.setVelocity(0.0, 0.0, 0.0);
        mc.player.setPosition(x, y, z);
        mc.player.resetPosition();
    }

    private boolean isPosLoaded(BlockPos pos) {
        if (mc.world == null) return false;
        return mc.world.getChunkManager().isChunkLoaded(
                ChunkSectionPos.getSectionCoord(pos.getX()),
                ChunkSectionPos.getSectionCoord(pos.getZ())
        );
    }

    private Vec3d getScanDirection() {
        if (mc.player == null) return new Vec3d(0, 1, 0);
        if (verticalOnly.get()) {
            return mc.player.getPitch() > 0 ? new Vec3d(0, -1, 0) : new Vec3d(0, 1, 0);
        }
        return mc.player.getRotationVec(1.0f).normalize();
    }

    private boolean hasClearance(BlockPos basePos) {
        if (mc.world == null) return false;
        for (int i = 0; i < clearance.get(); i++) {
            BlockPos upPos = basePos.up(i);
            BlockState state = mc.world.getBlockState(upPos);
            if (!state.getCollisionShape(mc.world, upPos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private double findFloorY(BlockPos pocketPos, double fallbackY) {
        if (mc.world == null) return fallbackY;
        BlockPos.Mutable cursor = pocketPos.mutableCopy();
        for (int i = 0; i < floorSearchDepth.get(); i++) {
            cursor.move(0, -1, 0);
            if (!mc.world.getBlockState(cursor).getCollisionShape(mc.world, cursor).isEmpty()) {
                return cursor.getY() + 1.0;
            }
        }
        return fallbackY;
    }
}