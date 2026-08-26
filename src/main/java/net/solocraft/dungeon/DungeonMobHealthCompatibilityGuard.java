package net.solocraft.dungeon;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonLevelHelper;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.entity.GoblinArcherEntity;
import net.solocraft.entity.GoblinClubEntity;
import net.solocraft.entity.GoblinMageEntity;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/**
 * Contains pathological max-health modifiers applied by third-party combat
 * stacks to Solo Leveling dungeon goblins.
 *
 * <p>Normal compatibility bonuses are intentionally untouched. Only
 * million-scale values, such as the reported 10,000,000-point addition, are
 * treated as invalid for these low-tier dungeon mobs.</p>
 */
@EventBusSubscriber
public final class DungeonMobHealthCompatibilityGuard {
	private static final String SANITIZED_TAG =
			"slr_dungeon_health_compat_sanitized";
	private static final double PATHOLOGICAL_VALUE = 1_000_000.0D;
	private static final double GOBLIN_BASE_HEALTH = 16.0D;

	private DungeonMobHealthCompatibilityGuard() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()
				|| !isEligibleDungeonGoblin(event.getEntity()))
			return;
		stabilize(event.getEntity());
		// Run once more after every join listener has returned. This also repairs
		// old saved mobs when the incompatible modifier is restored on load.
		SololevelingMod.queueServerWork(1, () -> stabilize(event.getEntity()));
	}

	public static void stabilize(Entity entity) {
		if (!(entity instanceof LivingEntity living)
				|| !isGoblin(living)
				|| !isDungeonMob(living))
			return;
		AttributeInstance maxHealth = living.getAttribute(
				Attributes.MAX_HEALTH);
		if (maxHealth == null)
			return;

		boolean changed = false;
		if (!Double.isFinite(maxHealth.getBaseValue())
				|| maxHealth.getBaseValue() >= PATHOLOGICAL_VALUE) {
			maxHealth.setBaseValue(expectedBaseHealth(living));
			changed = true;
		}
		for (AttributeModifier modifier :
				List.copyOf(maxHealth.getModifiers())) {
			if (pathological(modifier)) {
				maxHealth.removeModifier(modifier);
				changed = true;
			}
		}
		if (!Double.isFinite(maxHealth.getValue())
				|| maxHealth.getValue() >= PATHOLOGICAL_VALUE) {
			// A combination of individually smaller foreign modifiers can still
			// overflow. Preserve SLR's own level modifier and remove only foreign
			// modifiers until the result is sane.
			for (AttributeModifier modifier :
					List.copyOf(maxHealth.getModifiers())) {
				if (!SololevelingMod.MODID.equals(
						modifier.id().getNamespace())) {
					maxHealth.removeModifier(modifier);
					changed = true;
					if (Double.isFinite(maxHealth.getValue())
							&& maxHealth.getValue()
									< PATHOLOGICAL_VALUE)
						break;
				}
			}
		}
		boolean healthOverflow = !Float.isFinite(living.getHealth())
				|| living.getHealth() > living.getMaxHealth();
		if (!changed && !healthOverflow)
			return;
		float repairedHealth = Float.isFinite(living.getHealth())
				? Math.min(living.getHealth(), living.getMaxHealth())
				: living.getMaxHealth();
		living.setHealth(repairedHealth);
		if (living.getHealth() <= 0.0F && living.isAlive())
			living.setHealth(living.getMaxHealth());
		living.getPersistentData().putBoolean(SANITIZED_TAG, true);
		if (!changed)
			return;
		ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(
				living.getType());
		SololevelingMod.LOGGER.warn(
				"Removed a pathological max-health value from dungeon mob {} ({})",
				entityId, living.getUUID());
	}

	static boolean pathological(AttributeModifier modifier) {
		if (modifier == null || !Double.isFinite(modifier.amount()))
			return true;
		return switch (modifier.operation()) {
			case ADD_VALUE -> Math.abs(modifier.amount())
					>= PATHOLOGICAL_VALUE;
			case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL ->
				Math.abs(modifier.amount()) >= 10_000.0D;
		};
	}

	private static double expectedBaseHealth(LivingEntity living) {
		if (living.getPersistentData().getBoolean(
				DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG))
			return GOBLIN_BASE_HEALTH;
		double level = Math.max(0.0D,
				DungeonLevelHelper.levelOf(living));
		// Legacy procedural goblins receive +0.4 base health per level.
		// Runtime goblins keep their separate SLR level modifier above.
		return GOBLIN_BASE_HEALTH + Math.min(1_000.0D, level) * 0.4D;
	}

	private static boolean isEligibleDungeonGoblin(Entity entity) {
		return isGoblin(entity) && isDungeonMob(entity);
	}

	private static boolean isGoblin(Entity entity) {
		return entity instanceof GoblinClubEntity
				|| entity instanceof GoblinArcherEntity
				|| entity instanceof GoblinMageEntity;
	}

	private static boolean isDungeonMob(Entity entity) {
		return entity.getPersistentData().getBoolean(
					DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG)
				|| entity.getPersistentData().getBoolean(
					ProceduralDungeonCompletionHandler.PROCEDURAL_DUNGEON_TAG);
	}
}
