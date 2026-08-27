package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemNotifications;
import net.solocraft.util.SystemPlayerAccess;
import net.solocraft.util.RangerCombatManager;
import net.solocraft.util.LevelCapRules;
import net.solocraft.util.LevelRewardRules;
import net.solocraft.util.HunterEvaluationRules;
import net.solocraft.init.SololevelingModGameRules;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;

@EventBusSubscriber
public class LevelUpProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !SystemPlayerAccess.hasSystem(entity))
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(playerVars -> {
			// Store initial values for comparison
			double initialXp = playerVars.Xp;
			int initialLevel = (int) playerVars.Level;
			double initialRank = playerVars.HunterRank;
			// Calculate potential level ups
			int levelCap = LevelCapRules.resolveCap(world.getLevelData().getGameRules()
					.getInt(SololevelingModGameRules.SOLO_LEVELING_LEVEL_CAP));
			int newLevel = initialLevel;
			double remainingXp = initialXp;
			double requiredXP;
			while (true) {
				// The cap is the summit of the run, not a discard. XP earned past it
				// stays banked on the variable so lifting the cap later -- or setting
				// it to unlimited -- resumes exactly where the player actually was.
				if (LevelCapRules.isCapped(newLevel, levelCap))
					break;
				requiredXP = (newLevel * 16) + 8;
				if (remainingXp < requiredXP || newLevel >= Integer.MAX_VALUE - 1)
					break;
				remainingXp -= requiredXP;
				newLevel++;
			}
			int levelsGained = newLevel - initialLevel;
			if (levelsGained > 0 && LevelCapRules.isCapped(newLevel, levelCap))
				announcePeak(entity, playerVars);
			if (levelsGained > 0) {
				// Update all player stats atomically
				playerVars.Level = newLevel;
				playerVars.Xp = remainingXp;
				playerVars.Fatigue = 0;
				// Once evaluated, level progression establishes a consistent minimum
				// rank without erasing a higher awakening result. Unranked players still
				// receive their first roll from the Hunter Evaluator.
				if (initialRank > 0.0D) {
					int levelFloor = HunterEvaluationRules.rankFloorForLevel(newLevel);
					playerVars.HunterRank = Math.min(HunterEvaluationRules.RANK_COUNT,
							Math.max(initialRank, levelFloor));
				}
				int rankPromotions = Math.max(0,
						(int) Math.round(playerVars.HunterRank - initialRank));
				// Apply stat increases
				playerVars.Vitality += levelsGained;
				playerVars.Strength += levelsGained;
				playerVars.Intelligence += levelsGained;
				playerVars.perception += levelsGained;
				playerVars.Speed += levelsGained;
				playerVars.Durability += levelsGained;
				playerVars.SkillPoints += LevelRewardRules.skillPointsForLevels(levelsGained);
				// Sync all changes at once
				playerVars.syncPlayerVariables(entity);
				// Client-side effects
				if (!world.isClientSide()) {
					if (rankPromotions > 0)
						grantRankSkills(entity, (int) Math.round(playerVars.Classes), rankPromotions);
					if (entity instanceof ServerPlayer player) {
						SystemNotifications.showTitleUnder(player, SystemNotifications.ACCENT, 80,
								Component.literal("LEVEL UP").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
								Component.literal("Lv " + initialLevel + " -> " + newLevel).withStyle(ChatFormatting.YELLOW));
					}
					// Play sound
					((Level) world).playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 2, 1);
					// If rank increased, show additional message
					if (playerVars.HunterRank > initialRank) {
						if (entity instanceof ServerPlayer player) {
							SystemNotifications.showTitleUnder(player, 0xFFFFB83D, 80,
									Component.literal("RANK UP").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
									Component.literal("Hunter Rank " + Math.round(initialRank) + " -> " + Math.round(playerVars.HunterRank)).withStyle(ChatFormatting.GOLD));
						}
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

	private static final String PEAK_ANNOUNCED_TAG = "slr_level_peak_announced";

	/**
	 * Announces the summit once. The marker lives in persistent data rather than
	 * PlayerVariables because it is purely cosmetic -- nothing reads it back and
	 * it never needs to reach the client.
	 */
	private static void announcePeak(Entity entity, SololevelingModVariables.PlayerVariables playerVars) {
		if (!(entity instanceof ServerPlayer player)
				|| player.getPersistentData().getBoolean(PEAK_ANNOUNCED_TAG))
			return;
		player.getPersistentData().putBoolean(PEAK_ANNOUNCED_TAG, true);
		SystemNotifications.showTitleUnder(player, 0xFF9B5CFF, 140,
				Component.literal("THE PEAK").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
				Component.literal("There is nothing left to climb. Return to where it began.")
						.withStyle(ChatFormatting.WHITE));
	}

	private static void grantRankSkills(Entity entity, int playerClass, int promotions) {
		if (entity == null || promotions <= 0)
			return;
		if (playerClass == 6) {
			if (entity instanceof ServerPlayer player)
				RangerCombatManager.reconcileRanger(player);
			return;
		}
		for (int promotion = 0; promotion < promotions; promotion++) {
			switch (playerClass) {
				case 1 -> MasterylvlupassassinProcedure.execute(entity);
				case 2 -> MasterylvlupMageProcedure.execute(entity);
				case 3 -> MasterylvlupFighterProcedure.execute(entity);
				case 4 -> MasterylvlupTankerProcedure.execute(entity);
				case 5 -> MasterylvlupHealerProcedure.execute(entity);
				default -> {
					return;
				}
			}
		}
	}
}
