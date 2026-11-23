package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class antimacetest123 extends Module {
    private final SettingGroup mace = settings.createGroup("maceisbroken");
    private final SettingGroup thh = settings.createGroup("thh");

    private final Setting<Double> horizontalRange = mace.add(
            new DoubleSetting.Builder()
                    .name("horizontal range")
                    .defaultValue(3.5)
                    .min(0)
                    .max(5)
                    .sliderMax(5)
                    .build()
    );

    private final Setting<Double> verticalRange = mace.add(
            new DoubleSetting.Builder()
                    .name("vertical-range")
                    .defaultValue(2.5)
                    .min(0)
                    .max(5)
                    .sliderMax(5)
                    .build()
    );
    private final Setting<Boolean> onlyWhenHoldingMace = thh.add(
            new BoolSetting.Builder()
                    .name("only when holding mace")
                    .defaultValue(true)
                    .build()
    );

    private final Setting<Integer> normalDelay = thh.add(
            new IntSetting.Builder()
                    .name("normal delay")
                    .defaultValue(3)
                    .min(1)
                    .max(20)
                    .visible(() -> !onlyWhenHoldingMace.get())
                    .build()
    );

    private final Setting<Boolean> checkCollision = thh.add(
            new BoolSetting.Builder()
                    .name("check collision")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Double> detectionRange = thh.add(
            new DoubleSetting.Builder()
                    .name("detection range")
                    .defaultValue(12)
                    .min(3)
                    .max(32)
                    .sliderMax(24)
                    .build()
    );

    private final Setting<Integer> threatDelay = thh.add(
            new IntSetting.Builder()
                    .name("threat delay")
                    .defaultValue(1)
                    .min(1)
                    .max(10)
                    .build()
    );



    private final Random random = new Random();
    private int timer = 0;
    private boolean threatMode = false;

    public antimacetest123() {
        super(SeleniumAddon.CATEGORY, "anti-mace", "fuck ur opponents");
    }

    @Override
    public void onActivate() {
        timer = 0;
        threatMode = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        boolean wasThreat = threatMode;
        threatMode = false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isSpectator() || player.isInvisible()) continue;

            double distance = mc.player.distanceTo(player);
            if (distance > detectionRange.get()) continue;

            boolean hasMace = player.getMainHandStack().isOf(Items.MACE) ||
                    player.getOffHandStack().isOf(Items.MACE);

            if (onlyWhenHoldingMace.get()) {
                if (hasMace) {
                    threatMode = true;
                    break;
                }
            } else {
                threatMode = true;
                break;
            }
        }

        // Sadece Mace modunda tehdit yoksa bekle
        if (onlyWhenHoldingMace.get() && !threatMode) {
            timer = 0;
            return;
        }

        if (threatMode && !wasThreat) {
            timer = threatDelay.get();
        }

        int currentDelay = threatMode ? threatDelay.get() : normalDelay.get();

        if (++timer < currentDelay) return;
        timer = 0;

        // --- Güvenli Rastgele Pozisyon Bulma ---
        Vec3d current = mc.player.getPos();
        Vec3d safeTarget = findSafePosition(current);

        // Eğer 10 denemede boş bir yer bulunamadıysa duvara girmemek için iptal et
        if (safeTarget == null) return;

        // Client pozisyonunu güncelle
        mc.player.setPos(safeTarget.x, safeTarget.y, safeTarget.z);

        // Sunucuya paket gönder
        mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(
                        safeTarget.x,
                        safeTarget.y,
                        safeTarget.z,
                        mc.player.isOnGround()
                )
        );

        if (threatMode) {
            info("§cThreat Mode §7| Offset: %.2f %.2f %.2f",
                    safeTarget.x - current.x,
                    safeTarget.y - current.y,
                    safeTarget.z - current.z);
        }
    }

    private Vec3d findSafePosition(Vec3d current) {
        int maxAttempts = checkCollision.get() ? 10 : 1;

        for (int i = 0; i < maxAttempts; i++) {
            double x = (random.nextDouble() * 2 - 1) * horizontalRange.get();
            double y = (random.nextDouble() * 2 - 1) * verticalRange.get();
            double z = (random.nextDouble() * 2 - 1) * horizontalRange.get();

            // Çarpışma kontrolü kapalıysa ilk üretilen noktayı döndür
            if (!checkCollision.get()) {
                return new Vec3d(current.x + x, current.y + y, current.z + z);
            }

            // Oyuncunun mevcut hitbox'ını (çarpışma kutusunu) hedef koordinata kaydır
            Box targetBox = mc.player.getBoundingBox().offset(x, y, z);

            // Hedef kutunun içinde katı blok yoksa bu nokta güvenlidir
            if (mc.world.isSpaceEmpty(mc.player, targetBox)) {
                return new Vec3d(current.x + x, current.y + y, current.z + z);
            }
        }

        // Tüm denemeler dolu bloklara denk geldiyse null döndür
        return null;
    }

    @Override
    public String getInfoString() {
        if (threatMode) return "Threat";
        return onlyWhenHoldingMace.get() ? "Idle" : "Normal";
    }
}
