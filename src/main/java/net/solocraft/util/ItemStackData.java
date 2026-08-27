package net.solocraft.util;

import com.mojang.serialization.Dynamic;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.function.Consumer;

/**
 * Accessors for Solo Leveling's arbitrary per-stack data on the component-based
 * item format introduced after 1.20.1.
 *
 * <p>The loader also recognizes raw 1.20.1 ItemStack compounds embedded inside
 * mod-owned entity/player data. Vanilla cannot automatically data-fix those
 * nested compounds because they are outside a vanilla inventory schema.</p>
 */
public final class ItemStackData {
	private static final int MINECRAFT_1_20_1_DATA_VERSION = 3465;

	private ItemStackData() {
	}

	/** Returns a defensive copy; mutate stacks through {@link #update}. */
	public static CompoundTag copy(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return new CompoundTag();
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	public static void update(ItemStack stack, Consumer<CompoundTag> updater) {
		if (stack != null && !stack.isEmpty())
			CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
	}

	public static String getString(ItemStack stack, String key) {
		return copy(stack).getString(key);
	}

	public static void putString(ItemStack stack, String key, String value) {
		update(stack, tag -> tag.putString(key, value));
	}

	public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
		if (stack == null || stack.isEmpty())
			return new CompoundTag();
		return (CompoundTag) stack.saveOptional(registries);
	}

	public static ItemStack load(CompoundTag serialized, HolderLookup.Provider registries) {
		if (serialized == null || serialized.isEmpty())
			return ItemStack.EMPTY;
		CompoundTag migrated = migrateLegacyStack(serialized);
		return ItemStack.parseOptional(registries, migrated);
	}

	public static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
		if (stack == null || stack.isEmpty())
			return 0;
		for (var entry : stack.getEnchantments().entrySet()) {
			if (entry.getKey().is(enchantment))
				return entry.getIntValue();
		}
		return 0;
	}

	public static void enchant(ItemStack stack, ResourceKey<Enchantment> enchantment,
			int level, HolderLookup.Provider registries) {
		Holder<Enchantment> holder = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
		stack.enchant(holder, level);
	}

	/** The legacy undefined-creature calculation only allowed Sharpness to contribute. */
	public static double legacyUndefinedDamageBonus(ItemStack stack) {
		int sharpness = enchantmentLevel(stack, Enchantments.SHARPNESS);
		return sharpness <= 0 ? 0.0D : 0.5D * sharpness + 0.5D;
	}

	/** Reproduces the generated 1.20.1 modifier calculation used by dagger mana/cooldown rules. */
	public static double computedModifierValue(ItemStack stack, Holder<Attribute> target, EquipmentSlot slot) {
		double[] values = {0.0D, 1.0D, 1.0D};
		stack.forEachModifier(slot, (attribute, modifier) -> {
			if (!attribute.equals(target))
				return;
			switch (modifier.operation()) {
				case ADD_VALUE -> values[0] += modifier.amount();
				case ADD_MULTIPLIED_BASE -> values[1] += modifier.amount();
				case ADD_MULTIPLIED_TOTAL -> values[2] *= 1.0D + modifier.amount();
			}
		});
		return values[0] * values[1] * values[2];
	}

	public static double totalModifierAmount(ItemStack stack, Holder<Attribute> target, EquipmentSlot slot) {
		double[] total = {0.0D};
		stack.forEachModifier(slot, (attribute, modifier) -> {
			if (attribute.equals(target))
				total[0] += modifier.amount();
		});
		return total[0];
	}

	private static CompoundTag migrateLegacyStack(CompoundTag serialized) {
		// 1.20.1 used Count/tag; 1.21.1 uses count/components. Never re-fix data
		// already written by this port.
		if (!serialized.contains("Count") && !serialized.contains("tag"))
			return serialized;
		try {
			Dynamic<Tag> fixed = DataFixers.getDataFixer().update(
					References.ITEM_STACK,
					new Dynamic<>(NbtOps.INSTANCE, serialized),
					MINECRAFT_1_20_1_DATA_VERSION,
					SharedConstants.getCurrentVersion().getDataVersion().getVersion());
			return fixed.getValue() instanceof CompoundTag compound ? compound : serialized;
		} catch (RuntimeException ignored) {
			// Parsing below remains safe and yields ItemStack.EMPTY for malformed data.
			return serialized;
		}
	}
}
