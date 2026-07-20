package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Difficulty;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Locale;
import java.util.HashMap;

@Mod.EventBusSubscriber
public class XPGainProcedure {
	private static final String LAST_PLAYER_DAMAGE_UUID = "SLRLastPlayerDamageUUID";
	private static final String LAST_PLAYER_DAMAGE_EXPIRES = "SLRLastPlayerDamageExpires";
	private static final long FIRE_KILL_CREDIT_WINDOW_TICKS = 20L * 20L;

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

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onEntityHurt(LivingHurtEvent event) {
		if (event == null || event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide())
			return;
		Player player = ShadowKillCreditHelper.creditedPlayer(event.getEntity().level(), event.getSource().getEntity());
		if (player == null)
			player = ShadowKillCreditHelper.creditedPlayer(event.getEntity().level(), event.getSource().getDirectEntity());
		if (player == null || player == event.getEntity())
			return;
		CompoundTag data = event.getEntity().getPersistentData();
		data.putUUID(LAST_PLAYER_DAMAGE_UUID, player.getUUID());
		data.putLong(LAST_PLAYER_DAMAGE_EXPIRES, event.getEntity().level().getGameTime() + FIRE_KILL_CREDIT_WINDOW_TICKS);
	}

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			Entity source = event.getSource().getEntity();
			Player creditedPlayer = ShadowKillCreditHelper.creditedPlayer(event.getEntity().level(), source);
			if (creditedPlayer == null)
				creditedPlayer = ShadowKillCreditHelper.creditedPlayer(event.getEntity().level(), event.getSource().getDirectEntity());
			if (creditedPlayer == null && event.getSource().is(DamageTypeTags.IS_FIRE))
				creditedPlayer = fireKillCredit(event.getEntity());
			execute(event, event.getEntity().level(), event.getEntity(), creditedPlayer != null ? creditedPlayer : source);
		}
	}

	private static Player fireKillCredit(LivingEntity victim) {
		Player vanillaCredit = ShadowKillCreditHelper.creditedPlayer(victim.level(), victim.getKillCredit());
		if (vanillaCredit != null)
			return vanillaCredit;
		CompoundTag data = victim.getPersistentData();
		if (!data.hasUUID(LAST_PLAYER_DAMAGE_UUID))
			return null;
		if (data.getLong(LAST_PLAYER_DAMAGE_EXPIRES) < victim.level().getGameTime()) {
			data.remove(LAST_PLAYER_DAMAGE_UUID);
			data.remove(LAST_PLAYER_DAMAGE_EXPIRES);
			return null;
		}
		if (victim.level() instanceof ServerLevel serverLevel)
			return serverLevel.getServer().getPlayerList().getPlayer(data.getUUID(LAST_PLAYER_DAMAGE_UUID));
		return null;
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceEntity) {
		execute(null, world, entity, sourceEntity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceEntity) {
		if (entity == null || sourceEntity == null)
			return;
		Player xpReceiver = ShadowKillCreditHelper.creditedPlayer(world, sourceEntity);
		if (xpReceiver == null)
			return;
		// Retrieve player variables
		SololevelingModVariables.PlayerVariables playerVars = xpReceiver.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		// Ensure the XP system is enabled for the player
		if (!playerVars.Player)
			return;
		// Check if the entity is in the XP_REWARDS list
		boolean isListed = XP_REWARDS.containsKey(entity.getClass());
		boolean runtimeDungeonMob = entity.getPersistentData().getBoolean(DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG);
		boolean soloDungeonOnly = world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_DUNGEON_PROGRESSION_ONLY);
		// If soloDungeonProgressionOnly is true and the entity is not listed, don't give XP
		if (soloDungeonOnly && !isListed && !runtimeDungeonMob)
			return;
		// Get XP value for the entity (default is 1 XP only if soloDungeonProgressionOnly is false)
		boolean configuredDungeonXp = runtimeDungeonMob && entity.getPersistentData()
				.contains(DungeonMobLevelAdapter.XP_REWARD_TAG, Tag.TAG_ANY_NUMERIC);
		int baseXP = configuredDungeonXp
				? Math.max(0, entity.getPersistentData().getInt(DungeonMobLevelAdapter.XP_REWARD_TAG))
				: isListed ? XP_REWARDS.get(entity.getClass())
				: runtimeDungeonMob ? runtimeDungeonBaseXp(entity) : 1;
		if (baseXP <= 0)
			return;
		int xpMultiplier = world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER);
		double diffMultiplier = difficultyMultiplier(world);
		awardBaseXp(world, xpReceiver, baseXP, diffMultiplier, xpMultiplier, mobLevelXpMultiplier(entity));
	}

	public static void awardBaseXp(LevelAccessor world, Player player, int baseXP) {
		if (world == null || player == null)
			return;
		int xpMultiplier = world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER);
		double diffMultiplier = difficultyMultiplier(world);
		awardBaseXp(world, player, baseXP, diffMultiplier, xpMultiplier, 1);
	}

	private static double difficultyMultiplier(LevelAccessor world) {
		if (world.getDifficulty() == Difficulty.NORMAL)
			return 0.75;
		if (world.getDifficulty() == Difficulty.HARD)
			return 0.5;
		return 1;
	}

	private static double mobLevelXpMultiplier(Entity entity) {
		double statMultiplier = entity.getPersistentData().getDouble(EntityLoadedLevelPresetProcedure.LEVEL_STAT_MULTIPLIER_TAG);
		if (statMultiplier <= 1) {
			double level = Math.max(0, entity.getPersistentData().getDouble("Level"));
			if (level <= 0)
				return 1;
			// Conservative fallback for mobs saved before stat-multiplier tracking was added.
			statMultiplier = 1 + Math.min(200, level) * 0.005D;
		}
		return Math.min(3, Math.max(1, Math.sqrt(statMultiplier)));
	}

	private static int runtimeDungeonBaseXp(Entity entity) {
		int level = Math.max(1, Math.min(1_000, entity.getPersistentData().getInt("Level")));
		String role = entity.getPersistentData().getString(DungeonMobLevelAdapter.ROLE_TAG);
		return switch (role) {
			case "boss" -> Math.max(25, level * 2);
			case "elite" -> Math.max(3, level / 3);
			default -> Math.max(1, level / 5);
		};
	}

	private static void awardBaseXp(LevelAccessor world, Player player, int baseXP, double diffMultiplier, int xpMultiplier, double mobLevelMultiplier) {
		SololevelingModVariables.PlayerVariables playerVars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (!playerVars.Player)
			return;
		double totalXP = diffMultiplier * playerVars.xpmultiplier * (xpMultiplier / 10.0) * baseXP * mobLevelMultiplier;
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
