package dev.selenium.me.modules;


import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;

public class SilentFrame extends Module {
   public SilentFrame() {
      super(SeleniumAddon.CATEGORY, "silent-frame", "places item frame when it broke");
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null && this.mc.interactionManager != null) {
         boolean holdingFrame = this.mc.player.getMainHandStack().getItem() == Items.ITEM_FRAME
            || this.mc.player.getMainHandStack().getItem() == Items.GLOW_ITEM_FRAME;
         if (!holdingFrame) {
            int hotbarSlot = this.findItemFrameInHotbar();
            if (hotbarSlot != -1) {
               int oldSlot = this.mc.player.getInventory().selectedSlot;
               this.mc.player.getInventory().selectedSlot = hotbarSlot;
               if (this.mc.crosshairTarget != null && this.mc.crosshairTarget.getType() == Type.BLOCK) {
                  BlockHitResult hit = (BlockHitResult)this.mc.crosshairTarget;
                  this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, hit);
                  this.mc.player.swingHand(Hand.MAIN_HAND);
               }

               this.mc.player.getInventory().selectedSlot = oldSlot;
            }
         }
      }
   }

   private int findItemFrameInHotbar() {
      for (int i = 0; i < 9; i++) {
         Item item = this.mc.player.getInventory().getStack(i).getItem();
         if (item == Items.ITEM_FRAME || item == Items.GLOW_ITEM_FRAME) {
            return i;
         }
      }

      return -1;
   }
}
