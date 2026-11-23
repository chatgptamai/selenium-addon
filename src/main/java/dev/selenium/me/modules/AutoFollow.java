package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoFollow extends Module {
    private static final double SEARCH_RANGE = 512.0;
    private static final double SEARCH_RANGE_SQ = SEARCH_RANGE * SEARCH_RANGE;

    private final SettingGroup sgGeneral = settings.createGroup("generalyokbinbaşıamk");
    private final SettingGroup sgMovement = settings.createGroup("movementmahmut");

    private final Setting<Boolean> playersOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("players only")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore friends")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> sideDistance = sgMovement.add(new DoubleSetting.Builder()
            .name("side distance")
            .defaultValue(2.0)
            .min(0.5)
            .max(8.0)
            .sliderRange(1.0, 5.0)
            .build()
    );

    private final Setting<Double> heightOffset = sgMovement.add(new DoubleSetting.Builder()
            .name("height offset")
            .defaultValue(0.25)
            .min(-3.0)
            .max(4.0)
            .sliderRange(-1.5, 2.0)
            .build()
    );

    private int trackedTargetId = -1;
    private Vec3d lastTargetPos = null;

    public AutoFollow() {
        super(SeleniumAddon.CATEGORY, "Fly Target", "follows the nearest enemy");
    }

    @Override
    public void onActivate() {
        trackedTargetId = -1;
        lastTargetPos = null;
    }

    @Override
    public void onDeactivate() {
        trackedTargetId = -1;
        lastTargetPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = findNearestTarget();
        if (target == null) {
            trackedTargetId = -1;
            lastTargetPos = null;
            return;
        }

        Vec3d currentTargetPos = target.getPos();
        Vec3d targetDelta = Vec3d.ZERO;
        if (trackedTargetId == target.getId() && lastTargetPos != null) {
            targetDelta = currentTargetPos.subtract(lastTargetPos);
        }
        trackedTargetId = target.getId();
        lastTargetPos = currentTargetPos;

        double velSpeed = target.getVelocity().length();
        double deltaSpeed = targetDelta.length();
        double targetSpeed = Math.max(velSpeed, deltaSpeed);

        Vec3d predictedPos = currentTargetPos.add(targetDelta.multiply(1.0));

        float yawRad = (float) Math.toRadians(target.getYaw());
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        double desiredY = predictedPos.y + heightOffset.get();

        Vec3d leftPos = new Vec3d(
                predictedPos.x - rightX * sideDistance.get(),
                desiredY,
                predictedPos.z - rightZ * sideDistance.get()
        );
        Vec3d rightPos = new Vec3d(
                predictedPos.x + rightX * sideDistance.get(),
                desiredY,
                predictedPos.z + rightZ * sideDistance.get()
        );

        Vec3d goalPos = mc.player.squaredDistanceTo(leftPos) <= mc.player.squaredDistanceTo(rightPos)
                ? leftPos
                : rightPos;

        Vec3d toGoal = goalPos.subtract(mc.player.getPos());
        double distance = toGoal.length();
        double speed = computeAutoSpeed(distance, targetSpeed);

        Vec3d nextVelocity;
        if (distance > 0.02) {
            Vec3d dir = toGoal.normalize();
            nextVelocity = dir.multiply(Math.min(speed, distance));
        } else {
            nextVelocity = targetDelta.lengthSquared() > 1.0e-5
                    ? targetDelta
                    : new Vec3d(target.getVelocity().x, 0.0, target.getVelocity().z);
        }

        mc.player.fallDistance = 0.0f;
        mc.player.setVelocity(nextVelocity);
    }

    private double computeAutoSpeed(double distance, double targetSpeed) {
        double minMatchSpeed = targetSpeed * 1.15;

        if (distance < 0.25) {
            return Math.min(0.2 + targetSpeed * 0.8, Math.max(0.15, targetSpeed));
        }

        double distFactor = MathHelper.clamp(distance / 18.0, 0.0, 1.0);
        double catchUp = MathHelper.lerp(distFactor, 0.45, 3.6);

        double dynamicCap = Math.max(3.6, targetSpeed * 1.45 + 0.5);
        return Math.clamp(Math.max(catchUp, minMatchSpeed), 0.15, dynamicCap);
    }

    private LivingEntity findNearestTarget() {
        LivingEntity nearest = null;
        double bestDistSq = SEARCH_RANGE_SQ;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!isValidTarget(living)) continue;

            double distSq = mc.player.squaredDistanceTo(living);
            if (distSq <= bestDistSq) {
                bestDistSq = distSq;
                nearest = living;
            }
        }

        return nearest;
    }

    private boolean isValidTarget(LivingEntity living) {
        if (!living.isAlive() || living.isRemoved()) return false;
        if (living.deathTime > 0 || living.getHealth() <= 0.0f) return false;

        if (living instanceof PlayerEntity player) {
            if (player.isSpectator()) return false;
            if (ignoreFriends.get() && Friends.get().isFriend(player)) return false;
        } else if (playersOnly.get()) {
            return false;
        }

        return true;
    }
}