package dev.selenium.me.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class InfiniteAura extends Module {
    public static boolean active = false;
    public static Vec3d currentDesyncPos = null;

    private final SettingGroup sgMode;
    private final SettingGroup sgGeneral;
    private final SettingGroup sgTarget;
    private final SettingGroup sgRender;

    private final Setting<AuraMode> auraMode;
    private final Setting<Boolean> onlyHitback;
    private final Setting<Double> normalRange;
    private final Setting<Double> infiniteRange;
    private final Setting<Double> maxStep;
    private final Setting<AutoSwitch> autoSwitch;
    private final Setting<List<Item>> weapon;
    private final Setting<Boolean> customDelay;
    private final Setting<Integer> attackDelay;
    private final Setting<Double> horizontalOffset;
    private final Setting<Double> yOffset;

    private final Setting<Set<EntityType<?>>> entities;
    private final Setting<Integer> maxTargets;
    private final Setting<Boolean> ignoreFriends;
    private final Setting<Boolean> ignoreBots;
    private final Setting<Boolean> ignoreCreative;
    private final Setting<Boolean> disableOnLeave;
    private final Setting<SortMode> sortMode;

    private final Setting<Boolean> renderTarget;
    private final Setting<SettingColor> targetColor;
    private final Setting<Boolean> renderPath;
    private final Setting<SettingColor> pathColor;
    private final Setting<SwingMode> swingMode;

    private int delayTimer = 0;
    private List<Entity> currentTargets = new ArrayList<>();
    private SmartTPCore core;
    private final List<Vec3d> renderPathNodes = new ArrayList<>();
    private int packetsThisTick = 0;
    private long lastPacketTime = 0L;
    private final Map<UUID, Long> hitbackTargets = new HashMap<>();

    public InfiniteAura() {
        super(SeleniumAddon.CATEGORY, "InfiniteAura", "INFINITE AURA COPIED FROM AMETIUM SUPER AURA.");

        this.sgMode = this.settings.createGroup("Mode");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgTarget = this.settings.createGroup("Target");
        this.sgRender = this.settings.createGroup("Render");

        this.auraMode = this.sgMode.add(
                new EnumSetting.Builder<AuraMode>()
                        .name("mode")
                        .defaultValue(AuraMode.Normal)
                        .build()
        );

        this.onlyHitback = this.sgMode.add(
                new BoolSetting.Builder()
                        .name("Only HitBack")
                        .defaultValue(false)
                        .build()
        );

        this.normalRange = this.sgGeneral.add(
                new DoubleSetting.Builder()
                        .name("Range ")
                        .defaultValue(6.0)
                        .min(0.0)
                        .sliderMax(6.0)
                        .visible(() -> this.auraMode.get() == AuraMode.Normal)
                        .build()
        );

        this.infiniteRange = this.sgGeneral.add(
                new DoubleSetting.Builder()
                        .name("Range")
                        .defaultValue(200.0)
                        .min(0.0)
                        .sliderMax(200.0)
                        .visible(() -> this.auraMode.get() == AuraMode.Infinite)
                        .build()
        );

        this.maxStep = this.sgGeneral.add(
                new DoubleSetting.Builder()
                        .name("max-step")
                        .defaultValue(100.0)
                        .min(1.0)
                        .sliderMax(100.0)
                        .visible(() -> this.auraMode.get() == AuraMode.Infinite)
                        .build()
        );

        this.autoSwitch = this.sgGeneral.add(
                new EnumSetting.Builder<AutoSwitch>()
                        .name("auto-switch")
                        .defaultValue(AutoSwitch.Silent)
                        .build()
        );

        this.weapon = this.sgGeneral.add(
                new ItemListSetting.Builder()
                        .name("weapon")
                        .defaultValue(
                                Arrays.asList(
                                        Items.WOODEN_SWORD,
                                        Items.STONE_SWORD,
                                        Items.IRON_SWORD,
                                        Items.GOLDEN_SWORD,
                                        Items.DIAMOND_SWORD,
                                        Items.NETHERITE_SWORD,
                                        Items.WOODEN_AXE,
                                        Items.STONE_AXE,
                                        Items.IRON_AXE,
                                        Items.GOLDEN_AXE,
                                        Items.DIAMOND_AXE,
                                        Items.NETHERITE_AXE,
                                        Items.TRIDENT,
                                        Items.MACE
                                )
                        )
                        .filter(item -> {
                            String id = Registries.ITEM.getId(item).getPath();
                            return id.contains("_sword") || id.contains("_axe") || id.contains("trident") || id.contains("mace");
                        })
                        .build()
        );

        this.customDelay = this.sgGeneral.add(
                new BoolSetting.Builder()
                        .name("custom-delay")
                        .defaultValue(false)
                        .build()
        );

        this.attackDelay = this.sgGeneral.add(
                new IntSetting.Builder()
                        .name("attack-delay")
                        .defaultValue(10)
                        .min(0)
                        .sliderMax(20)
                        .visible(this.customDelay::get)
                        .build()
        );

        this.horizontalOffset = this.sgGeneral.add(
                new DoubleSetting.Builder()
                        .name("horizontal-offset")
                        .defaultValue(0.0)
                        .min(0.0)
                        .sliderMax(1.0)
                        .visible(() -> this.auraMode.get() == AuraMode.Infinite)
                        .build()
        );

        this.yOffset = this.sgGeneral.add(
                new DoubleSetting.Builder()
                        .name("y-offset")
                        .defaultValue(0.01)
                        .min(0.0)
                        .sliderMax(1.0)
                        .visible(() -> this.auraMode.get() == AuraMode.Infinite)
                        .build()
        );

        this.entities = this.sgTarget.add(
                new EntityTypeListSetting.Builder()
                        .name("targets")
                        .defaultValue(EntityType.PLAYER)
                        .build()
        );

        this.maxTargets = this.sgTarget.add(
                new IntSetting.Builder()
                        .name("max-targets")
                        .defaultValue(5)
                        .min(1)
                        .sliderMax(10)
                        .build()
        );

        this.ignoreFriends = this.sgTarget.add(
                new BoolSetting.Builder()
                        .name("ignore-friends")
                        .defaultValue(true)
                        .build()
        );

        this.ignoreBots = this.sgTarget.add(
                new BoolSetting.Builder()
                        .name("ignore-bots")
                        .defaultValue(true)
                        .build()
        );

        this.ignoreCreative = this.sgTarget.add(
                new BoolSetting.Builder()
                        .name("ignore-creative")
                        .defaultValue(true)
                        .build()
        );

        this.disableOnLeave = this.sgTarget.add(
                new BoolSetting.Builder()
                        .name("disable-leave")
                        .defaultValue(true)
                        .build()
        );

        this.sortMode = this.sgTarget.add(
                new EnumSetting.Builder<SortMode>()
                        .name("Priority")
                        .defaultValue(SortMode.Closest)
                        .build()
        );

        this.renderTarget = this.sgRender.add(
                new BoolSetting.Builder()
                        .name("render-target")
                        .defaultValue(true)
                        .build()
        );

        this.targetColor = this.sgRender.add(
                new ColorSetting.Builder()
                        .name("target-color")
                        .defaultValue(new SettingColor(150, 0, 0, 255))
                        .visible(this.renderTarget::get)
                        .build()
        );

        this.renderPath = this.sgRender.add(
                new BoolSetting.Builder()
                        .name("render-path")
                        .defaultValue(false)
                        .visible(() -> this.auraMode.get() == AuraMode.Infinite)
                        .build()
        );

        this.pathColor = this.sgRender.add(
                new ColorSetting.Builder()
                        .name("path-color")
                        .defaultValue(new SettingColor(0, 150, 0, 255))
                        .visible(() -> this.renderPath.get() && this.auraMode.get() == AuraMode.Infinite)
                        .build()
        );

        this.swingMode = this.sgRender.add(
                new EnumSetting.Builder<SwingMode>()
                        .name("swing-mode")
                        .defaultValue(SwingMode.Client)
                        .build()
        );
    }

    public static Vec3d getCurrentDesyncPos() {
        return currentDesyncPos;
    }

    @Override
    public void onActivate() {
        this.delayTimer = 0;
        this.currentTargets.clear();
        active = true;
        currentDesyncPos = null;
        if (this.mc.player != null && this.mc.world != null) {
            this.core = new SmartTPCore(this.mc.world, this.mc.player);
        }
    }

    @Override
    public void onDeactivate() {
        active = false;
        this.hitbackTargets.clear();
        currentDesyncPos = null;
    }

    @EventHandler
    private void onPacketReceive(Receive event) {
        if (event.packet instanceof EntityDamageS2CPacket packet) {
            if (this.mc.player != null && this.mc.world != null) {
                Entity target = this.mc.world.getEntityById(packet.entityId());
                if (target == this.mc.player) {
                    Entity attacker = this.mc.world.getEntityById(packet.sourceCauseId());
                    if (attacker instanceof LivingEntity) {
                        this.hitbackTargets.put(attacker.getUuid(), System.currentTimeMillis());
                    }
                }
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (this.disableOnLeave.get()) {
            this.toggle();
        }
    }

    @EventHandler
    private void onTick(Pre event) {
        this.packetsThisTick = 0;
        if (this.mc.player == null || this.mc.world == null) {
            return;
        }

        if (this.delayTimer > 0) {
            this.delayTimer--;
            return;
        }

        this.currentTargets = this.findTargets();
        if (this.currentTargets.isEmpty()) {
            return;
        }

        FindItemResult weaponSlot = this.findWeapon();
        boolean needsSwitch = weaponSlot.found() && !this.isWeaponInHand();
        if (needsSwitch) {
            if (this.autoSwitch.get() == AutoSwitch.None) {
                return;
            }
            InvUtils.swap(weaponSlot.slot(), this.autoSwitch.get() == AutoSwitch.Silent);
        } else if (!this.weapon.get().isEmpty() && !this.isWeaponInHand()) {
            return;
        }

        Entity primaryTarget = this.currentTargets.get(0);
        Vec3d startPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
        Vec3d targetPos = new Vec3d(primaryTarget.getX(), primaryTarget.getY(), primaryTarget.getZ());
        double currentRange = this.getCurrentRange();
        boolean attacked = false;

        if (this.auraMode.get() == AuraMode.Normal) {
            if (this.mc.player.distanceTo(primaryTarget) > currentRange) {
                if (needsSwitch && this.autoSwitch.get() == AutoSwitch.Silent) {
                    InvUtils.swapBack();
                }
                return;
            }

            for (Entity target : this.currentTargets) {
                this.doAttack(target);
                this.handleSwing();
            }
            attacked = true;
        } else {
            attacked = this.executeAuraAttack(startPos, targetPos);
        }

        if (attacked) {
            if (this.customDelay.get()) {
                this.delayTimer = this.attackDelay.get();
            } else {
                ItemStack usedWeapon = weaponSlot.found()
                        ? this.mc.player.getInventory().getStack(weaponSlot.slot())
                        : this.mc.player.getMainHandStack();

                double attackSpeed = this.getItemAttackSpeed(usedWeapon);
                this.delayTimer = (int) Math.round(20.0 / (attackSpeed > 0.0 ? attackSpeed : 4.0));
            }
        }

        if (needsSwitch && this.autoSwitch.get() == AutoSwitch.Silent) {
            InvUtils.swapBack();
        }
    }

    private double getCurrentRange() {
        return this.auraMode.get() == AuraMode.Normal ? this.normalRange.get() : this.infiniteRange.get();
    }

    private double getItemAttackSpeed(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            String id = Registries.ITEM.getId(stack.getItem()).getPath();
            if (id.contains("_sword")) {
                return 1.6;
            } else if (id.equals("wooden_axe") || id.equals("stone_axe")) {
                return 0.8;
            } else if (id.equals("iron_axe")) {
                return 0.9;
            } else if (id.equals("diamond_axe") || id.equals("netherite_axe") || id.equals("golden_axe")) {
                return 1.0;
            } else if (id.contains("_axe")) {
                return 1.0;
            } else if (id.contains("trident")) {
                return 1.1;
            } else if (id.contains("mace")) {
                return 0.6;
            } else {
                double attr = this.mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
                return attr > 0.0 ? attr : 4.0;
            }
        }
        return 4.0;
    }

    private boolean executeAuraAttack(Vec3d startPos, Vec3d targetPos) {
        if (this.core == null && this.mc.player != null && this.mc.world != null) {
            this.core = new SmartTPCore(this.mc.world, this.mc.player);
        }
        if (this.core == null) {
            return false;
        }

        this.core.updatePathfinding(startPos, targetPos, this.maxStep.get());
        List<Vec3d> path = this.core.getEfficientPath(this.maxStep.get());
        if (path != null && !path.isEmpty()) {
            this.renderPathNodes.clear();
            this.renderPathNodes.addAll(path);

            for (int i = 0; i < 2; i++) {
                this.sendPacketSafe(new OnGroundOnly(this.mc.player.isOnGround()));
            }

            for (Vec3d p : path) {
                this.sendTPPacket(p);
                this.core.desyncPos = p;
                currentDesyncPos = p;
            }

            for (Entity target : this.currentTargets) {
                this.doAttack(target);
                this.handleSwing();
            }

            List<Vec3d> reverse = new ArrayList<>(path);
            Collections.reverse(reverse);

            for (Vec3d p : reverse) {
                this.sendTPPacket(p);
                currentDesyncPos = p;
            }

            this.sendTPPacket(startPos);
            this.mc.player.updatePosition(startPos.x, startPos.y, startPos.z);
            currentDesyncPos = startPos;

            Vec3d offset = this.getOffset(startPos);
            this.sendTPPacket(offset);
            this.mc.player.updatePosition(offset.x, offset.y, offset.z);
            currentDesyncPos = offset;
            return true;
        }
        return false;
    }

    private void doAttack(Entity target) {
        if (this.mc.getNetworkHandler() != null && this.mc.player != null) {
            this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
        }
        MeteorClient.EVENT_BUS.post(AttackEntityEvent.get(target));
    }

    private void handleSwing() {
        if (this.mc.player == null) {
            return;
        }
        switch (this.swingMode.get()) {
            case None -> {
            }
            case Client -> this.mc.player.swingHand(Hand.MAIN_HAND);
            case Packet -> {
                if (this.mc.getNetworkHandler() != null) {
                    this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
            }
        }
    }

    private void sendTPPacket(Vec3d p) {
        if (p != null && this.mc.getNetworkHandler() != null && this.mc.player != null && this.mc.world != null) {
            double floorY = this.core != null ? this.core.getFloorHeightAt(p) : 0.0;
            boolean physicallyOnGround = Math.abs(p.y - (Math.floor(p.y) + floorY)) < 0.01;
            boolean collisionOnGround = !this.mc.world.isSpaceEmpty(null, new Box(p.x - 0.3, p.y - 0.05, p.z - 0.3, p.x + 0.3, p.y, p.z + 0.3));
            boolean onGround = physicallyOnGround || collisionOnGround;
            PlayerMoveC2SPacket packet = new PositionAndOnGround(p.x, p.y, p.z, onGround);
            this.sendPacketSafe(packet);
        }
    }

    private void sendPacketSafe(Packet<?> packet) {
        if (this.mc.getNetworkHandler() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long cd = 0L;
        if (this.packetsThisTick < 20 && now - this.lastPacketTime >= cd) {
            this.lastPacketTime = now;
            this.packetsThisTick++;
            this.mc.getNetworkHandler().sendPacket(packet);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.renderTarget.get() && !this.currentTargets.isEmpty()) {
            for (Entity target : this.currentTargets) {
                event.renderer.box(target.getBoundingBox(), this.targetColor.get(), this.targetColor.get(), ShapeMode.Lines, 0);
            }
        }

        if (this.renderPath.get() && !this.renderPathNodes.isEmpty()) {
            for (int i = 0; i < this.renderPathNodes.size() - 1; i++) {
                Vec3d n1 = this.renderPathNodes.get(i);
                Vec3d n2 = this.renderPathNodes.get(i + 1);
                event.renderer.line(n1.x, n1.y + 1.0, n1.z, n2.x, n2.y + 1.0, n2.z, this.pathColor.get());
                event.renderer.box(
                        new Box(n1.x - 0.2, n1.y, n1.z - 0.2, n1.x + 0.2, n1.y + 2.0, n1.z + 0.2),
                        this.pathColor.get(),
                        this.pathColor.get(),
                        ShapeMode.Lines,
                        0
                );
            }
        }
    }

    private FindItemResult findWeapon() {
        return InvUtils.findInHotbar(s -> {
            for (Item item : this.weapon.get()) {
                if (s.getItem() == item) {
                    return true;
                }
            }
            return false;
        });
    }

    private boolean isWeaponInHand() {
        if (this.mc.player == null) {
            return false;
        }
        if (this.weapon.get().isEmpty()) {
            return true;
        }
        Item held = this.mc.player.getMainHandStack().getItem();
        for (Item item : this.weapon.get()) {
            if (held == item) {
                return true;
            }
        }
        return false;
    }

    private Vec3d getOffset(Vec3d base) {
        double dx = this.horizontalOffset.get();
        double dy = this.yOffset.get();
        Vec3d[] offsets = new Vec3d[]{
                base.add(dx, dy, 0.0),
                base.add(-dx, dy, 0.0),
                base.add(0.0, dy, dx),
                base.add(0.0, dy, -dx),
                base.add(dx, dy, dx),
                base.add(-dx, dy, -dx),
                base.add(-dx, dy, dx),
                base.add(dx, dy, -dx)
        };
        List<Vec3d> list = Arrays.asList(offsets);
        Collections.shuffle(list);

        for (Vec3d p : list) {
            if (!this.isInvalid(p)) {
                return p;
            }
        }

        return base.add(0.0, dy, 0.0);
    }

    private boolean isInvalid(Vec3d pos) {
        if (this.mc.world == null || this.mc.player == null) {
            return true;
        }
        BlockPos bp = BlockPos.ofFloored(pos);
        if (this.mc.world.getWorldChunk(bp) == null) {
            return true;
        }

        Entity entity = this.mc.player.hasVehicle() ? this.mc.player.getVehicle() : this.mc.player;
        Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        Box box = entity.getBoundingBox().offset(pos.subtract(entityPos));

        for (BlockPos b : BlockPos.iterate(
                BlockPos.ofFloored(box.minX, box.minY, box.minZ),
                BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
        )) {
            BlockState state = this.mc.world.getBlockState(b);
            if (state.isOf(Blocks.LAVA) || !state.getCollisionShape(this.mc.world, b).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private List<Entity> findTargets() {
        if (this.mc.world == null || this.mc.player == null || this.mc.getNetworkHandler() == null) {
            return new ArrayList<>();
        }

        long currentTime = System.currentTimeMillis();
        this.hitbackTargets.entrySet().removeIf(ex -> currentTime - ex.getValue() > 300000L);
        List<Entity> candidates = new ArrayList<>();
        double maxRange = this.getCurrentRange();

        for (Entity e : this.mc.world.getEntities()) {
            if (e != this.mc.player
                    && e.isAlive()
                    && e instanceof LivingEntity
                    && this.entities.get().contains(e.getType())
                    && (!this.onlyHitback.get() || this.hitbackTargets.containsKey(e.getUuid()))
                    && !(e.distanceTo(this.mc.player) > maxRange)) {
                if (e instanceof PlayerEntity p) {
                    if ((this.ignoreFriends.get() && Friends.get().isFriend(p)) || !Friends.get().shouldAttack(p)) {
                        continue;
                    }

                    PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                    if ((this.ignoreBots.get() && entry == null)
                            || (this.ignoreCreative.get()
                            && entry != null
                            && (entry.getGameMode() == GameMode.CREATIVE || entry.getGameMode() == GameMode.SPECTATOR))) {
                        continue;
                    }
                }

                candidates.add(e);
            }
        }

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        switch (this.sortMode.get()) {
            case Closest -> candidates.sort(Comparator.comparingDouble(ex -> ex.distanceTo(this.mc.player)));
            case Farthest -> candidates.sort(Comparator.<Entity>comparingDouble(ex -> ex.distanceTo(this.mc.player)).reversed());
            case LowestHealth -> candidates.sort(Comparator.comparingDouble(ex -> ((LivingEntity) ex).getHealth()));
            case HighestHealth -> candidates.sort(Comparator.<Entity>comparingDouble(ex -> ((LivingEntity) ex).getHealth()).reversed());
        }

        return candidates.subList(0, Math.min(this.maxTargets.get(), candidates.size()));
    }

    private static class AStarPathFinder {
        private final World world;
        private final CollisionHelper collision;
        private static final int MAX_ITER = 15000;
        private static final long TIMEOUT_MS = 150L;

        public AStarPathFinder(World world, CollisionHelper collision) {
            this.world = world;
            this.collision = collision;
        }

        public List<Vec3d> findPath(Vec3d startVec, Vec3d endVec, double maxStep) {
            BlockPos start = BlockPos.ofFloored(startVec);
            BlockPos end = BlockPos.ofFloored(endVec);
            if (this.collision.canSweep(startVec, endVec)) {
                return new ArrayList<>(List.of(startVec, endVec));
            }

            Vec3d vclip = this.tryVClip(startVec, endVec);
            if (vclip != null) {
                Vec3d upEnd = new Vec3d(endVec.x, vclip.y, endVec.z);
                List<Vec3d> path = new ArrayList<>();
                path.add(startVec);
                path.add(vclip);
                path.add(upEnd);
                path.add(endVec);
                return path;
            }

            PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
            Map<BlockPos, Node> visited = new HashMap<>();
            Node startNode = new Node(start, null, 0.0, this.heuristic(start, end));
            open.add(startNode);
            visited.put(start, startNode);
            long startTime = System.currentTimeMillis();
            int iterations = 0;
            Node goal = null;

            while (!open.isEmpty()) {
                if (++iterations > MAX_ITER || System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                    break;
                }

                Node current = open.poll();
                if (current.pos.equals(end)) {
                    goal = current;
                    break;
                }

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx != 0 || dy != 0 || dz != 0) {
                                BlockPos nextPos = current.pos.add(dx, dy, dz);
                                if (this.collision.isPassable(nextPos)) {
                                    double moveCost = Math.sqrt(dx * dx + dy * dy + dz * dz);
                                    double newG = current.g + moveCost;
                                    Node neighbor = visited.getOrDefault(nextPos, new Node(nextPos));
                                    if (newG < neighbor.g) {
                                        neighbor.parent = current;
                                        neighbor.g = newG;
                                        neighbor.f = newG + this.heuristic(nextPos, end);
                                        if (!visited.containsKey(nextPos)) {
                                            visited.put(nextPos, neighbor);
                                            open.add(neighbor);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (goal == null) {
                return new ArrayList<>(List.of(startVec, endVec));
            }
            List<BlockPos> gridPath = this.reconstruct(goal);
            return this.smooth(gridPath, startVec, endVec);
        }

        private double heuristic(BlockPos a, BlockPos b) {
            return Math.sqrt(a.getSquaredDistance(b));
        }

        private List<BlockPos> reconstruct(Node goal) {
            List<BlockPos> path = new ArrayList<>();
            for (Node curr = goal; curr != null; curr = curr.parent) {
                path.add(curr.pos);
            }
            Collections.reverse(path);
            return path;
        }

        private List<Vec3d> smooth(List<BlockPos> grid, Vec3d start, Vec3d end) {
            List<Vec3d> result = new ArrayList<>();
            result.add(start);
            Vec3d last = start;

            for (int i = 1; i < grid.size(); i++) {
                Vec3d curr = grid.get(i).toCenterPos().add(0.0, 0.1, 0.0);
                if (!this.collision.canSweep(last, curr)) {
                    Vec3d prev = grid.get(i - 1).toCenterPos().add(0.0, 0.1, 0.0);
                    result.add(prev);
                    last = prev;
                }
            }

            result.add(end);
            return result;
        }

        private Vec3d tryVClip(Vec3d start, Vec3d end) {
            double maxY = Math.max(start.y, end.y);
            double base = maxY + 1.0;

            for (double y = base; y < base + 50.0; y++) {
                Vec3d upStart = new Vec3d(start.x, y, start.z);
                Vec3d upEnd = new Vec3d(end.x, y, end.z);
                if (this.isSpaceEmpty(upStart) && this.isSpaceEmpty(upEnd) && this.collision.canSweep(upStart, upEnd)) {
                    return upStart;
                }
            }

            return null;
        }

        private boolean isSpaceEmpty(Vec3d pos) {
            Box box = new Box(pos.x - 0.3, pos.y, pos.z - 0.3, pos.x + 0.3, pos.y + 1.8, pos.z + 0.3);
            return this.world.isSpaceEmpty(null, box) && this.world.getOtherEntities(null, box).isEmpty();
        }

        private static class Node {
            BlockPos pos;
            Node parent;
            double g = Double.MAX_VALUE;
            double f = Double.MAX_VALUE;

            Node(BlockPos pos) {
                this.pos = pos;
            }

            Node(BlockPos pos, Node parent, double g, double f) {
                this.pos = pos;
                this.parent = parent;
                this.g = g;
                this.f = f;
            }
        }
    }

    private static class CollisionHelper {
        private final World world;

        public CollisionHelper(World world) {
            this.world = world;
        }

        public boolean isPassable(BlockPos pos) {
            BlockState state = this.world.getBlockState(pos);
            return state.getCollisionShape(this.world, pos).isEmpty();
        }

        public boolean canSweep(Vec3d start, Vec3d end) {
            double dist = start.distanceTo(end);
            if (dist < 0.001) {
                return true;
            }
            int steps = (int) Math.ceil(dist / 0.05);
            Vec3d dir = end.subtract(start).multiply(1.0 / steps);
            Vec3d current = start;

            for (int i = 1; i <= steps; i++) {
                current = current.add(dir);
                if (!this.isSafe(current)) {
                    return false;
                }
            }

            return true;
        }

        public boolean isSafe(Vec3d pos) {
            return this.isSafeBox(pos.x, pos.y, pos.z);
        }

        private boolean isSafeBox(double x, double y, double z) {
            Box box = new Box(x - 0.425, y + 0.01, z - 0.425, x + 0.425, y + 1.8, z + 0.425);
            return this.world.getOtherEntities(null, box).isEmpty() && this.world.isSpaceEmpty(null, box);
        }

        public double getFloorHeight(BlockPos pos) {
            BlockState state = this.world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(this.world, pos);
            return shape.isEmpty() ? 0.0 : shape.getMax(Axis.Y);
        }
    }

    private static class SmartTPCore {
        private final CollisionHelper collisionHelper;
        private final AStarPathFinder pathFinder;
        public Vec3d desyncPos = null;
        private volatile List<Vec3d> currentPath = new ArrayList<>();

        public SmartTPCore(World world, PlayerEntity player) {
            this.collisionHelper = new CollisionHelper(world);
            this.pathFinder = new AStarPathFinder(world, this.collisionHelper);
        }

        public void updatePathfinding(Vec3d start, Vec3d target, double maxStep) {
            List<Vec3d> path = this.pathFinder.findPath(start, target, maxStep);
            this.currentPath = path != null ? path : new ArrayList<>();
        }

        public List<Vec3d> getEfficientPath(double maxStep) {
            return this.chunkPath(this.currentPath, maxStep);
        }

        private List<Vec3d> chunkPath(List<Vec3d> input, double maxStep) {
            if (input != null && !input.isEmpty()) {
                List<Vec3d> raw = new ArrayList<>(input);
                List<Vec3d> corners = new ArrayList<>();
                corners.add(raw.get(0));

                for (int i = 1; i < raw.size() - 1; i++) {
                    Vec3d prev = raw.get(i - 1);
                    Vec3d curr = raw.get(i);
                    Vec3d next = raw.get(i + 1);
                    Vec3d d1 = curr.subtract(prev).normalize();
                    Vec3d d2 = next.subtract(curr).normalize();
                    if (d1.squaredDistanceTo(d2) > 0.01) {
                        corners.add(curr);
                    }
                }

                corners.add(raw.get(raw.size() - 1));
                List<Vec3d> finalPath = new ArrayList<>();
                finalPath.add(corners.get(0));
                int ix = 0;

                while (ix < corners.size() - 1) {
                    int furthest = ix + 1;

                    for (int j = ix + 1; j < corners.size(); furthest = j++) {
                        Vec3d p1 = corners.get(ix);
                        Vec3d p2 = corners.get(j);
                        if (p1.distanceTo(p2) > maxStep - 0.05 || !this.collisionHelper.canSweep(p1, p2)) {
                            break;
                        }
                    }

                    finalPath.add(corners.get(furthest));
                    ix = furthest;
                }

                return finalPath;
            }
            return new ArrayList<>();
        }

        public double getFloorHeightAt(Vec3d pos) {
            return this.collisionHelper.getFloorHeight(BlockPos.ofFloored(pos));
        }
    }

    public enum AuraMode {
        Infinite,
        Normal
    }

    public enum AutoSwitch {
        Normal,
        Silent,
        None
    }

    public enum SortMode {
        Closest,
        Farthest,
        LowestHealth,
        HighestHealth
    }

    public enum SwingMode {
        None,
        Client,
        Packet
    }
}