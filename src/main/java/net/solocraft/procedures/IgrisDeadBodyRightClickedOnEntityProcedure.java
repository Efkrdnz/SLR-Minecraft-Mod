package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.IgrisDeadBodyEntity;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.Comparator;

public class IgrisDeadBodyRightClickedOnEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1) {
			if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).igris == 0) {
				if ((entity instanceof IgrisDeadBodyEntity _datEntI ? _datEntI.getEntityData().get(IgrisDeadBodyEntity.DATA_arise) : 0) < 3) {
					if (entity instanceof IgrisDeadBodyEntity _datEntSetI)
						_datEntSetI.getEntityData().set(IgrisDeadBodyEntity.DATA_arise, (int) ((entity instanceof IgrisDeadBodyEntity _datEntI ? _datEntI.getEntityData().get(IgrisDeadBodyEntity.DATA_arise) : 0) + 1));
					if (sourceentity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal(("Failed! " + Math.round(4 - (entity instanceof IgrisDeadBodyEntity _datEntI ? _datEntI.getEntityData().get(IgrisDeadBodyEntity.DATA_arise) : 0)) + " tries remaining")), false);
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SMOKE, x, y, z, 6, 2, 2, 2, 1);
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_BLUE.get()), x, y, z, 6, 2, 2, 2, 1);
				} else if ((entity instanceof IgrisDeadBodyEntity _datEntI ? _datEntI.getEntityData().get(IgrisDeadBodyEntity.DATA_arise) : 0) == 3) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SMOKE, x, y, z, 12, 2, 2, 2, 1);
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_BLUE.get()), x, y, z, 12, 2, 2, 2, 1);
					if (world instanceof ServerLevel _level) {
						LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
						entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y, z)));
						entityToSpawn.setVisualOnly(true);
						_level.addFreshEntity(entityToSpawn);
					}
					{
						double _setval = 1;
						sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.igris = _setval;
							capability.syncPlayerVariables(sourceentity);
						});
					}
					{
						double _setval = 1;
						sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.IgrisSpawned = _setval;
							capability.syncPlayerVariables(sourceentity);
						});
					}
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = SololevelingModEntities.IGRIS_SHADOW.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							if (sourceentity instanceof Player _owner)
								ShadowMonarchManager.tagExistingSummon(_owner, entityToSpawn, "igris");
						}
					}
					if (!world.getEntitiesOfClass(IgrisShadowEntity.class, AABB.ofSize(new Vec3(x, y, z), 4, 4, 4), e -> true).isEmpty()) {
						if (((Entity) world.getEntitiesOfClass(IgrisShadowEntity.class, AABB.ofSize(new Vec3(x, y, z), 4, 4, 4), e -> true).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
							}
						}.compareDistOf(x, y, z)).findFirst().orElse(null)) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
							_toTame.tame(_owner);
					}
					if (!entity.level().isClientSide())
						entity.discard();
				}
			}
		}
	}
}
