package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoLink extends Module {
    private boolean clicked = false;
    private boolean waitingScreen = false;

    public AutoLink() {
        super(SeleniumAddon.CATEGORY, "autolink", "automatically links nearest enderchest");
    }

    @Override
    public void onActivate() {
        eclink ecLinkModulesss = Modules.get().get(eclink.class);
        if (ecLinkModulesss != null && ecLinkModulesss.isActive() == false) {
            ecLinkModulesss.toggle();
        }
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        clicked = false;
        waitingScreen = false;
    }

    private boolean isEcLinked() {
        eclink ecLinkModule = Modules.get().get(eclink.class);
        return ecLinkModule != null && ecLinkModule.isActive() && ecLinkModule.isLinked();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }

        if (isEcLinked()) {
            reset();
            return;
        }

        if (clicked) return;

        for (BlockPos pos : BlockPos.iterateOutwards(mc.player.getBlockPos(), 5, 5, 5)) {
            if (mc.world.getBlockState(pos).isOf(Blocks.ENDER_CHEST)) {
                BlockPos target = pos.toImmutable();
                BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(target), Direction.UP, target, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                clicked = true;
                waitingScreen = true;
                break;
            }
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!waitingScreen || isEcLinked()) return;

        if (event.screen instanceof GenericContainerScreen) {
            waitingScreen = false;
            mc.setScreen(null);
            event.cancel();
        }
    }
}