package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Comparator;

@Mod.EventBusSubscriber
public class LightningStormTickProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player.getX(), event.player.getY(), event.player.getZ(), event.player);
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world.dayTime() % 20 == 0) {
			if (!world.isClientSide()) {
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).baranlightningstrike > 0) {
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).baranlightningstrike - 1;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.baranlightningstrike = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 5, 10); index0++) {
						entity.getPersistentData().putBoolean("foundRand", false);
						while (!entity.getPersistentData().getBoolean("foundRand")) {
							entity.getPersistentData().putDouble("rx", (entity.getX() + Mth.nextInt(RandomSource.create(), -25, 25)));
							entity.getPersistentData().putDouble("ry", (entity.getY() + Mth.nextInt(RandomSource.create(), -25, 25)));
							entity.getPersistentData().putDouble("rz", (entity.getZ() + Mth.nextInt(RandomSource.create(), -25, 25)));
							if ((world.getBlockState(BlockPos.containing(entity.getPersistentData().getDouble("rx"), entity.getPersistentData().getDouble("ry"), entity.getPersistentData().getDouble("rz")))).getBlock() == Blocks.AIR
									&& world.getBlockFloorHeight(BlockPos.containing(entity.getPersistentData().getDouble("rx"), entity.getPersistentData().getDouble("ry") - 1, entity.getPersistentData().getDouble("rz"))) > 0) {
								entity.getPersistentData().putBoolean("foundRand", true);
								if (world instanceof ServerLevel _level) {
									LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
									entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(entity.getPersistentData().getDouble("rx"), entity.getPersistentData().getDouble("ry"), entity.getPersistentData().getDouble("rz"))));
									entityToSpawn.setVisualOnly(true);
									_level.addFreshEntity(entityToSpawn);
								}
								{
									final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("rx")), (entity.getPersistentData().getDouble("ry")), (entity.getPersistentData().getDouble("rz")));
									List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
											.toList();
									for (Entity entityiterator : _entfound) {
										if (!(entity == entityiterator) && entityiterator instanceof LivingEntity) {
										}
									}
								}
							} else {
								continue;
							}
						}
					}
					if (world.dayTime() % 40 == 0) {
						{
							final Vec3 _center = new Vec3(x, y, z);
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(15 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
							for (Entity entityiterator : _entfound) {
								if (!(entity == entityiterator) && entityiterator instanceof LivingEntity) {
									entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), entity), 5);
									if (world instanceof ServerLevel _level) {
										LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
										entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ())));
										entityToSpawn.setVisualOnly(true);
										_level.addFreshEntity(entityToSpawn);
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
