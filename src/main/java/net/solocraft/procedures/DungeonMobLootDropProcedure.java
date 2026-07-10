package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.entity.StoneGolemEntity;
import net.solocraft.entity.SteelFangWolfEntity;
import net.solocraft.entity.SteelFangedLycanEntity;
import net.solocraft.entity.SpiderBossEntity;
import net.solocraft.entity.RedAntsEntity;
import net.solocraft.entity.MutatedEntity;
import net.solocraft.entity.KamishEntity;
import net.solocraft.entity.GoblinMageEntity;
import net.solocraft.entity.GoblinKingEntity;
import net.solocraft.entity.GoblinClubEntity;
import net.solocraft.entity.GoblinArcherEntity;
import net.solocraft.entity.GemGolemEntity;
import net.solocraft.entity.FuturisticGolemEntity;
import net.solocraft.entity.FangedKasakaEntity;
import net.solocraft.entity.BloodRedComIgrisEntity;
import net.solocraft.entity.BeruBossEntity;
import net.solocraft.entity.BarukaEntity;
import net.solocraft.entity.AncientSamuraiEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DungeonMobLootDropProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof GoblinArcherEntity) {
			if (Math.random() < (3) / ((float) 5)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_E.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			}
		} else if (entity instanceof GoblinClubEntity) {
			if (Math.random() < (3) / ((float) 5)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_E.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			}
		} else if (entity instanceof GoblinMageEntity) {
			if (Math.random() < (3) / ((float) 5)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_D.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			} else {
				if (Math.random() < (1) / ((float) 2)) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_E.get()));
						entityToSpawn.setPickUpDelay(10);
						_level.addFreshEntity(entityToSpawn);
					}
				}
			}
		} else if (entity instanceof SteelFangWolfEntity || entity instanceof SteelFangedLycanEntity) {
			if (Math.random() < (1) / ((float) 2)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_D.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			} else {
				if (Math.random() < (1) / ((float) 2)) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_D.get()));
						entityToSpawn.setPickUpDelay(10);
						_level.addFreshEntity(entityToSpawn);
					}
				}
			}
		} else if (entity instanceof AncientSamuraiEntity) {
			if (Math.random() < (3) / ((float) 5)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_A.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			}
		} else if (entity instanceof StoneGolemEntity) {
			if (Math.random() < (2) / ((float) 5)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_C.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			} else {
				if (Math.random() < (2) / ((float) 3)) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_D.get()));
						entityToSpawn.setPickUpDelay(10);
						_level.addFreshEntity(entityToSpawn);
					}
				}
			}
		} else if (entity instanceof MutatedEntity) {
			if (Math.random() < (1) / ((float) 5)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_C.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			} else {
				if (Math.random() < (1) / ((float) 5)) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_A.get()));
						entityToSpawn.setPickUpDelay(10);
						_level.addFreshEntity(entityToSpawn);
					}
				}
			}
		} else if (entity instanceof RedAntsEntity) {
			if (Math.random() < (1) / ((float) 3)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_C.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			} else {
				if (Math.random() < (1) / ((float) 2)) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_A.get()));
						entityToSpawn.setPickUpDelay(10);
						_level.addFreshEntity(entityToSpawn);
					}
				}
			}
		} else if (entity instanceof GemGolemEntity) {
			if (Math.random() < (1) / ((float) 2)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_A.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			}
		} else if (entity instanceof FangedKasakaEntity) {
			if (Math.random() < (1) / ((float) 2)) {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			} else {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_A.get()));
					entityToSpawn.setPickUpDelay(10);
					_level.addFreshEntity(entityToSpawn);
				}
			}
		} else if (entity instanceof BarukaEntity) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
		} else if (entity instanceof BeruBossEntity) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
		} else if (entity instanceof BloodRedComIgrisEntity) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
		} else if (entity instanceof FuturisticGolemEntity) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
		} else if (entity instanceof SpiderBossEntity) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_B.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_B.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
		} else if (entity instanceof GoblinKingEntity) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_A.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
		} else if (entity instanceof KamishEntity) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get()));
				entityToSpawn.setPickUpDelay(10);
				_level.addFreshEntity(entityToSpawn);
			}
		}
	}
}
