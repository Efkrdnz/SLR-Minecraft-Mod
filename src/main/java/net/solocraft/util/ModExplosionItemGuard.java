package net.solocraft.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import net.solocraft.SololevelingMod;

/**
 * Keeps this mod's own explosions from destroying dropped items.
 *
 * <p>Most of the mod's area attacks already pass
 * {@code ExplosionInteraction.NONE}, so they leave the terrain alone -- but a
 * vanilla explosion still damages every entity in range, and an
 * {@link ItemEntity} has so little health that it simply evaporates. A boss
 * using an area attack over a fight's worth of loot deleted it, which reads as
 * the mod eating your drops rather than as an explosion doing what explosions
 * do.
 *
 * <p>Scoped to explosions this mod's own entities cause. Vanilla TNT still
 * destroys items, because that is a rule players already know and rely on.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class ModExplosionItemGuard {
	private ModExplosionItemGuard() {
	}

	@SubscribeEvent
	public static void onDetonate(ExplosionEvent.Detonate event) {
		Explosion explosion = event.getExplosion();
		if (explosion == null || !isOurs(explosion.getDirectSourceEntity())
				&& !isOurs(explosion.getIndirectSourceEntity()))
			return;
		// Experience goes with it: an orb is destroyed the same way, and losing a
		// boss's experience to the boss's own attack is the same complaint.
		event.getAffectedEntities().removeIf(
				affected -> affected instanceof ItemEntity
						|| affected instanceof ExperienceOrb);
	}

	/** True for an entity this mod registered. */
	private static boolean isOurs(Entity entity) {
		if (entity == null)
			return false;
		var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return id != null && SololevelingMod.MODID.equals(id.getNamespace());
	}
}
