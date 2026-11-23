package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;

public class macetotemsuckers extends Module {
    private final SettingGroup sgGeneral = this.settings.createGroup("genelkültür");
    private final SettingGroup sgHits = this.settings.createGroup("hitsvro");

    private final Setting<Integer> hitAmount = this.sgGeneral.add(
            new IntSetting.Builder()
                    .name("hit-amount")
                    .description("number of smash hits per trigger")
                    .defaultValue(3)
                    .min(1)
                    .max(4)
                    .sliderMin(1)
                    .sliderMax(3)
                    .build()
    );

    private final Setting<Integer> spoofPackets = this.sgGeneral.add(
            new IntSetting.Builder()
                    .name("spoof packets")
                    .description("ooff ground packets sent before each height spoof")
                    .defaultValue(4)
                    .min(1)
                    .max(10)
                    .sliderMin(1)
                    .sliderMax(10)
                    .build()
    );

    private final Setting<Double> range = this.sgGeneral.add(
            new DoubleSetting.Builder()
                    .name("range")
                    .description("max target range when swinging manually ")
                    .defaultValue(6.5)
                    .min(2.0)
                    .max(9.0)
                    .sliderMin(3.0)
                    .sliderMax(8.0)
                    .build()
    );

    private final Setting<Double> auraRange = this.sgGeneral.add(
            new DoubleSetting.Builder()
                    .name("aura-range")
                    .defaultValue(200.0)
                    .min(6.0)
                    .max(300.0)
                    .sliderMin(20.0)
                    .sliderMax(250.0)
                    .build()
    );

    private final Setting<Boolean> auraTrigger = this.sgGeneral.add(
            new BoolSetting.Builder()
                    .name("aura-trigger")
                    .defaultValue(true)
                    .build()
    );

    private final Setting<SwitchMode> autoSwitch = this.sgGeneral.add(
            new EnumSetting.Builder<SwitchMode>()
                    .name("auto-switch")
                    .defaultValue(SwitchMode.Silent)
                    .build()
    );

    @SuppressWarnings("unchecked")
    private final Setting<Integer>[] hitHeights = new Setting[3];
    private boolean running = false;

    public macetotemsuckers() {
        super(SeleniumAddon.CATEGORY, "MaceTotemFail", "fuck ur opponent with totem fail");

        for (int i = 0; i < 3; i++) {
            int index = i;
            int defaultHeight = 100 + i * 20;
            this.hitHeights[i] = this.sgHits.add(
                    new IntSetting.Builder()
                            .name("hit-" + (i + 1))
                            .description("Spoof fall height for hit " + (i + 1) + ".")
                            .defaultValue(defaultHeight)
                            .min(1)
                            .max(1000)
                            .sliderMin(10)
                            .sliderMax(500)
                            .visible(() -> this.hitAmount.get() > index)
                            .build()
            );
        }
    }

    @Override
    public void onActivate() {
        this.running = false;
    }

    @Override
    public void onDeactivate() {
        this.running = false;
    }

    @EventHandler
    private void onAttackEntity(AttackEntityEvent event) {
        if (this.running || this.mc.player == null || this.mc.world == null || this.mc.getNetworkHandler() == null) return;
        if (!(event.entity instanceof LivingEntity living)) return;

        double maxR = this.auraTrigger.get() ? Math.max(this.range.get(), this.auraRange.get()) : this.range.get();
        if (!this.validTarget(living, maxR)) return;

        FindItemResult mace = InvUtils.findInHotbar(s -> s.getItem() == Items.MACE);
        if (!mace.found()) return;

        this.running = true;
        boolean hasMaceInHand = this.mc.player.getMainHandStack().getItem() == Items.MACE;
        boolean swapped = false;

        try {
            if (!hasMaceInHand && this.autoSwitch.get() != SwitchMode.None) {
                swapped = InvUtils.swap(mace.slot(), this.autoSwitch.get() == SwitchMode.Silent);
            }

            this.hitEntity(living);
        } finally {
            if (swapped && this.autoSwitch.get() == SwitchMode.Silent) {
                InvUtils.swapBack();
            }
            this.running = false;
        }
    }

    public void hitEntity(Entity target) {
        if (this.mc.player == null || this.mc.world == null || this.mc.getNetworkHandler() == null || target == null) return;

        int currentHits = Math.min(3, Math.max(1, this.hitAmount.get()));
        boolean sneaking = this.mc.player.isSneaking();

        for (int i = 0; i < currentHits; i++) {
            int h = this.hitHeights[i].get();
            this.spoofTp(h, target);
            this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, sneaking));
            this.mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void spoofTp(int h, Entity target) {
        if (this.mc.player == null || this.mc.getNetworkHandler() == null) return;

        double x = target != null ? target.getX() : this.mc.player.getX();
        double y = this.mc.player.getY();
        double z = target != null ? target.getZ() : this.mc.player.getZ();

        for (int i = 0; i < this.spoofPackets.get(); i++) {
            this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(false));
        }

        PositionAndOnGround up = new PositionAndOnGround(x, y + (double) h, z, false);
        PositionAndOnGround down = new PositionAndOnGround(x, y, z, false);

        ((IPlayerMoveC2SPacket) up).setTag(1337);
        ((IPlayerMoveC2SPacket) down).setTag(1337);

        this.mc.getNetworkHandler().sendPacket(up);
        this.mc.getNetworkHandler().sendPacket(down);
    }

    private boolean validTarget(LivingEntity e, double maxR) {
        if (e == null || !e.isAlive() || e.isRemoved() || e.isDead() || e == this.mc.player || this.mc.player == null) {
            return false;
        }
        if (e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        return this.mc.player.distanceTo(e) <= maxR;
    }

    public enum SwitchMode {
        None,
        Normal,
        Silent
    }
}