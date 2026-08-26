package net.solocraft.init;

import net.solocraft.SololevelingMod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

/**
 * Registered armor materials for the custom modeled armor sets.
 *
 * <p>Armor materials became registry-backed records in 1.21. Durability now
 * belongs to each item, so the legacy per-slot durability values remain on the
 * corresponding item properties.</p>
 */
public final class SololevelingModArmorMaterials {
	public static final DeferredRegister<ArmorMaterial> REGISTRY =
			DeferredRegister.create(Registries.ARMOR_MATERIAL, SololevelingMod.MODID);

	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOLIATH_HELMET = REGISTRY.register("goliath_helmet",
			() -> material("goliath_helmet", 3, 14, 18, 7, 0, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 1.0F));
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOLIATH_CHESTPLATE = REGISTRY.register("goliath_chestplate",
			() -> material("goliath_chestplate", 3, 14, 18, 7, 0, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 1.0F));
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOLIATH_LEGGINGS = REGISTRY.register("goliath_leggings",
			() -> material("goliath_leggings", 3, 14, 18, 7, 0, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 1.0F));
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOLIATH_BOOTS = REGISTRY.register("goliath_boots",
			() -> material("goliath_boots", 3, 14, 18, 7, 0, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 1.0F));
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SHADOW_ARMOR = REGISTRY.register("shadow_armor",
			() -> material("shadow_armor", 2, 12, 16, 6, 0, holder(SoundEvents.ENDER_DRAGON_HURT), 4.0F, 0.1F));
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SUNG_JIN_WOO_DRIP = REGISTRY.register("sung_jin_woo_drip",
			() -> material("sung_jin_woo_drip", 3, 6, 7, 3, 9, holder(SoundEvents.EMPTY), 0.5F, 0.0F));
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SUNG_JIN_WOO_DRIP_2 = REGISTRY.register("sung_jin_woo_drip_2",
			() -> material("sung_jin_woo_drip_2", 3, 6, 7, 3, 9, holder(SoundEvents.EMPTY), 0.5F, 0.0F));
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> KANG_HAIR = REGISTRY.register("kang_hair",
			() -> material("kang_hair", 2, 5, 6, 2, 9, holder(SoundEvents.EMPTY), 0.0F, 0.0F));
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CHOI_CLOAK = REGISTRY.register("choi_cloak",
			() -> material("choi_cloak", 0, 0, 10, 0, 9, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F));

	private SololevelingModArmorMaterials() {
	}

	public static Holder<ArmorMaterial> goliath(ArmorItem.Type type) {
		return switch (type) {
			case HELMET -> GOLIATH_HELMET;
			case CHESTPLATE -> GOLIATH_CHESTPLATE;
			case LEGGINGS -> GOLIATH_LEGGINGS;
			case BOOTS -> GOLIATH_BOOTS;
			case BODY -> throw new IllegalArgumentException("Goliath armor has no body-armor item");
		};
	}

	private static ArmorMaterial material(String name, int boots, int leggings, int chestplate, int helmet,
			int enchantmentValue, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance) {
		return new ArmorMaterial(
				Map.of(
						ArmorItem.Type.BOOTS, boots,
						ArmorItem.Type.LEGGINGS, leggings,
						ArmorItem.Type.CHESTPLATE, chestplate,
						ArmorItem.Type.HELMET, helmet,
						ArmorItem.Type.BODY, 0),
				enchantmentValue,
				equipSound,
				() -> Ingredient.of(),
				List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, name))),
				toughness,
				knockbackResistance);
	}

	private static Holder<SoundEvent> holder(SoundEvent sound) {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
	}
}
