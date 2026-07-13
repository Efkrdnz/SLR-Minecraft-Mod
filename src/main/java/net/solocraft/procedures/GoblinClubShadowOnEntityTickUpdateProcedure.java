package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.entity.GoblinClubShadowEntity;
import net.solocraft.util.CombatRangeHelper;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class GoblinClubShadowOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double hei = 0;
		if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
						((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
				if (entity instanceof Mob _entity)
					_entity.getNavigation().moveTo(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), 1);
				if ((entity instanceof GoblinClubShadowEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubShadowEntity.DATA_state) : "").equals("idle")) {
					if (CombatRangeHelper.withinSurfaceRange(entity,
							(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null), 2.5D)) {
						if (entity instanceof GoblinClubShadowEntity _datEntSetS)
							_datEntSetS.getEntityData().set(GoblinClubShadowEntity.DATA_state, "attack");
					} else {
						if (entity instanceof GoblinClubShadowEntity _datEntSetS)
							_datEntSetS.getEntityData().set(GoblinClubShadowEntity.DATA_state, "idle");
					}
				}
			}
			if ((entity instanceof GoblinClubShadowEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubShadowEntity.DATA_state) : "").equals("attack")) {
				GoblinClubShadowattackProcedure.execute(world, entity);
				if (entity instanceof GoblinClubShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(GoblinClubShadowEntity.DATA_MF, (int) ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) + 1));
				if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) >= 11) {
					if (entity instanceof GoblinClubShadowEntity _datEntSetS)
						_datEntSetS.getEntityData().set(GoblinClubShadowEntity.DATA_state, "idle");
					if (entity instanceof GoblinClubShadowEntity _datEntSetI)
						_datEntSetI.getEntityData().set(GoblinClubShadowEntity.DATA_MF, 0);
				}
			}
			if ((entity instanceof GoblinClubShadowEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubShadowEntity.DATA_state) : "").equals("attack2")) {
				GoblinClubShadowattackProcedure.execute(world, entity);
				if (entity instanceof GoblinClubShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(GoblinClubShadowEntity.DATA_MF, (int) ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) + 1));
				if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) >= 21) {
					if (entity instanceof GoblinClubShadowEntity _datEntSetS)
						_datEntSetS.getEntityData().set(GoblinClubShadowEntity.DATA_state, "idle");
					if (entity instanceof GoblinClubShadowEntity _datEntSetI)
						_datEntSetI.getEntityData().set(GoblinClubShadowEntity.DATA_MF, 0);
				}
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
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_BLUE.get()), (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 2, (entity.getBbWidth() * 0.5), (hei / 2), (entity.getBbWidth() * 0.5), 0.05);
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 5, (entity.getBbWidth() * 0.75), (hei / 2), (entity.getBbWidth() * 0.75), 0.05);
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.LARGE_SMOKE, (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 2, (entity.getBbWidth() * 0.75), (hei / 2), (entity.getBbWidth() * 0.75), 0.05);
		}
	}
}
