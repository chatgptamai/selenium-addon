package dev.selenium.me.mixin;

import dev.selenium.me.event.DeathEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class NeededMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onDeathScreenInit(Text message, boolean isHardcore, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            MeteorClient.EVENT_BUS.post(DeathEvent.get(mc.player, true));
        }
    }
}