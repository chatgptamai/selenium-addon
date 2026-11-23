package dev.selenium.me.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

        @Inject(method = "render", at = @At("RETURN"))
    private void renderCredit(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TitleScreen screen = (TitleScreen) (Object) this;

        String credit = "Created by GPT";
        int textWidth = mc.textRenderer.getWidth(credit);

        // Position: top right with some padding
        int x = screen.width - textWidth - 1;
        int y = 56;

        // Bright red color
        int color = 0xFFFF5555;

        // Draw with shadow for visibility
        context.drawTextWithShadow(mc.textRenderer, credit, x, y, color);

        // Also draw the mod name below it
        String modName = "§6Andremoda §7v1.0.0";
        int modNameWidth = mc.textRenderer.getWidth(modName);
        context.drawTextWithShadow(mc.textRenderer, modName, screen.width - modNameWidth - 25, y + 12, 0xFFFFFFFF);
    }
}
