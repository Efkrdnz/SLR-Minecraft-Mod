package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.entity.StoneGolemEntity;
import net.solocraft.entity.SteelFangWolfEntity;
import net.solocraft.entity.SteelFangedLycanEntity;
import net.solocraft.entity.SpiderBossEntity;
import net.solocraft.entity.SkeletonWarriorEntity;
import net.solocraft.entity.SkeletonSummonerEntity;
import net.solocraft.entity.SkeletonBruteEntity;
import net.solocraft.entity.RedAntsEntity;
import net.solocraft.entity.PolarBearEntity;
import net.solocraft.entity.OrcEntity;
import net.solocraft.entity.MutatedEntity;
import net.solocraft.entity.MiniGemGolemEntity;
import net.solocraft.entity.KargalganEntity;
import net.solocraft.entity.KamishEntity;
import net.solocraft.entity.IceElfEntity;
import net.solocraft.entity.HighOrcEntity;
import net.solocraft.entity.GreenOrcEntity;
import net.solocraft.entity.GoblinMageEntity;
import net.solocraft.entity.GoblinKingEntity;
import net.solocraft.entity.GoblinClubEntity;
import net.solocraft.entity.GoblinArcherEntity;
import net.solocraft.entity.GemGolemEntity;
import net.solocraft.entity.FuturisticGolemEntity;
import net.solocraft.entity.FangedKasakaEntity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.CentipedeEntity;
import net.solocraft.entity.BloodRedComIgrisEntity;
import net.solocraft.entity.BeruBossEntity;
import net.solocraft.entity.BarukaEntity;
import net.solocraft.entity.AncientSamuraiEntity;
import net.solocraft.entity.AncientGolemEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Difficulty;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Locale;
import java.util.HashMap;

@Mod.EventBusSubscriber
public class XPGainProcedure {
	// Define XP rewards for each entity type
	private static final Map<Class<? extends Entity>, Integer> XP_REWARDS = new HashMap<>();
	static {
		// Normal Enemies
		XP_REWARDS.put(GoblinArcherEntity.class, 3);
		XP_REWARDS.put(GoblinMageEntity.class, 3);
		XP_REWARDS.put(GoblinClubEntity.class, 3);
		XP_REWARDS.put(StoneGolemEntity.class, 15);
		XP_REWARDS.put(SteelFangWolfEntity.class, 5);
		XP_REWARDS.put(SteelFangedLycanEntity.class, 5);
		XP_REWARDS.put(OrcEntity.class, 25);
		XP_REWARDS.put(RedAntsEntity.class, 40);
		XP_REWARDS.put(PolarBearEntity.class, 16);
		XP_REWARDS.put(IceElfEntity.class, 35);
		XP_REWARDS.put(MiniGemGolemEntity.class, 22);
		XP_REWARDS.put(MutatedEntity.class, 25);
		XP_REWARDS.put(SkeletonWarriorEntity.class, 15);
		XP_REWARDS.put(SkeletonBruteEntity.class, 15);
		XP_REWARDS.put(DKnight1Entity.class, 10);
		XP_REWARDS.put(DKnight2Entity.class, 10);
		XP_REWARDS.put(DKnight3Entity.class, 10);
		XP_REWARDS.put(CentipedeEntity.class, 30);
		XP_REWARDS.put(GreenOrcEntity.class, 25);
		XP_REWARDS.put(HighOrcEntity.class, 40);
		// Bosses
		XP_REWARDS.put(AncientSamuraiEntity.class, 600);
		XP_REWARDS.put(GoblinKingEntity.class, 900);
		XP_REWARDS.put(SpiderBossEntity.class, 1000);
		XP_REWARDS.put(GemGolemEntity.class, 2000);
		XP_REWARDS.put(AncientGolemEntity.class, 1500);
		XP_REWARDS.put(FangedKasakaEntity.class, 1100);
		XP_REWARDS.put(FuturisticGolemEntity.class, 1200);
		XP_REWARDS.put(BloodRedComIgrisEntity.class, 3000);
		XP_REWARDS.put(BarukaEntity.class, 5000);
		XP_REWARDS.put(SkeletonSummonerEntity.class, 6750);
		XP_REWARDS.put(KargalganEntity.class, 8000);
		XP_REWARDS.put(BeruBossEntity.class, 10000);
		XP_REWARDS.put(KamishEntity.class, 20000);
	}

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceEntity) {
		execute(null, world, entity, sourceEntity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceEntity) {
		if (entity == null || sourceEntity == null)
			return;
		// Determine who should receive XP
		final Entity xpReceiver;
		// If the source is a player, they get XP
		if (sourceEntity instanceof Player) {
			xpReceiver = sourceEntity;
		}
		// If the source is a tamed entity, give XP to its owner
		else if (sourceEntity instanceof TamableAnimal tamable && tamable.isTame()) {
			Entity owner = tamable.getOwner();
			if (owner instanceof Player) {
				xpReceiver = owner;
			} else {
				return; // No valid XP receiver found
			}
		} else {
			return; // If source is neither a player nor a tamed entity, ignore XP gain
		}
		// Retrieve player variables
		SololevelingModVariables.PlayerVariables playerVars = xpReceiver.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		// Ensure the XP system is enabled for the player
		if (!playerVars.Player)
			return;
		// Check if the entity is in the XP_REWARDS list
		boolean isListed = XP_REWARDS.containsKey(entity.getClass());
		boolean soloDungeonOnly = world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_DUNGEON_PROGRESSION_ONLY);
		// If soloDungeonProgressionOnly is true and the entity is not listed, don't give XP
		if (soloDungeonOnly && !isListed)
			return;
		// Get XP value for the entity (default is 1 XP only if soloDungeonProgressionOnly is false)
		int baseXP = isListed ? XP_REWARDS.get(entity.getClass()) : 1;
		int xpMultiplier = world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER);
		double diffMultiplier = difficultyMultiplier(world);
		awardBaseXp(world, (Player) xpReceiver, baseXP, diffMultiplier, xpMultiplier);
	}

	public static void awardBaseXp(LevelAccessor world, Player player, int baseXP) {
		if (world == null || player == null)
			return;
		int xpMultiplier = world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER);
		double diffMultiplier = difficultyMultiplier(world);
		awardBaseXp(world, player, baseXP, diffMultiplier, xpMultiplier);
	}

	private static double difficultyMultiplier(LevelAccessor world) {
		if (world.getDifficulty() == Difficulty.NORMAL)
			return 0.75;
		if (world.getDifficulty() == Difficulty.HARD)
			return 0.5;
		return 1;
	}

	private static void awardBaseXp(LevelAccessor world, Player player, int baseXP, double diffMultiplier, int xpMultiplier) {
		SololevelingModVariables.PlayerVariables playerVars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (!playerVars.Player)
			return;
		double totalXP = diffMultiplier * playerVars.xpmultiplier * (xpMultiplier / 10.0) * baseXP;
		double newXP = playerVars.Xp + totalXP;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.Xp = newXP;
			capability.syncPlayerVariables(player);
		});
		if (!player.level().isClientSide()) {
			String formattedXP = String.format(Locale.FRANCE, "%,.1f", totalXP);
			player.displayClientMessage(Component.literal("\u00A7bGained \u00A7f" + formattedXP + "\u00A7b XP"), true);
		}
	}
}
