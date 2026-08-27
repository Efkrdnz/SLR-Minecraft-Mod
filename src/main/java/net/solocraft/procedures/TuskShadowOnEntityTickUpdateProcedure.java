package net.solocraft.procedures;

import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.TuskShadowCombatManager;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Server tick bridge for Shadow Tusk ownership, ambience, and combat. */
public final class TuskShadowOnEntityTickUpdateProcedure {
	private TuskShadowOnEntityTickUpdateProcedure() {
	}

	public static void execute(LevelAccessor world, Entity entity) {
		if (!(world instanceof ServerLevel level)
				|| !(entity instanceof TuskShadowEntity tusk))
			return;
		if (ShadowMonarchManager.handleUnavailableShadowOwner(tusk))
			return;
		if (!tusk.isTame() && ShadowMonarchManager.isShadowEntity(tusk)) {
			ShadowMonarchManager.dropStoredShadowInventory(tusk);
			tusk.discard();
			return;
		}

		emitAmbientShadowParticles(level, tusk);
		TuskShadowCombatManager.tick(tusk);
	}

	private static void emitAmbientShadowParticles(ServerLevel level,
			TuskShadowEntity tusk) {
		int staggeredTick = tusk.tickCount + tusk.getId();
		if (Math.floorMod(staggeredTick, 4) != 0)
			return;
		double height = tusk.getBbHeight();
		SimpleParticleType mana = tusk.hasEffect(
				SololevelingModMobEffects.DOMAIN_BOOST)
						? (SimpleParticleType)
								SololevelingModParticleTypes.MANA_PURPLE.get()
						: (SimpleParticleType)
								SololevelingModParticleTypes.MANA_BLUE.get();
		level.sendParticles(mana, tusk.getX(), tusk.getY() + height * 0.5D,
				tusk.getZ(), 1, tusk.getBbWidth() * 0.45D,
				height * 0.45D, tusk.getBbWidth() * 0.45D, 0.025D);
		if (Math.floorMod(staggeredTick, 8) == 0)
			level.sendParticles(ParticleTypes.SMOKE, tusk.getX(),
					tusk.getY() + height * 0.5D, tusk.getZ(), 1,
					tusk.getBbWidth() * 0.55D, height * 0.4D,
					tusk.getBbWidth() * 0.55D, 0.02D);
	}
}
