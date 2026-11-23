package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.util.Hand;

public class Fuckback extends Module {
    private static final double RANGE = 5.0;
    private static final int DELAY_TICKS = 1;

    private final SettingGroup sgfuck = settings.createGroup("fuck");

    private final Setting<Boolean> playersOnly = sgfuck.add(new BoolSetting.Builder()
            .name("players only")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreFriends = sgfuck.add(new BoolSetting.Builder()
            .name("ignore friends")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> turnonAura = sgfuck.add(new BoolSetting.Builder()
            .name("turn on aura")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> autoTurnOff = sgfuck.add(new BoolSetting.Builder()
            .name("auto turn off")
            .description("turns off InfiniteAura after the attacked target dies or disappears")
            .defaultValue(true)
            .visible(turnonAura::get)
            .build()
    );

    private int pendingAttackerId = -1;
    private int trackedTargetId = -1;
    private int waitCounter = 0;

    public Fuckback() {
        super(SeleniumAddon.CATEGORY, "Fuckback", "hitback");
    }

    @Override
    public void onActivate() {
        pendingAttackerId = -1;
        trackedTargetId = -1;
        waitCounter = 0;
    }

    @Override
    public void onDeactivate() {
        pendingAttackerId = -1;
        trackedTargetId = -1;
        waitCounter = 0;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;
        if (!(event.packet instanceof EntityDamageS2CPacket packet)) return;
        if (packet.entityId() != mc.player.getId()) return;

        int attackerId = packet.sourceCauseId();
        if (attackerId == -1) attackerId = packet.sourceDirectId();
        if (attackerId != -1 && attackerId != mc.player.getId()) {
            Entity attacker = mc.world.getEntityById(attackerId);
            if (!shouldTriggerOnAttacker(attacker)) return;

            pendingAttackerId = attackerId;
            trackedTargetId = attackerId;
            waitCounter = DELAY_TICKS;
            enableInfiniteAuraIfNeeded();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        handleTrackedTargetDeath();

        if (pendingAttackerId == -1 && mc.player.hurtTime > 0) {
            LivingEntity attacker = mc.player.getAttacker();
            if (attacker != null && attacker != mc.player && shouldTriggerOnAttacker(attacker)) {
                pendingAttackerId = attacker.getId();
                trackedTargetId = attacker.getId();
                waitCounter = DELAY_TICKS;
                enableInfiniteAuraIfNeeded();
            }
        }

        if (pendingAttackerId == -1) return;

        if (waitCounter > 0) {
            waitCounter--;
            return;
        }

        Entity target = mc.world.getEntityById(pendingAttackerId);
        pendingAttackerId = -1;

        if (!isValidTarget(target)) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;

        trackedTargetId = target.getId();
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void handleTrackedTargetDeath() {
        if (!autoTurnOff.get() || trackedTargetId == -1) return;

        Entity tracked = mc.world.getEntityById(trackedTargetId);
        boolean deadOrGone = !(tracked instanceof LivingEntity living)
                || !living.isAlive()
                || living.isRemoved()
                || living.getHealth() <= 0.0f;

        if (deadOrGone) {
            trackedTargetId = -1;
            disableInfiniteAuraIfNeeded();
        }
    }

    private void enableInfiniteAuraIfNeeded() {
        if (!turnonAura.get()) return;
        Module infiniteAura = Modules.get().get("InfiniteAura");
        if (infiniteAura != null && !infiniteAura.isActive()) {
            infiniteAura.toggle();
        }
    }

    private void disableInfiniteAuraIfNeeded() {
        Module infiniteAura = Modules.get().get("InfiniteAura");
        if (infiniteAura != null && infiniteAura.isActive()) {
            infiniteAura.toggle();
        }
    }

    private boolean shouldTriggerOnAttacker(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive() || living.isRemoved()) return false;

        if (entity instanceof PlayerEntity player) {
            if (ignoreFriends.get() && Friends.get().isFriend(player)) return false;
        } else if (playersOnly.get()) {
            return false;
        }

        return true;
    }

    private boolean isValidTarget(Entity entity) {
        if (!shouldTriggerOnAttacker(entity)) return false;
        if (mc.player.distanceTo(entity) > RANGE) return false;
        return true;
    }
}