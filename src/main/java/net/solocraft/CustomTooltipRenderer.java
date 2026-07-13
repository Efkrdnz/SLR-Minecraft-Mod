package net.solocraft;

import net.solocraft.client.gui.WeaponTooltipProfiles;
import net.solocraft.client.gui.WeaponTooltipRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

import java.awt.Color;
import java.util.Locale;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "sololeveling", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CustomTooltipRenderer {

    private static final String MODID = "sololeveling";
    private static final TagKey<Item> WEAPONS_TAG =
            TagKey.create(Registries.ITEM, new ResourceLocation(MODID, "weapons"));

    private static final Set<String> WEAPON_NAME_KEYS = Set.of(
            "sword", "long_sword", "longsword", "dagger", "katana",
            "axe", "war_axe", "hammer", "spear", "bow",
            "griamore", "grimoire", "gun", "wrath", "killer"
    );

    // ===== Solo Leveling aesthetic controls =====
    private static final boolean ANIMATE = true;
    private static final float PULSE_SPEED = 0.00055f;
    private static final float PULSE_MIN   = 0.96f;
    private static final float PULSE_MAX   = 1.00f;

    // goofy ahh palettes (HSB hues)
    private static final float H_CYAN    = 0.54f; // solo ui
    private static final float H_BLUE    = 0.60f; // system window
    private static final float H_INDIGO  = 0.72f; // Shadow Monarch body
    private static final float H_PURPLE  = 0.78f; // SSS
    private static final float H_RED     = 0.03f; // SS
    private static final float H_GOLD    = 0.11f; // S
    private static final float H_GREEN   = 0.33f; // AB
    private static final float H_ICE     = 0.58f; // CD

    private static final float SAT_LOW   = 0.20f; // background
    private static final float SAT_BORDER= 0.35f; // border accent
    private static final float V_DARK1   = 0.22f;
    private static final float V_DARK2   = 0.16f;
    private static final float V_BORDER  = 0.34f;

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        WeaponTooltipProfiles.Profile profile = WeaponTooltipProfiles.find(event.getItemStack());
        if (profile != null)
            WeaponTooltipRenderer.render(event, profile);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !isModItem(stack)) return;

        if (WeaponTooltipProfiles.find(stack) != null) {
            event.setBackground(0x00000000);
            event.setBorderStart(0x00000000);
            event.setBorderEnd(0x00000000);
            return;
        }

        String name = getPath(stack.getItem()).toLowerCase(Locale.ROOT);
        if (name.equals("redkey")) {
            themeSoloDualBorder(event, 0.78f, SAT_LOW * 1.25f, 0.17f, 0.08f, 0.97f, SAT_BORDER * 1.35f, 0.38f, H_GOLD, SAT_BORDER, 0.34f);
            return;
        }
        if (!isModWeapon(stack)) return;
        Rarity rarity = stack.getRarity();

        // --- Solo Leveling themed routing ---
        if (name.contains("shadow") || name.contains("monarch") || name.contains("igris") || name.contains("beru")) {
            // Shadow Monarch / shadow soldiers: indigo slab with cyan line glow
            themeSolo(event, H_INDIGO, SAT_LOW, V_DARK1, V_DARK2, H_CYAN, SAT_BORDER, V_BORDER);
        } else if (name.contains("kamish")) {
            // Kamish (dragon): deep ember with restrained gold edge
            themeSolo(event, H_RED, SAT_LOW * 1.15f, 0.24f, 0.18f, H_GOLD, SAT_BORDER * 0.9f, 0.32f);
        } else if (name.contains("frost") || name.contains("ice")) {
            // Ice: steel-blue slab with cyan edge
            themeSolo(event, H_ICE, SAT_LOW * 0.9f, 0.24f, 0.18f, H_CYAN, SAT_BORDER, V_BORDER);
        } else if (name.contains("emerald")) {
            // Nature/poison: muted green with pale cyan border
            themeSolo(event, H_GREEN, SAT_LOW * 1.05f, V_DARK1, V_DARK2, 0.40f, SAT_BORDER * 0.8f, 0.30f);
        } else if (name.contains("demon")) {
            // Demon King: abyssal with dark crimson accent
            themeSolo(event, 0.97f, SAT_LOW * 1.1f, 0.20f, 0.14f, H_RED, SAT_BORDER * 0.9f, 0.30f);
        } else if (name.contains("spirit_bow") || name.contains("bow") || name.contains("mana_gun")
                || name.contains("griamore") || name.contains("grimoire")) {
            // Ranged/magic: void-blue body, cyan border (system-like)
            themeSolo(event, H_BLUE, SAT_LOW, V_DARK1, V_DARK2, H_CYAN, SAT_BORDER, V_BORDER);
        } else if (name.contains("axe") || name.contains("hammer")) {
            // Heavy hitters: obsidian slab, subtle blue border
            themeSolo(event, 0.64f, SAT_LOW * 0.9f, 0.20f, 0.14f, H_BLUE, SAT_BORDER * 0.8f, 0.30f);
        } else if (name.contains("mythic") || name.contains("s_tier") || name.contains("stier")
                || name.contains("knight_killer") || name.contains("igrislongsword")) {
            // Prestige/legendary: royal purple slab, cyan line glow (anime “UI + power”)
            themeSolo(event, H_PURPLE, SAT_LOW, V_DARK1, V_DARK2, H_CYAN, SAT_BORDER, V_BORDER);
        } else if (rarity == Rarity.EPIC) {
            themeSolo(event, H_PURPLE, SAT_LOW, V_DARK1, V_DARK2, H_CYAN, SAT_BORDER, V_BORDER);
        } else if (rarity == Rarity.RARE) {
            themeSolo(event, H_BLUE, SAT_LOW, V_DARK1, V_DARK2, H_CYAN, SAT_BORDER, V_BORDER);
        } else if (name.contains("spear") || name.contains("katana") || name.contains("dagger") || name.contains("sword")) {
            // Default hunter weapon look: Solo system window
            themeSolo(event, H_BLUE, SAT_LOW, V_DARK1, V_DARK2, H_CYAN, SAT_BORDER, V_BORDER);
        } else {
            // Catch-all: subdued obsidian UI
            themeSolo(event, 0.64f, SAT_LOW * 0.9f, 0.20f, 0.14f, H_BLUE, SAT_BORDER * 0.8f, 0.30f);
        }
    }

    // --- classify only your mod's weapons ---
    private static boolean isModItem(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && MODID.equals(key.getNamespace());
    }

    private static boolean isModWeapon(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem) return false;
        if (item instanceof ArmorItem) return false;
        if (item instanceof ForgeSpawnEggItem) return false;

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null || !MODID.equals(key.getNamespace())) return false;

        if (stack.is(WEAPONS_TAG)) return true;

        if (item instanceof SwordItem || item instanceof AxeItem
                || item instanceof BowItem || item instanceof CrossbowItem
                || item instanceof TridentItem) {
            return true;
        }

        String path = key.getPath().toLowerCase(Locale.ROOT);
        for (String s : WEAPON_NAME_KEYS) if (path.contains(s)) return true;
        return false;
    }

    private static String getPath(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? "" : key.getPath();
    }

    // --- Theming core: “system window” slab + thin cyan-like border ---
    private static void themeSolo(RenderTooltipEvent.Color event,
                                  float bgHue, float bgSat, float vStart, float vEnd,
                                  float borderHue, float borderSat, float borderV) {
        float pulse = ANIMATE ? easePulse(PULSE_MIN, PULSE_MAX, PULSE_SPEED) : 1.0f;

        int bgStart = withAlpha(Color.HSBtoRGB(bgHue, clamp01(bgSat), clamp01(vStart * pulse)));
        int bgEnd   = withAlpha(Color.HSBtoRGB(bgHue, clamp01(bgSat * 0.9f), clamp01(vEnd)));

        // Border gets a mild pulse to mimic “UI glow”
        int border = withAlpha(Color.HSBtoRGB(borderHue, clamp01(borderSat), clamp01(borderV * pulse)));

        event.setBackgroundStart(bgStart);
        event.setBackgroundEnd(bgEnd);
        event.setBorderStart(border);
        event.setBorderEnd(border);
    }

    private static void themeSoloDualBorder(RenderTooltipEvent.Color event,
                                            float bgHue, float bgSat, float vStart, float vEnd,
                                            float borderStartHue, float borderStartSat, float borderStartV,
                                            float borderEndHue, float borderEndSat, float borderEndV) {
        float pulse = ANIMATE ? easePulse(PULSE_MIN, PULSE_MAX, PULSE_SPEED * 1.4f) : 1.0f;

        int bgStart = withAlpha(Color.HSBtoRGB(bgHue, clamp01(bgSat), clamp01(vStart * pulse)));
        int bgEnd = withAlpha(Color.HSBtoRGB(bgHue + 0.04f, clamp01(bgSat * 1.15f), clamp01(vEnd)));
        int borderStart = withAlpha(Color.HSBtoRGB(borderStartHue, clamp01(borderStartSat), clamp01(borderStartV * pulse)));
        int borderEnd = withAlpha(Color.HSBtoRGB(borderEndHue, clamp01(borderEndSat), clamp01(borderEndV * pulse)));

        event.setBackgroundStart(bgStart);
        event.setBackgroundEnd(bgEnd);
        event.setBorderStart(borderStart);
        event.setBorderEnd(borderEnd);
    }

    // --- helpers ---
    private static float easePulse(float min, float max, float speed) {
        double s = Math.sin(System.currentTimeMillis() * speed);
        float t = (float)((s + 1.0) * 0.5); // 0..1
        return min + (max - min) * t;
    }
    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(1f, v); }
    private static int withAlpha(int rgb) { return rgb | 0xFF000000; }
}
