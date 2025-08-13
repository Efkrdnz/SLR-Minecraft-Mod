package net.solocraft.procedures;

import net.solocraft.entity.SteelFangWolfShadowEntity;
import net.solocraft.entity.KamishShadowEntity;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.BeruShadowEntity;

import net.minecraft.world.entity.Entity;

public class DomainBoostEffectExpiresProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof IgrisShadowEntity animatable)
			animatable.setTexture("igris_shadow_marcus_zero");
		if (entity instanceof SteelFangWolfShadowEntity animatable)
			animatable.setTexture("lycanshadow");
		if (entity instanceof BeruShadowEntity animatable)
			animatable.setTexture("beru_shadow");
		if (entity instanceof KamishShadowEntity animatable)
			animatable.setTexture("dragonshadow");
	}
}
