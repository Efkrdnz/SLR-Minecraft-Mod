package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class IgrisShadowOnEntityTickUpdateProcedure {
	private static final String NAV_TARGET_TAG = "sl_igris_nav_target";
	private static final String NEXT_REPATH_TAG = "sl_igris_next_repath";
	private static final int REPATH_INTERVAL_TICKS = 25;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		double hei = 0;
		double rand2 = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 0) {
			String combatState = entity.getPersistentData().getString("state");
			if (!(combatState.equals("idle") || combatState.equals("spin") || combatState.equals("stab") || combatState.equals("slam"))) {
				entity.getPersistentData().putString("state", "idle");
				entity.getPersistentData().putDouble("MF", 0);
			}
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				entity.getPersistentData().putBoolean("sprint", true);
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
						((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
				if (!entity.level().isClientSide() && entity instanceof Mob mob && mob.getTarget() != null) {
					LivingEntity target = mob.getTarget();
					CompoundTag data = entity.getPersistentData();
					long now = entity.level().getGameTime();
					boolean targetChanged = !data.hasUUID(NAV_TARGET_TAG)
							|| !data.getUUID(NAV_TARGET_TAG).equals(target.getUUID());
					if (targetChanged || mob.getNavigation().isDone()
							&& now >= data.getLong(NEXT_REPATH_TAG)) {
						mob.getNavigation().moveTo(target, 1.0D);
						data.putUUID(NAV_TARGET_TAG, target.getUUID());
						data.putLong(NEXT_REPATH_TAG, now + REPATH_INTERVAL_TICKS);
					}
				}
				entity.getPersistentData().putDouble("MF", (entity.getPersistentData().getDouble("MF") + 1));
			} else {
				entity.getPersistentData().remove(NAV_TARGET_TAG);
				entity.getPersistentData().remove(NEXT_REPATH_TAG);
				entity.getPersistentData().putDouble("MF", 0);
				entity.getPersistentData().putString("state", "idle");
			}
			if ((entity.getPersistentData().getString("state")).equals("idle")) {
				if (entity.getPersistentData().getDouble("MF") >= 10) {
					ShadowIgrisStateChangerProcedure.execute(entity);
				}
			}
			if ((entity.getPersistentData().getString("state")).equals("spin")) {
				ShadowIgrisSpinProcedure.execute(world, x, y, z, entity);
			}
			if ((entity.getPersistentData().getString("state")).equals("stab")) {
				ShadowIgrisStabProcedure.execute(world, x, y, z, entity);
			}
			if ((entity.getPersistentData().getString("state")).equals("slam")) {
				ShadowIgrisSlamProcedure.execute(world, x, y, z, entity);
			}
		} else {
			entity.getPersistentData().putDouble("MF", 0);
		}
		if ((entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
			if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).isAlive())) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 30, 0.05, 0.05, 0.05, 1);
				if (!entity.level().isClientSide()) {
					ShadowMonarchManager.dropStoredShadowInventory(entity);
					entity.discard();
				}
			}
		}
		if (!(entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
			if (!entity.level().isClientSide()) {
				ShadowMonarchManager.dropStoredShadowInventory(entity);
				entity.discard();
			}
		}
		hei = entity.getBbHeight();
		if (entity instanceof LivingEntity _livEnt41 && _livEnt41.hasEffect(SololevelingModMobEffects.DOMAIN_BOOST.get())) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_PURPLE.get()), (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 2, (entity.getBbWidth() * 0.5), (hei / 2), (entity.getBbWidth() * 0.5), 0.05);
		} else {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_BLUE.get()), (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 2, (entity.getBbWidth() * 0.5), (hei / 2), (entity.getBbWidth() * 0.5), 0.05);
		}
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 5, (entity.getBbWidth() * 0.75), (hei / 2), (entity.getBbWidth() * 0.75), 0.05);
	}
}
