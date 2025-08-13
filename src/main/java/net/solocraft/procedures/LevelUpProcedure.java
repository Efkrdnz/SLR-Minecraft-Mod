package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class LevelUpProcedure {
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
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(playerVars -> {
			// Store initial values for comparison
			double initialXp = playerVars.Xp;
			int initialLevel = (int) playerVars.Level;
			double initialRank = playerVars.HunterRank;
			// Calculate potential level ups
			int newLevel = initialLevel;
			double remainingXp = initialXp;
			double requiredXP;
			while (true) {
				requiredXP = (newLevel * 16) + 8;
				if (remainingXp < requiredXP || newLevel >= Integer.MAX_VALUE - 1)
					break;
				remainingXp -= requiredXP;
				newLevel++;
			}
			int levelsGained = newLevel - initialLevel;
			if (levelsGained > 0) {
				// Update all player stats atomically
				playerVars.Level = newLevel;
				playerVars.Xp = remainingXp;
				playerVars.Fatigue = 0;
				// Handle rank promotion (every 25 levels)
				if (newLevel / 25 > initialLevel / 25) {
					playerVars.HunterRank = Math.min(6, initialRank + (newLevel / 25 - initialLevel / 25));
				}
				// Apply stat increases
				playerVars.Vitality += levelsGained;
				playerVars.Strength += levelsGained;
				playerVars.Intelligence += levelsGained;
				playerVars.perception += levelsGained;
				playerVars.Speed += levelsGained;
				playerVars.Durability += levelsGained;
				// Sync all changes at once
				playerVars.syncPlayerVariables(entity);
				// Client-side effects
				if (!world.isClientSide()) {
					// Level up message
					String titleCommand = String.format("/title @p title [\"\",{\"text\":\"Leveled Up\",\"color\":\"yellow\",\"bold\":true}]");
					String subtitleCommand = String.format("/title @p subtitle [\"\",{\"text\":\"%d → %d\",\"color\":\"yellow\",\"bold\":true}]", initialLevel, newLevel);
					CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), world instanceof ServerLevel ? (ServerLevel) world : null, 4, entity.getName().getString(),
							entity.getDisplayName(), world.getServer(), entity);
					entity.getServer().getCommands().performPrefixedCommand(source, titleCommand);
					entity.getServer().getCommands().performPrefixedCommand(source, subtitleCommand);
					// Play sound
					((Level) world).playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 2, 1);
					// If rank increased, show additional message
					if (playerVars.HunterRank > initialRank) {
						String rankUpCommand = String.format("/title @p title [\"\",{\"text\":\"Rank Up!\",\"color\":\"gold\",\"bold\":true}]");
						entity.getServer().getCommands().performPrefixedCommand(source, rankUpCommand);
					}
				}
				// Apply regeneration effects
				if (entity instanceof LivingEntity livingEntity && !livingEntity.level().isClientSide()) {
					livingEntity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 255, false, false));
					livingEntity.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 10, false, false));
					livingEntity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 3, false, false));
				}
			}
		});
	}
}
