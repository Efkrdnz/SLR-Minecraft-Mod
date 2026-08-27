package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.AncientGolemEntity;
import net.solocraft.entity.BarukaEntity;
import net.solocraft.entity.BeruBossEntity;
import net.solocraft.entity.BloodRedComIgrisEntity;
import net.solocraft.entity.FangedKasakaEntity;
import net.solocraft.entity.FuturisticGolemEntity;
import net.solocraft.entity.GemGolemEntity;
import net.solocraft.entity.GoblinKingEntity;
import net.solocraft.entity.KamishEntity;
import net.solocraft.util.RewardManager;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.SkillPointRules;
import net.solocraft.util.SystemAuthorityManager;
import net.solocraft.util.StoryModeIntroManager;
import net.solocraft.util.SystemNotifications;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-authoritative boss reward queueing with robust kill-credit resolution. */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class RewardGainAdvProcedure {
	private static final String PAID_TAG_PREFIX = "slr_system_reward_paid/";

	private RewardGainAdvProcedure() {
	}

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event == null || event.getEntity().level().isClientSide())
			return;
		queueBossReward(event.getEntity(), resolveRewardOwner(event.getSource().getEntity(),
				event.getSource().getDirectEntity(), event.getEntity()));
	}

	/** Compatibility entry point retained for legacy procedure callers. */
	public static void execute(Entity entity, Entity sourceentity) {
		queueBossReward(entity, resolveRewardOwner(sourceentity, sourceentity,
				entity instanceof LivingEntity living ? living : null));
	}

	private static void queueBossReward(Entity target, ServerPlayer player) {
		if (target == null || player == null || StoryModeIntroManager.isIntroActive(target))
			return;
		// A released System issues no more rewards. The kill still counts for
		// everything else -- loot, advancements elsewhere, shadows -- but the
		// System reward inbox has stopped being filled.
		if (SystemAuthorityManager.isReleased(player))
			return;
		boolean runtimeSpawn = target.getPersistentData().getBoolean(
				net.solocraft.dungeon.runtime.DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG);
		boolean redGateBaruka = target instanceof BarukaEntity
				&& net.solocraft.dungeon.runtime.SnowRedGateArenaManager.isArenaMob(target);
		if (runtimeSpawn && !redGateBaruka)
			return;

		RewardDefinition reward = rewardFor(target);
		if (reward == null || alreadyPaid(player, reward))
			return;
		for (String entry : reward.entries())
			RewardManager.appendReward(player, entry);
		player.getPersistentData().putBoolean(PAID_TAG_PREFIX + reward.key(), true);
		awardAdvancement(player, reward.advancement());
		SystemNotifications.showTitleUnder(player, 0xFFFFB83D, 90,
				Component.literal("REWARDS AVAILABLE").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.literal("Boss rewards were added to System Rewards.")
						.withStyle(ChatFormatting.GRAY));
	}

	/**
	 * First-kill skill points, scaled by how dangerous the boss is.
	 *
	 * <p>The tier is an offset from {@link SkillPointRules#BOSS_FIRST_KILL_MIN},
	 * so the whole roster stays inside the intended 3-10 band no matter how the
	 * bounds are retuned later.
	 */
	private static String bossSkillPoints(int tier) {
		return "SP" + SkillPointRules.bossFirstKill(tier);
	}

	private static RewardDefinition rewardFor(Entity target) {
		if (target instanceof FangedKasakaEntity)
			return reward("kasakas_domain", "kasakas_domain", "FR", bossSkillPoints(2),
					"ITEM:sololeveling:kasakas_venom_fangs");
		if (target instanceof BloodRedComIgrisEntity)
			return reward("blood_red_commander_igris", "blood_red_commander_igris", "FR",
					bossSkillPoints(3),
					"ITEM:sololeveling:telekinesis_stone", "GOLD5000");
		if (target instanceof BeruBossEntity)
			return reward("ant_king", "ant_king", "FR", bossSkillPoints(6), "GOLD10000");
		if (target instanceof GoblinKingEntity)
			return reward("goblin_king_adv", "goblin_king_adv", "FR", bossSkillPoints(0),
					"ITEMBOX", "ITEM:sololeveling:medium_health_potion");
		if (target instanceof GemGolemEntity)
			return reward("ancient_golem_adv", "ancient_golem_adv", "FR",
					bossSkillPoints(0), "ITEMBOX");
		if (target instanceof AncientGolemEntity)
			return reward("gem_golem_adv", "gem_golem_adv", "FR",
					bossSkillPoints(1), "ITEMBOX");
		if (target instanceof FuturisticGolemEntity)
			return reward("futuristic_golem_adv", "futuristic_golem_adv", "FR",
					bossSkillPoints(2), "GOLD2000");
		if (target instanceof BarukaEntity)
			return reward("baruka_adv", "baruka_adv", "FR", bossSkillPoints(5),
					"ITEM:sololeveling:barukas_dagger");
		if (target instanceof KamishEntity)
			return reward("kamish_adv", "kamish_adv", "FR", bossSkillPoints(7),
					"GOLD15000");
		return null;
	}

	private static RewardDefinition reward(String key, String advancement, String... entries) {
		return new RewardDefinition(key, advancement, List.of(entries));
	}

	private static boolean alreadyPaid(ServerPlayer player, RewardDefinition reward) {
		if (player.getPersistentData().getBoolean(PAID_TAG_PREFIX + reward.key()))
			return true;
		AdvancementHolder advancement = player.server.getAdvancements().get(
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, reward.advancement()));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static void awardAdvancement(ServerPlayer player, String path) {
		AdvancementHolder advancement = player.server.getAdvancements().get(
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, path));
		if (advancement == null)
			return;
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		List<String> remainingCriteria = new ArrayList<>();
		progress.getRemainingCriteria().forEach(remainingCriteria::add);
		for (String criterion : remainingCriteria)
			player.getAdvancements().award(advancement, criterion);
	}

	private static ServerPlayer resolveRewardOwner(Entity source, Entity direct,
			LivingEntity killCredit) {
		ServerPlayer owner = resolveOwner(source);
		if (owner != null)
			return owner;
		owner = resolveOwner(direct);
		if (owner != null)
			return owner;
		return resolveOwner(killCredit == null ? null : killCredit.getKillCredit());
	}

	private static ServerPlayer resolveOwner(Entity candidate) {
		if (candidate instanceof ServerPlayer player)
			return player;
		if (candidate instanceof Projectile projectile)
			return resolveOwner(projectile.getOwner());
		if (candidate instanceof TamableAnimal tame)
			return resolveOwner(tame.getOwner());
		if (candidate != null) {
			UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(candidate);
			if (ownerId != null && candidate.getServer() != null)
				return candidate.getServer().getPlayerList().getPlayer(ownerId);
		}
		return null;
	}

	private record RewardDefinition(String key, String advancement, List<String> entries) {
	}
}
