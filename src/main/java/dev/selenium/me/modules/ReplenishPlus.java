package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;

public class ReplenishPlus extends Module {
   public ReplenishPlus() {
      super(SeleniumAddon.CATEGORY, "replenish-plus", "faster replenish");
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null && this.mc.interactionManager != null) {
         if (this.mc.currentScreen == null) {
            int selectedSlot = this.mc.player.getInventory().selectedSlot;
            ItemStack mainHandStack = this.mc.player.getMainHandStack();
            if (mainHandStack.getCount() <= 1) {
               int inventorySlot = this.findMatchingItem(mainHandStack);
               if (inventorySlot != -1) {
                  this.mc
                     .interactionManager
                     .clickSlot(this.mc.player.playerScreenHandler.syncId, inventorySlot, selectedSlot, SlotActionType.SWAP, this.mc.player);
               }
            }
         }
      }
   }

   private int findMatchingItem(ItemStack referenceStack) {
      if (referenceStack.isEmpty()) {
         return -1;
      } else {
         for (int i = 9; i < 36; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(referenceStack, stack)) {
               return i;
            }
         }

         return -1;
      }
   }
}
