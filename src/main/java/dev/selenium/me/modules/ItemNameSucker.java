package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class ItemNameSucker extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("suckmydick");

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("text scale")
            .defaultValue(1.1)
            .min(0.5)
            .sliderMax(3.0)
            .build()
    );

    private final Setting<Double> howlongmydick = sgGeneral.add(new DoubleSetting.Builder()
            .name("y offset")
            .description("how high the text will sit above the players head")
            .defaultValue(0.65)
            .min(0.0)
            .sliderMax(2.0)
            .build()
    );

    private final Setting<Double> siksik = sgGeneral.add(new DoubleSetting.Builder()
            .name("nametag extra offset")
            .description("extra height added when Meteor Nametags module is active")
            .defaultValue(0.45)
            .min(0.0)
            .sliderMax(2.0)
            .build()
    );

    private final Setting<Boolean> onlyanvilhits = sgGeneral.add(new BoolSetting.Builder()
            .name("only custom names")
            .description("it only shows accessories that don't have names on the anvil")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> showplayerdick = sgGeneral.add(new BoolSetting.Builder()
            .name("show enchants")
            .description("shows shortened enchantments under the item name")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> txtcolor = sgGeneral.add(new ColorSetting.Builder()
            .name("text color")
            .description("text color")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> enccolor = sgGeneral.add(new ColorSetting.Builder()
            .name("enchant color")
            .description("enchantment text color")
            .defaultValue(new SettingColor(180, 180, 180, 255))
            .build()
    );

    private final Vector3d pos = new Vector3d();

    public ItemNameSucker() {
        super(SeleniumAddon.CATEGORY, "item-name", "the player sees the name of the item they are holding above their heads");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        TextRenderer text = TextRenderer.get();
        boolean nametagsActive = Modules.get().isActive(Nametags.class);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            ItemStack stack = player.getMainHandStack();
            if (stack.isEmpty()) continue;
            if (onlyanvilhits.get() && !stack.contains(DataComponentTypes.CUSTOM_NAME)) {
                continue;
            }

            String itemName = stack.getName().getString();
            String enchantLine = "";

            if (showplayerdick.get()) {
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(stack);
                if (!enchants.isEmpty()) {
                    List<String> shortList = new ArrayList<>();
                    for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                        int level = enchants.getLevel(entry);
                        String shortName = getShortEnchantName(entry);
                        shortList.add(shortName + " " + level);
                    }
                    enchantLine = String.join(" ", shortList);
                }
            }

            double extraY = nametagsActive ? siksik.get() : 0.0;
            Utils.set(pos, player, event.tickDelta);
            pos.add(0, player.getHeight() + howlongmydick.get() + extraY, 0);

            // 3D dünya koordinatını 2D ekran koordinatına çevir
            if (NametagUtils.to2D(pos, scale.get())) {
                NametagUtils.begin(pos);

                text.begin(1.0, false, true);
                double nameWidth = text.getWidth(itemName);
                double lineHeight = text.getHeight();

                double enchantScale = 0.8;
                double enchantWidth = enchantLine.isEmpty() ? 0 : text.getWidth(enchantLine) * enchantScale;
                double totalWidth = Math.max(nameWidth, enchantWidth);
                double totalHeight = enchantLine.isEmpty() ? lineHeight : lineHeight + (lineHeight * enchantScale) + 2;

                Renderer2D.COLOR.begin();
                Renderer2D.COLOR.quad(-totalWidth / 2 - 2, -totalHeight - 2, totalWidth + 4, totalHeight + 4, new SettingColor(0, 0, 0, 140));
                Renderer2D.COLOR.render(null);

                text.render(itemName, -nameWidth / 2, -totalHeight, txtcolor.get(), true);
                text.end();

                if (!enchantLine.isEmpty()) {
                    text.begin(enchantScale, false, true);
                    text.render(enchantLine, -text.getWidth(enchantLine) / 2, -totalHeight + lineHeight + 2, enccolor.get(), true);
                    text.end();
                }

                NametagUtils.end();
            }
        }
    }

    private String getShortEnchantName(RegistryEntry<Enchantment> entry) {
        String id = entry.getKey().map(key -> key.getValue().getPath()).orElse("");
        return switch (id) {
            case "sharpness" -> "sharp";
            case "smite" -> "smite";
            case "bane_of_arthropods" -> "BoA";
            case "knockback" -> "KB";
            case "fire_aspect" -> "fire";
            case "looting" -> "loot";
            case "sweeping_edge" -> "sweep";
            case "unbreaking" -> "unb";
            case "mending" -> "mend";
            case "protection" -> "prot";
            case "fire_protection" -> "fprot";
            case "blast_protection" -> "bprot";
            case "projectile_protection" -> "pprot";
            case "thorns" -> "thorns";
            case "respiration" -> "resp";
            case "aqua_affinity" -> "aqua";
            case "depth_strider" -> "depth";
            case "frost_walker" -> "frost";
            case "soul_speed" -> "soul";
            case "swift_sneak" -> "sneak";
            case "feather_falling" -> "FF";
            case "efficiency" -> "effic";
            case "silk_touch" -> "silk";
            case "fortune" -> "fort";
            case "power" -> "pow";
            case "punch" -> "punch";
            case "flame" -> "flame";
            case "infinity" -> "inf";
            case "quick_charge" -> "quick";
            case "multishot" -> "multi";
            case "piercing" -> "pierce";
            case "loyalty" -> "loyal";
            case "impaling" -> "imp";
            case "riptide" -> "rip";
            case "channeling" -> "chan";
            case "density" -> "dens";
            case "breach" -> "breach";
            case "wind_burst" -> "wind";
            case "vanishing_curse" -> "vanish";
            case "binding_curse" -> "bind";
            default -> {
                if (id.isEmpty()) yield "Ench";
                String clean = id.replace("_", "");
                yield Character.toUpperCase(clean.charAt(0)) + clean.substring(1, Math.min(4, clean.length()));
            }
        };
    }
}
