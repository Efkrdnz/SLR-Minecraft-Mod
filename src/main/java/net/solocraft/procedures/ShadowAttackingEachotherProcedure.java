package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class ShadowAttackingEachotherProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (ShadowMonarchManager.isShadowEntity(entity) && entity instanceof Mob mob) {
			Entity target = mob.getTarget();
			if (target != null && (ShadowMonarchManager.haveSameShadowOwner(entity, target) || target.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("portals"))))) {
				return false;
			}
		}
		return true;
	}
}
