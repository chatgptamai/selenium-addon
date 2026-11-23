package dev.selenium.me.modules;


import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;

public class AutoMace extends Module {
    private static final int MAX_HITS = 5000;
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgHits = this.settings.createGroup("Hits");
    private final Setting<Integer> hitAmount = this.sgGeneral
            .add(((Builder)((Builder)new Builder().name("hit-amount")).defaultValue(3)).min(1).max(5000).sliderMin(1).sliderMax(50).build());
    private final Setting<Integer> packets = this.sgGeneral
            .add(((Builder)((Builder)new Builder().name("spoof-packets")).defaultValue(4)).min(1).sliderMax(10).build());
    private final Setting<AutoMace.SwitchMode> switchMode = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                            .name("auto-switch"))
                            .defaultValue(AutoMace.SwitchMode.Silent))
                            .build()
            );
    private final Setting<Integer>[] hits = new Setting[5000];

    public AutoMace() {
        super(SeleniumAddon.CATEGORY, "AutoMace", "Makes mace powerful.");

        for (int i = 0; i < 5000; i++) {
            int index = i;
            int defaultHeight = 100 + i * 20;
            this.hits[i] = this.sgHits
                    .add(
                            ((Builder)((Builder)((Builder)new Builder().name("hit-" + (i + 1))).defaultValue(defaultHeight))
                                    .min(1)
                                    .sliderMax(1000)
                                    .visible(() -> (Integer)this.hitAmount.get() > index))
                                    .build()
                    );
        }
    }

    @EventHandler
    private void onAttackEntity(AttackEntityEvent event) {
        if (this.mc.player != null && this.mc.world != null) {
            FindItemResult mace = InvUtils.findInHotbar(s -> s.getItem().toString().toLowerCase().contains("mace"));
            if (mace.found()) {
                boolean hasMaceInHand = this.mc.player.getMainHandStack().getItem().toString().toLowerCase().contains("mace");
                if (!hasMaceInHand && this.switchMode.get() != AutoMace.SwitchMode.None) {
                    InvUtils.swap(mace.slot(), this.switchMode.get() == AutoMace.SwitchMode.Silent);
                }

                this.hitEntity(event.entity);
                if (!hasMaceInHand && this.switchMode.get() == AutoMace.SwitchMode.Silent) {
                    InvUtils.swapBack();
                }
            }
        }
    }

    public void hitEntity(Entity target) {
        if (this.mc.player != null && this.mc.world != null) {
            int currentHits = Math.min((Integer)this.hitAmount.get(), 5000);

            for (int i = 0; i < currentHits; i++) {
                int height = (Integer)this.hits[i].get();
                this.spoofTp(height, target);
                this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
                this.mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private void spoofTp(int h, Entity target) {
        double x = this.mc.player.getX();
        double y = this.mc.player.getY();
        double z = this.mc.player.getZ();

        if (target != null) {
            x = target.getX();
            z = target.getZ();
        }

        for (int i = 0; i < this.packets.get(); i++) {
            this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(false));
        }

        PositionAndOnGround up =
                new PositionAndOnGround(x, y + (double) h, z, false);

        PositionAndOnGround down =
                new PositionAndOnGround(x, y, z, false);

        ((IPlayerMoveC2SPacket) up).setTag(1337);
        ((IPlayerMoveC2SPacket) down).setTag(1337);

        this.mc.getNetworkHandler().sendPacket(up);
        this.mc.getNetworkHandler().sendPacket(down);
    }
    public static enum SwitchMode {
        None,
        Normal,
        Silent;
    }
}
