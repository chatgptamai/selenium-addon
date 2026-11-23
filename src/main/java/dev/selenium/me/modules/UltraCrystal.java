package dev.selenium.me.modules;


import dev.selenium.me.SeleniumAddon;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class UltraCrystal extends Module {
    private static final UltraCrystal.AutoSwitch Normal = AutoSwitch.Normal ;
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgTarget = this.settings.createGroup("Target");
    private final SettingGroup sgRender = this.settings.createGroup("Render");
    private final Setting<Set<EntityType<?>>> entities = this.sgTarget
            .add(
                    ((Builder)new Builder().name("entities"))
                            .onlyAttackable()
                            .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.WARDEN, EntityType.WITHER})
                            .build()
            );
    private final Setting<Double> placeRange = this.sgTarget
            .add(
                    ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                            .name("place-range"))
                            .defaultValue(6.0)
                            .min(0.0)
                            .sliderMax(6.0)
                            .build()
            );
    private final Setting<Double> breakRange = this.sgTarget
            .add(
                    ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                            .name("break-range"))
                            .defaultValue(6.0)
                            .min(0.0)
                            .sliderMax(6.0)
                            .build()
            );
    private final Setting<SortPriority> priority = this.sgTarget
            .add(
                    ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                            .name("priority"))
                            .defaultValue(SortPriority.LowestHealth))
                            .build()
            );
    private final Setting<Boolean> ignoreFriends = this.sgTarget
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("ignore-friends"))
                            .defaultValue(true))
                            .build()
            );
    private final Setting<Boolean> ignoreBots = this.sgTarget
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("ignore-bots"))
                            .defaultValue(true))
                            .build()
            );
    private final Setting<Double> minDamage = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("min-damage"))
                            .defaultValue(6.0)
                            .min(0.0)
                            .sliderMax(36.0)
                            .build()
            );
    private final Setting<Double> maxSelfDamage = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                            .name("max-self-damage"))
                            .defaultValue(16.0)
                            .min(0.0)
                            .sliderMax(36.0)
                            .build()
            );
    private final Setting<Integer> placeDelay = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                            .name("place-delay"))
                            .defaultValue(0))
                            .min(0)
                            .sliderMax(20)
                            .build()
            );
    private final Setting<Integer> breakDelay = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                            .name("break-delay"))
                            .defaultValue(0))
                            .min(0)
                            .sliderMax(20)
                            .build()
            );
    private final Setting<UltraCrystal.AutoSwitch> autoSwitch = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                            .name("auto-switch"))
                            .defaultValue(UltraCrystal.AutoSwitch.Silent))
                            .build()
            );
    private final Setting<Boolean> rotate = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("rotate"))
                            .defaultValue(false))
                            .build()
            );
    private final Setting<Boolean> autoRefill = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("auto-refill"))
                            .defaultValue(true))
                            .build()
            );
    private final Setting<Boolean> pauseOnEat = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("pause-on-eat"))
                            .defaultValue(true))
                            .build()
            );
    private final Setting<UltraCrystal.SwingMode> swingMode = this.sgRender
            .add(
                    ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                            .name("swing-mode"))
                            .defaultValue(UltraCrystal.SwingMode.Client))
                            .build()
            );
    private final Setting<Boolean> renderPlace = this.sgRender
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("render-place"))
                            .defaultValue(true))
                            .build()
            );
    private final Setting<ShapeMode> placeRenderMode = this.sgRender
            .add(
                    ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                            .name("place-render-mode"))
                            .defaultValue(ShapeMode.Sides))
                            .visible(this.renderPlace::get))
                            .build()
            );
    private final Setting<SettingColor> placeFillColor = this.sgRender
            .add(
                    ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                            .name("place-fill-color"))
                            .defaultValue(new SettingColor(90, 0, 150, 50))
                            .visible(this.renderPlace::get))
                            .build()
            );
    private final Setting<SettingColor> placeOutlineColor = this.sgRender
            .add(
                    ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                            .name("place-outline-color"))
                            .defaultValue(new SettingColor(90, 0, 150, 255))
                            .visible(this.renderPlace::get))
                            .build()
            );
    private Entity target = null;
    private BlockPos bestBase = null;
    private final Map<BlockPos, Long> recentPlacements = new HashMap<>();
    private int lastCrystalSlot = -1;
    private int placeTickCounter = 0;
    private int breakTickCounter = 0;

    public UltraCrystal() {
        super(SeleniumAddon.CATEGORY, "SuperCrystal", "High-Speed AutoCrystal.");
    }

    public void onActivate() {
        this.target = null;
        this.bestBase = null;
        this.recentPlacements.clear();
        this.lastCrystalSlot = -1;
        this.placeTickCounter = 0;
        this.breakTickCounter = 0;
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player != null && this.mc.world != null) {
            if ((Boolean)this.pauseOnEat.get() && this.mc.player.isUsingItem()) {
                this.target = null;
                this.bestBase = null;
            } else {
                this.recentPlacements.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 40L);
                if (this.placeTickCounter > 0) {
                    this.placeTickCounter--;
                }

                if (this.breakTickCounter > 0) {
                    this.breakTickCounter--;
                }

                this.target = TargetUtils.get(e -> {
                    if (e instanceof LivingEntity && e != this.mc.player && e.isAlive()) {
                        if (!((Set)this.entities.get()).contains(e.getType())) {
                            return false;
                        } else {
                            if ((Boolean)this.ignoreFriends.get() && e instanceof PlayerEntity p && Friends.get().isFriend(p)) {
                                return false;
                            }

                            if ((Boolean)this.ignoreBots.get() && e instanceof PlayerEntity p && this.mc.getNetworkHandler().getPlayerListEntry(p.getUuid()) == null) {
                                return false;
                            }

                            if (e instanceof TameableEntity t && t.isTamed()) {
                                return false;
                            }

                            return true;
                        }
                    } else {
                        return false;
                    }
                }, (SortPriority)this.priority.get());
                if (this.target == null) {
                    this.bestBase = null;
                } else {
                    if (this.breakTickCounter <= 0) {
                        this.tryBreakCrystalLoop();
                    }

                    if (this.placeTickCounter <= 0) {
                        List<BlockPos> candidates = this.collectPlacePositions();
                        if (!candidates.isEmpty()) {
                            candidates.sort(Comparator.<BlockPos>comparingDouble(pos -> this.score(pos.up())).reversed());
                            BlockPos absoluteBest = candidates.get(0);
                            this.bestBase = absoluteBest;
                            this.tryPlaceCrystalAt(absoluteBest);
                        } else {
                            this.bestBase = null;
                        }
                    }
                }
            }
        }
    }

    private double score(BlockPos crystalPos) {
        return this.calculateDamage(this.target, crystalPos) - this.calculateDamage(this.mc.player, crystalPos) * 0.1;
    }

    private List<BlockPos> collectPlacePositions() {
        List<BlockPos> result = new ArrayList<>();
        BlockPos feet = this.mc.player.getBlockPos();
        double rangeSq = (Double)this.placeRange.get() * (Double)this.placeRange.get();
        int rangeI = (int)Math.ceil((Double)this.placeRange.get());

        for (int x = -rangeI; x <= rangeI; x++) {
            for (int y = -rangeI; y <= rangeI; y++) {
                for (int z = -rangeI; z <= rangeI; z++) {
                    BlockPos base = feet.add(x, y, z);
                    if (this.isValidBase(base) && !this.recentPlacements.containsKey(base)) {
                        BlockPos up = base.up();
                        double distSq = this.mc
                                .player
                                .squaredDistanceTo((double)up.getX() + 0.5, (double)up.getY() + 0.5, (double)up.getZ() + 0.5);
                        if (!(distSq > rangeSq)) {
                            double targetDmg = this.calculateDamage(this.target, up);
                            double selfDmg = this.calculateDamage(this.mc.player, up);
                            if (!(targetDmg < (Double)this.minDamage.get())
                                    && !(selfDmg > (Double)this.maxSelfDamage.get())
                                    && (!(Boolean)this.ignoreBots.get() || !this.wouldHitIgnoredBot(up))) {
                                result.add(base);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private boolean isBot(Entity entity) {
        return entity instanceof PlayerEntity player ? this.mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null : false;
    }

    private boolean wouldHitIgnoredBot(BlockPos crystalPos) {
        for (Entity e : this.mc.world.getEntities()) {
            if (e instanceof PlayerEntity p && p != this.mc.player && this.isBot(p) && this.calculateDamage(p, crystalPos) >= (Double)this.minDamage.get()) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidBase(BlockPos pos) {
        BlockState state = this.mc.world.getBlockState(pos);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) {
            return false;
        } else {
            BlockPos up = pos.up();
            if (!this.mc.world.getBlockState(up).isAir()) {
                return false;
            } else {
                BlockPos playerFeet = this.mc.player.getBlockPos();
                if (!up.equals(playerFeet) && !up.equals(playerFeet.up())) {
                    Box crystalBox = new Box(
                            (double)up.getX(),
                            (double)up.getY(),
                            (double)up.getZ(),
                            (double)up.getX() + 1.0,
                            (double)up.getY() + 1.0,
                            (double)up.getZ() + 1.0
                    );

                    for (Entity e : this.mc.world.getEntities()) {
                        if (e != this.mc.player && e.isAlive() && !(e instanceof EndCrystalEntity) && e.getBoundingBox().intersects(crystalBox)) {
                            return false;
                        }
                    }

                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private void tryPlaceCrystalAt(BlockPos base) {
        if (this.placeCrystal(base.up())) {
            this.recentPlacements.put(base, System.currentTimeMillis());
            this.placeTickCounter = (Integer)this.placeDelay.get();
        }
    }

    private boolean placeCrystal(BlockPos pos) {
        BlockPos below = pos.down();
        BlockHitResult hit = new BlockHitResult(
                new Vec3d((double)below.getX() + 0.5, (double)below.getY() + 1.0, (double)below.getZ() + 0.5),
                Direction.UP,
                below,
                false
        );
        if (this.mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
            this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hit, 0));
            this.doSwing(Hand.OFF_HAND);
            return true;
        } else {
            FindItemResult crystals = InvUtils.findInHotbar(s -> s.getItem() == Items.END_CRYSTAL);
            if (crystals.found()) {
                this.lastCrystalSlot = crystals.slot();
                switch ((UltraCrystal.AutoSwitch)this.autoSwitch.get()) {
                    case Normal:
                        InvUtils.swap(crystals.slot(), false);
                        this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
                        this.doSwing(Hand.MAIN_HAND);
                        return true;
                    case Silent:
                        int prev = this.mc.player.getInventory().selectedSlot;
                        this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(crystals.slot()));
                        this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
                        this.doSwing(Hand.MAIN_HAND);
                        this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prev));
                        return true;
                    case Off:
                        if (this.mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
                            return false;
                        }

                        this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
                        this.doSwing(Hand.MAIN_HAND);
                        return true;
                    default:
                        return false;
                }
            } else {
                if ((Boolean)this.autoRefill.get()) {
                    FindItemResult inInv = InvUtils.find(s -> s.getItem() == Items.END_CRYSTAL);
                    if (!inInv.found()) {
                        return false;
                    }

                    int targetSlot = this.lastCrystalSlot != -1 ? this.lastCrystalSlot : this.mc.player.getInventory().selectedSlot;
                    InvUtils.move().from(inInv.slot()).toHotbar(targetSlot);
                }

                return false;
            }
        }
    }

    private void tryBreakCrystalLoop() {
        double rangeSq = (Double)this.breakRange.get() * (Double)this.breakRange.get();
        List<EndCrystalEntity> toAttack = new ArrayList<>();

        for (Entity e : this.mc.world.getEntities()) {
            if (e instanceof EndCrystalEntity) {
                EndCrystalEntity crystal = (EndCrystalEntity)e;
                if (!crystal.isRemoved() && !(this.mc.player.squaredDistanceTo(crystal) > rangeSq)) {
                    BlockPos crystalPos = crystal.getBlockPos();
                    double targetDmg = this.calculateDamage(this.target, crystalPos);
                    double selfDmg = this.calculateDamage(this.mc.player, crystalPos);
                    if (!(targetDmg < (Double)this.minDamage.get()) && !(selfDmg > (Double)this.maxSelfDamage.get())) {
                        toAttack.add(crystal);
                    }
                }
            }
        }

        for (EndCrystalEntity crystal : toAttack) {
            this.attackCrystal(crystal);
        }
    }

    private void attackCrystal(EndCrystalEntity crystal) {
        if ((Boolean)this.rotate.get()) {
            Rotations.rotate(Rotations.getYaw(crystal), Rotations.getPitch(crystal));
        }

        this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, this.mc.player.isSneaking()));
        this.doSwing(Hand.MAIN_HAND);
        crystal.discard();
        this.breakTickCounter = (Integer)this.breakDelay.get();
    }

    private void doSwing(Hand hand) {
        switch ((UltraCrystal.SwingMode)this.swingMode.get()) {
            case Packet:
                this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
                break;
            case Client:
                this.mc.player.swingHand(hand);
            case None:
        }
    }

    private double calculateDamage(Entity target, BlockPos crystalPos) {
        Vec3d explosion = new Vec3d(
                (double)crystalPos.getX() + 0.5, (double)crystalPos.getY() + 0.5, (double)crystalPos.getZ() + 0.5
        );
        double dist = Math.sqrt(
                Math.pow(target.getX() - explosion.x, 2.0)
                        + Math.pow(target.getY() - explosion.y, 2.0)
                        + Math.pow(target.getZ() - explosion.z, 2.0)
        );
        double blastRadius = 12.0;
        if (dist > blastRadius) {
            return 0.0;
        } else {
            double exposure = this.getExposure(explosion, target);
            if (exposure <= 0.0) {
                return 0.0;
            } else {
                double impact = (1.0 - dist / blastRadius) * exposure;
                double baseDmg = impact * impact * 7.0 * blastRadius;
                if (target instanceof LivingEntity living) {
                    if (target instanceof PlayerEntity player) {
                        float armor = (float)player.getArmor();
                        double toughness = player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
                        double armorReduction = Math.max((double)armor / 5.0, (double)armor - baseDmg / (2.0 + toughness / 4.0));
                        armorReduction = Math.min(20.0, armorReduction);
                        baseDmg *= 1.0 - armorReduction / 25.0;
                        int epf = 0;
                        epf += this.getEpf(player.getEquippedStack(EquipmentSlot.HEAD), Enchantments.PROTECTION);
                        epf += this.getEpf(player.getEquippedStack(EquipmentSlot.HEAD), Enchantments.BLAST_PROTECTION);
                        epf += this.getEpf(player.getEquippedStack(EquipmentSlot.CHEST), Enchantments.PROTECTION);
                        epf += this.getEpf(player.getEquippedStack(EquipmentSlot.CHEST), Enchantments.BLAST_PROTECTION);
                        epf += this.getEpf(player.getEquippedStack(EquipmentSlot.LEGS), Enchantments.PROTECTION);
                        epf += this.getEpf(player.getEquippedStack(EquipmentSlot.LEGS), Enchantments.BLAST_PROTECTION);
                        epf += this.getEpf(player.getEquippedStack(EquipmentSlot.FEET), Enchantments.PROTECTION);
                        epf += this.getEpf(player.getEquippedStack(EquipmentSlot.FEET), Enchantments.BLAST_PROTECTION);
                        epf = Math.min(epf, 20);
                        baseDmg *= 1.0 - (double)epf * 0.04;
                        StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
                        if (resistance != null) {
                            baseDmg *= Math.max(0.0, 1.0 - (double)(resistance.getAmplifier() + 1) * 0.2);
                        }
                    } else {
                        double armorReduction = Math.min(20.0, (double)living.getArmor() / 5.0);
                        baseDmg *= 1.0 - armorReduction / 25.0;
                    }

                    return Math.max(0.0, baseDmg);
                } else {
                    return Math.max(0.0, baseDmg);
                }
            }
        }
    }

    private double getExposure(Vec3d source, Entity target) {
        Box box = target.getBoundingBox();
        double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        if (!(stepX < 0.0) && !(stepY < 0.0) && !(stepZ < 0.0)) {
            double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
            double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
            int hits = 0;
            int total = 0;

            for (double x = 0.0; x <= 1.0; x += stepX) {
                for (double y = 0.0; y <= 1.0; y += stepY) {
                    for (double z = 0.0; z <= 1.0; z += stepZ) {
                        double px = MathHelper.lerp(x, box.minX, box.maxX) + offsetX;
                        double py = MathHelper.lerp(y, box.minY, box.maxY);
                        double pz = MathHelper.lerp(z, box.minZ, box.maxZ) + offsetZ;
                        Vec3d point = new Vec3d(px, py, pz);
                        RaycastContext ctx = new RaycastContext(point, source, ShapeType.COLLIDER, FluidHandling.NONE, target);
                        if (this.mc.world.raycast(ctx).getType() == Type.MISS) {
                            hits++;
                        }

                        total++;
                    }
                }
            }

            return total == 0 ? 0.0 : (double)hits / (double)total;
        } else {
            return 0.0;
        }
    }

    private int getEpf(ItemStack stack, RegistryKey<Enchantment> key) {
        ItemEnchantmentsComponent enchantments = stack.getEnchantments();

        for (Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantmentEntry = (RegistryEntry<Enchantment>)entry.getKey();
            if (enchantmentEntry.matchesKey(key)) {
                return Math.min(entry.getIntValue(), 4);
            }
        }

        return 0;
    }

    @EventHandler
    private void onRender3D(Render3DEvent e) {
        if (this.mc.player != null && this.mc.world != null) {
            if ((Boolean)this.renderPlace.get() && this.bestBase != null) {
                e.renderer.box(this.bestBase, (Color)this.placeFillColor.get(), (Color)this.placeOutlineColor.get(), (ShapeMode)this.placeRenderMode.get(), 0);
            }
        }
    }

    public String getInfoString() {
        return this.target != null ? this.target.getName().getString() : null;
    }

    public static enum AutoSwitch {
        Normal,
        Silent,
        Off;
    }

    public static enum SwingMode {
        Packet,
        Client,
        None;
    }
}
