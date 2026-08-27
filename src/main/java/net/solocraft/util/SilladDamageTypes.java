package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.SilladBossEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** Data-driven damage sources reserved for Sillad's boss mechanics. */
public final class SilladDamageTypes {
	public static final ResourceKey<DamageType> TRUE_FROST =
			ResourceKey.create(Registries.DAMAGE_TYPE,
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
							"sillad_true_frost"));

	private SilladDamageTypes() {
	}

	public static DamageSource trueFrost(Level level,
			SilladBossEntity sillad) {
		return new DamageSource(level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(TRUE_FROST), sillad, sillad);
	}

	public static boolean isTrueFrost(DamageSource source) {
		return source != null && source.is(TRUE_FROST);
	}
}
