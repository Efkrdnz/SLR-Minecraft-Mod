package net.solocraft.procedures;

import net.solocraft.entity.BaranEntity;
import net.solocraft.entity.CerberusEntity;
import net.solocraft.entity.GoblinKingEntity;
import net.solocraft.entity.VulcanEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;

public class StealthBossDetectionHelper {
	private static final TagKey<net.minecraft.world.entity.EntityType<?>> SOLO_BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("soloboss"));

	public static boolean seesThroughStealth(Entity entity) {
		if (entity == null || entity instanceof GoblinKingEntity)
			return false;
		return entity.getType().is(SOLO_BOSS_TAG) || entity instanceof CerberusEntity || entity instanceof VulcanEntity || entity instanceof BaranEntity;
	}
}
