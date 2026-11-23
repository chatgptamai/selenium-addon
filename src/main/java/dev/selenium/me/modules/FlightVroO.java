package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FlightVroO extends Module {
    public enum Mode {
        Velocity,
        Abilities
    }

    private final SettingGroup sgGeneral = settings.createGroup("mode");
    private final SettingGroup fuck = settings.createGroup("fuckflight");
    private final SettingGroup speedControls = settings.createGroup("speed controls");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.Velocity)
            .build()
    );

    private final DoubleSetting speed = (DoubleSetting) fuck.add(new DoubleSetting.Builder()
            .name("fly spede")
            .description("didnt u understand are u dumb its speed")
            .defaultValue(1.000)
            .sliderMax(10.000)
            .sliderMin(0.100)
            .min(0.100)
            .max(10.000)
            .build()
    );

    private final Setting<Boolean> verticalSpeedMatch = fuck.add(new BoolSetting.Builder()
            .name("vertical speed match")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> nofall = fuck.add(new BoolSetting.Builder()
            .name("nofall")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> antigotfucked = fuck.add(new BoolSetting.Builder()
            .name("anti kock")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> antigotfuckeddelay = fuck.add(new IntSetting.Builder()
            .name("anti kock delay")
            .defaultValue(20)
            .min(5)
            .max(80)
            .sliderMax(80)
            .visible(antigotfucked::get)
            .build()
    );

    private final Setting<Double> speedStep = speedControls.add(new DoubleSetting.Builder()
            .name("speed step")
            .defaultValue(0.05)
            .min(0.005)
            .max(1.0)
            .sliderRange(0.005, 0.5)
            .build()
    );

    private final Setting<Keybind> accelerateKey = speedControls.add(new KeybindSetting.Builder()
            .name("accelerate key")
            .defaultValue(Keybind.none())
            .build()
    );

    private final Setting<Keybind> slowKey = speedControls.add(new KeybindSetting.Builder()
            .name("slow key")
            .defaultValue(Keybind.none())
            .build()
    );

    private int floatTicks = 0;
    private int antiKickRecoverTicks = 0;
    private float defaultFlySpeed = 0.05f;
    private boolean hadDefaultFlySpeed = false;
    private float lastAppliedFlySpeed = -1f;

    public FlightVroO() {
        super(SeleniumAddon.CATEGORY, "FLY", "fly faster");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        floatTicks = 0;
        antiKickRecoverTicks = 0;
        lastAppliedFlySpeed = -1f;
        if (mc.player != null && !hadDefaultFlySpeed) {
            defaultFlySpeed = mc.player.getAbilities().getFlySpeed();
            hadDefaultFlySpeed = true;
        }
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        resetAbilities();
        floatTicks = 0;
        antiKickRecoverTicks = 0;
        lastAppliedFlySpeed = -1f;
    }

    private void resetAbilities() {
        if (mc.player == null) return;
        mc.player.getAbilities().flying = false;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().allowFlying = false;
        }
        if (hadDefaultFlySpeed) {
            mc.player.getAbilities().setFlySpeed(defaultFlySpeed);
        }
        mc.player.sendAbilitiesUpdate();
        lastAppliedFlySpeed = -1f;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (!hadDefaultFlySpeed) {
            defaultFlySpeed = mc.player.getAbilities().getFlySpeed();
            hadDefaultFlySpeed = true;
        }

        handleSpeedKeys();
        double spd = speed.get();
        double verticalSpd = verticalSpeedMatch.get() ? spd : Math.min(spd, 1.0);

        boolean jumping = mc.options.jumpKey.isPressed();
        boolean sneaking = mc.options.sneakKey.isPressed();

        if (mode.get() == Mode.Abilities) {
            floatTicks = 0;
            antiKickRecoverTicks = 0;

            float targetFlySpeed = MathHelper.clamp((float) (spd * 0.05), 0.01f, 0.5f);
            boolean stateChanged = !mc.player.getAbilities().allowFlying
                    || !mc.player.getAbilities().flying
                    || Math.abs(targetFlySpeed - lastAppliedFlySpeed) > 0.0001f;

            mc.player.getAbilities().allowFlying = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlySpeed(targetFlySpeed);

            if (stateChanged) {
                mc.player.sendAbilitiesUpdate();
                lastAppliedFlySpeed = targetFlySpeed;
            }

            if (jumping || sneaking) {
                Vec3d currentVel = mc.player.getVelocity();
                double abilityVerticalSpd = verticalSpeedMatch.get()
                        ? MathHelper.clamp(spd * 0.25, 0.15, 1.5)
                        : 0.18;
                double targetY = jumping ? abilityVerticalSpd : -abilityVerticalSpd;
                mc.player.setVelocity(new Vec3d(currentVel.x, targetY, currentVel.z));
            }

            if (nofall.get()) {
                mc.player.fallDistance = 0f;
            }
            return;
        }

        resetAbilitiesIfNeeded();

        double velX = 0;
        double velY = 0;
        double velZ = 0;

        if (jumping) {
            velY = verticalSpd;
            antiKickRecoverTicks = 0;
        } else if (sneaking) {
            velY = -verticalSpd;
            antiKickRecoverTicks = 0;
        }

        double forward = 0;
        double right = 0;

        if (mc.options.forwardKey.isPressed()) forward += 1;
        if (mc.options.backKey.isPressed()) forward -= 1;
        if (mc.options.rightKey.isPressed()) right += 1;
        if (mc.options.leftKey.isPressed()) right -= 1;

        if (forward != 0 || right != 0) {
            float yaw = mc.player.getYaw();
            Vec3d forwardDir = Vec3d.fromPolar(0f, yaw);
            Vec3d rightDir = forwardDir.rotateY(-MathHelper.HALF_PI);

            Vec3d moveDir = forwardDir.multiply(forward).add(rightDir.multiply(right));
            if (moveDir.lengthSquared() > 1.0) {
                moveDir = moveDir.normalize();
            }

            velX = moveDir.x * spd;
            velZ = moveDir.z * spd;
        }

        if (antigotfucked.get()) {
            if (!mc.player.isOnGround() && !jumping && !sneaking) {
                if (antiKickRecoverTicks > 0) {
                    velY = 0.04;
                    antiKickRecoverTicks--;
                } else {
                    floatTicks++;
                    if (floatTicks >= antigotfuckeddelay.get()) {
                        velY = -0.04;
                        floatTicks = 0;
                        antiKickRecoverTicks = 1;
                    }
                }
            } else {
                floatTicks = 0;
                antiKickRecoverTicks = 0;
            }
        } else {
            antiKickRecoverTicks = 0;
        }

        mc.player.setVelocity(new Vec3d(velX, velY, velZ));

        if (nofall.get()) {
            mc.player.fallDistance = 0f;
            if (mc.getNetworkHandler() != null && !mc.player.isOnGround() && mc.player.getVelocity().y < 0) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
            }
        }
    }

    private void resetAbilitiesIfNeeded() {
        if (mc.player != null && mc.player.getAbilities().flying && !mc.player.isCreative() && !mc.player.isSpectator()) {
            resetAbilities();
        }
    }

    private void handleSpeedKeys() {
        double step = speedStep.get();
        double current = speed.get();

        boolean accel = accelerateKey.get() != null && accelerateKey.get().isPressed();
        boolean slow = slowKey.get() != null && slowKey.get().isPressed();

        if (accel && !slow) {
            double next = MathHelper.clamp(current + step, speed.min, speed.max);
            speed.set(next);
        } else if (slow && !accel) {
            double next = MathHelper.clamp(current - step, speed.min, speed.max);
            speed.set(next);
        }
    }
}