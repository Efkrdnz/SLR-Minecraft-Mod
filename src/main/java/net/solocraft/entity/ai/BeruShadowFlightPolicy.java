package net.solocraft.entity.ai;

import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;

import net.neoforged.neoforge.common.Tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;

/** Decides when Shadow Beru should leave its normal grounded combat stance. */
public final class BeruShadowFlightPolicy {
	private static final TagKey<EntityType<?>> FLIGHT_TARGETS = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("sololeveling", "beru_flight_targets"));
	private static final TagKey<EntityType<?>> SOLO_BOSSES = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("minecraft", "soloboss"));

	private BeruShadowFlightPolicy() {
	}

	/**
	 * True for targets Beru must continuously meet in the air. The data tag lets
	 * packs add unusual flying mobs without adding another Java special case.
	 */
	public static boolean requiresSustainedFlight(LivingEntity target) {
		if (target == null)
			return false;
		return target.getType().is(FLIGHT_TARGETS)
				|| target instanceof FlyingMob
				|| target.isFallFlying()
				|| target.isNoGravity() && !target.onGround();
	}

	/** Bosses get brief aerial bursts instead of forcing Beru to hover forever. */
	public static boolean isBossTarget(LivingEntity target) {
		if (target == null)
			return false;
		if (target.getType().is(Tags.EntityTypes.BOSSES)
				|| target.getType().is(SOLO_BOSSES))
			return true;
		return DungeonMobLevelAdapter.MobRole.fromString(target.getPersistentData()
				.getString(DungeonMobLevelAdapter.ROLE_TAG))
				== DungeonMobLevelAdapter.MobRole.BOSS;
	}
}
