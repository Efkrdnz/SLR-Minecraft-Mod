package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.KangTaeshikEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class AfterImagesStealthProcedure {
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
		double rand1 = 0;
		double rand2 = 0;
		double rand3 = 0;
		double rand4 = 0;
		if (CooldownManager.isOnCooldown(entity, "Stealth") && entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(MobEffects.INVISIBILITY)
				&& CooldownManager.getRemainingTicks(entity, "Stealth") % 5 == 0) {
			if (Math.random() < 0.125) {
				rand2 = -2;
				rand3 = -2;
			} else if (Math.random() < 0.25) {
				rand2 = -2;
				rand3 = 0;
			} else if (Math.random() < 0.325) {
				rand2 = -2;
				rand3 = 2;
			} else if (Math.random() < 0.5) {
				rand2 = 2;
				rand3 = -2;
			} else if (Math.random() < 0.625) {
				rand2 = 2;
				rand3 = 0;
			} else if (Math.random() < 0.75) {
				rand2 = 2;
				rand3 = 2;
			} else if (Math.random() < 0.875) {
				rand2 = 0;
				rand3 = 2;
			} else if (Math.random() < 1) {
				rand2 = 0;
				rand3 = -2;
			}
			if (entity instanceof KangTaeshikEntity || entity.isSprinting()) {
				rand4 = Mth.nextInt(RandomSource.create(), 1, 3);
				if (rand4 == 1) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = SololevelingModEntities.AFTER_IMAGE.get().spawn(_level, BlockPos.containing(x + rand2, y + 0.3, z + rand3), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setYRot(world.getRandom().nextFloat() * 360F);
						}
					}
				} else if (rand4 == 2) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = SololevelingModEntities.AFTER_IMAGE_1.get().spawn(_level, BlockPos.containing(x + rand2, y + 0.3, z + rand3), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setYRot(world.getRandom().nextFloat() * 360F);
						}
					}
				} else if (rand4 == 3) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = SololevelingModEntities.AFTER_IMAGE_2.get().spawn(_level, BlockPos.containing(x + rand2, y + 0.3, z + rand3), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setYRot(world.getRandom().nextFloat() * 360F);
						}
					}
				}
			}
		}
	}
}
