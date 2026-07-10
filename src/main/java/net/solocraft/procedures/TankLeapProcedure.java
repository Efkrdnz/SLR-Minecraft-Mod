package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.SololevelingMod;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.solocraft.util.CooldownManager;

public class TankLeapProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).leapjump == false) {
			CooldownManager.set(entity, "Tank Leap", 240);
			entity.setDeltaMovement(new Vec3((entity.getDeltaMovement().x() + entity.getLookAngle().x * 2), 1, (entity.getDeltaMovement().z() + entity.getLookAngle().z * 2)));
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.anvil.place")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.anvil.place")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
			{
				double _setval = 2;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.tj = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			SololevelingMod.queueServerWork(5, () -> {
				{
					boolean _setval = true;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.leapjump = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			});
		}
	}
}
