package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

public class ClickTP extends Module {
    private static final double MAX_DISTANCE = 1000.0;

    private final SettingGroup sgGeneral = settings.createGroup("tvy");

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
            .name("instant")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> stepSize = sgGeneral.add(new DoubleSetting.Builder()
            .name("step size")
            .defaultValue(4.0)
            .min(0.5)
            .max(16.0)
            .sliderRange(1.0, 10.0)
            .build()
    );

    private final Setting<Integer> stepIntervalTicks = sgGeneral.add(new IntSetting.Builder()
            .name("tick interval")
            .description("ticks to wait between each step")
            .defaultValue(1)
            .min(0)
            .max(20)
            .sliderRange(0, 10)
            .visible(() -> !instant.get())
            .build()
    );

    private final Setting<Boolean> goBack = sgGeneral.add(new BoolSetting.Builder()
            .name("go back")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> delayTicks = sgGeneral.add(new IntSetting.Builder()
            .name("delay ticks")
            .defaultValue(10)
            .min(0)
            .max(1200)
            .sliderMax(400)
            .visible(goBack::get)
            .build()
    );

    private final Setting<Boolean> onTop = sgGeneral.add(new BoolSetting.Builder()
            .name("on top")
            .description("teleport onto the top of the targeted block instead of the hit face")
            .defaultValue(false)
            .build()
    );

    private final Deque<Vec3d> stepQueue = new ArrayDeque<>();
    private Vec3d startPos;
    private Vec3d targetPos;
    private Phase phase = Phase.IDLE;
    private int waitTimer;
    private int stepTimer;

    public ClickTP() {
        super(SeleniumAddon.CATEGORY, "TP", "teleports to the targeted block");
    }

    @Override
    public void onActivate() {
        resetState();

        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        HitResult hitResult = mc.player.raycast(MAX_DISTANCE, 1.0f, false);
        if (!(hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            toggle();
            return;
        }

        Vec3d resolved = resolveTargetPos(blockHit);
        if (resolved == null) {
            toggle();
            return;
        }

        startPos = mc.player.getPos();
        targetPos = resolved;
        buildSteps(startPos, targetPos);
        phase = Phase.GOING;
        stepTimer = 0;

        if (instant.get()) {
            flushQueue(targetPos);
            finishGoingPhase();
        } else if (!stepQueue.isEmpty()) {
            teleportTo(stepQueue.pollFirst());
        }
    }

    @Override
    public void onDeactivate() {
        resetState();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || phase == Phase.IDLE) return;

        switch (phase) {
            case GOING -> {
                if (instant.get()) {
                    flushQueue(targetPos);
                    finishGoingPhase();
                    return;
                }

                if (!stepQueue.isEmpty()) {
                    stepTimer++;
                    if (stepTimer >= stepIntervalTicks.get()) {
                        stepTimer = 0;
                        teleportTo(stepQueue.pollFirst());
                    }
                    return;
                }

                teleportTo(targetPos);
                finishGoingPhase();
            }
            case WAITING -> {
                if (waitTimer > 0) {
                    waitTimer--;
                    return;
                }

                buildSteps(mc.player.getPos(), startPos);
                phase = Phase.RETURNING;
                stepTimer = stepIntervalTicks.get();

                if (instant.get()) {
                    flushQueue(startPos);
                    toggle();
                }
            }
            case RETURNING -> {
                if (instant.get()) {
                    flushQueue(startPos);
                    toggle();
                    return;
                }

                if (!stepQueue.isEmpty()) {
                    stepTimer++;
                    if (stepTimer >= stepIntervalTicks.get()) {
                        stepTimer = 0;
                        teleportTo(stepQueue.pollFirst());
                    }
                    return;
                }

                teleportTo(startPos);
                toggle();
            }
            case IDLE -> {
            }
        }
    }

    private void flushQueue(Vec3d finalPos) {
        while (!stepQueue.isEmpty()) {
            teleportTo(stepQueue.pollFirst());
        }
        teleportTo(finalPos);
    }

    private void finishGoingPhase() {
        if (!goBack.get() || startPos == null) {
            toggle();
            return;
        }

        waitTimer = delayTicks.get();
        phase = Phase.WAITING;
    }

    private void buildSteps(Vec3d from, Vec3d to) {
        stepQueue.clear();
        double totalDistance = from.distanceTo(to);
        double step = Math.max(0.5, stepSize.get());

        if (totalDistance <= step) {
            stepQueue.addLast(to);
            return;
        }

        Vec3d direction = to.subtract(from).normalize();
        double traveled = step;

        while (traveled < totalDistance) {
            stepQueue.addLast(from.add(direction.multiply(traveled)));
            traveled += step;
        }

        stepQueue.addLast(to);
    }

    private void teleportTo(Vec3d pos) {
        if (mc.player == null) return;
        mc.player.setVelocity(0.0, 0.0, 0.0);
        mc.player.setPosition(pos.x, pos.y, pos.z);
        mc.player.resetPosition();
    }

    private Vec3d resolveTargetPos(BlockHitResult hit) {
        BlockPos hitPos = hit.getBlockPos();

        if (onTop.get()) {
            BlockPos topPos = hitPos.up();
            if (isPassable(topPos) && isPassable(topPos.up())) {
                return new Vec3d(topPos.getX() + 0.5, topPos.getY(), topPos.getZ() + 0.5);
            }
        }

        Direction side = hit.getSide();
        BlockPos adjPos = hitPos.offset(side);

        if (side == Direction.DOWN) {
            BlockPos below = hitPos.down(2);
            if (isPassable(below)) {
                return new Vec3d(hitPos.getX() + 0.5, below.getY(), hitPos.getZ() + 0.5);
            }
        }

        if (isPassable(adjPos) && isPassable(adjPos.up())) {
            return new Vec3d(adjPos.getX() + 0.5, adjPos.getY(), adjPos.getZ() + 0.5);
        }

        BlockPos aboveHit = hitPos.up();
        if (isPassable(aboveHit) && isPassable(aboveHit.up())) {
            return new Vec3d(aboveHit.getX() + 0.5, aboveHit.getY(), aboveHit.getZ() + 0.5);
        }

        return null;
    }

    private boolean isPassable(BlockPos pos) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(pos);
        return state.getCollisionShape(mc.world, pos).isEmpty();
    }

    private void resetState() {
        stepQueue.clear();
        startPos = null;
        targetPos = null;
        phase = Phase.IDLE;
        waitTimer = 0;
        stepTimer = 0;
    }

    private enum Phase {
        IDLE,
        GOING,
        WAITING,
        RETURNING
    }
}