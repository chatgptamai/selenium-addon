package dev.selenium.me.modules;


import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

public class ItemFrameDupe extends Module {
   private final SettingGroup sgGeneral = this.settings.createGroup("dupeinbedroom");
   private final Setting<Boolean> ultraSpeed = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("ultra-speed")).description("")).defaultValue(false)).build());
   private final Setting<Integer> speed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("speed"))
                     .description(""))
                  .defaultValue(10))
               .min(1)
               .max(50)
               .visible(() -> !(Boolean)this.ultraSpeed.get()))
            .build()
      );
   private final Setting<Boolean> instantBreak = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("instant-break")).description(""))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> attackEvery = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("attack per click"))
                     .description(""))
                  .defaultValue(3))
               .min(1)
               .max(20)
               .visible(this.instantBreak::get))
            .build()
      );
   private final Setting<Boolean> onlyHold = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("only-when-holding")).description("")).defaultValue(false))
            .build()
      );
   private final Setting<Hand> hand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("hand"))
                  .description(""))
               .defaultValue(Hand.MAIN_HAND))
            .build()
      );
   private final Setting<Boolean> protectEmpty = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("protect empty")).description("")).defaultValue(true)).build());
   private int rightClickCounter = 0;

   public ItemFrameDupe() {
      super(SeleniumAddon.CATEGORY, "item-frame-dupe", "item frame dupe vroooooooo");
   }

   public void onActivate() {
      this.rightClickCounter = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null && this.mc.interactionManager != null) {
         if (!(Boolean)this.onlyHold.get() || this.mc.options.useKey.isPressed()) {
            HitResult hit = this.mc.crosshairTarget;
            if (hit != null && hit.getType() == Type.ENTITY) {
               if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof ItemFrameEntity frame) {
                  int multiplier = this.ultraSpeed.get() ? 40 : (Integer)this.speed.get();

                  for (int i = 0; i < multiplier; i++) {
                     this.mc.interactionManager.interactEntityAtLocation(this.mc.player, frame, ehr, (Hand)this.hand.get());
                     this.mc.interactionManager.interactEntity(this.mc.player, frame, (Hand)this.hand.get());
                     this.mc.player.swingHand((Hand)this.hand.get());
                     if ((Boolean)this.instantBreak.get()) {
                        this.rightClickCounter++;
                        if (this.rightClickCounter >= (Integer)this.attackEvery.get()) {
                           boolean isEmpty = frame.getHeldItemStack().isEmpty() || frame.getHeldItemStack().getItem() == Items.AIR;
                           if ((Boolean)this.protectEmpty.get() && isEmpty) {
                              this.rightClickCounter = 0;
                           } else {
                              if (frame.isAlive() && !isEmpty) {
                                 this.mc.interactionManager.attackEntity(this.mc.player, frame);
                                 this.mc.player.swingHand((Hand)this.hand.get());
                              }

                              this.rightClickCounter = 0;
                           }
                        }
                     }
                  }

                  return;
               }
            }
         }
      }
   }
}
