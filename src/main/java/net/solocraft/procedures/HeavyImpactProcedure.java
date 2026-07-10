package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.SololevelingMod;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.solocraft.util.CooldownManager;

public class HeavyImpactProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		CooldownManager.set(entity, "Slash Dash", 200);
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 2, 1);
			} else {
				_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 2, 1, false);
			}
		}
		{
			boolean _setval = true;
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.rushattack = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
		SololevelingMod.queueServerWork(20, () -> {
			{
				boolean _setval = false;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.rushattack = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		});
	}
}
