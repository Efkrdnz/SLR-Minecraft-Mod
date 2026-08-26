package net.solocraft.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

import java.util.Optional;

/** Registry-tag helpers replacing the removed Forge tag-manager facade. */
public final class RegistryTagAccess {
	private RegistryTagAccess() {
	}

	public static ItemTag getTag(TagKey<Item> tag) {
		return new ItemTag(tag);
	}

	public record ItemTag(TagKey<Item> key) {
		public Optional<Item> getRandomElement(RandomSource random) {
			return BuiltInRegistries.ITEM.getTag(key)
					.flatMap(values -> values.getRandomElement(random))
					.map(holder -> holder.value());
		}
	}
}
