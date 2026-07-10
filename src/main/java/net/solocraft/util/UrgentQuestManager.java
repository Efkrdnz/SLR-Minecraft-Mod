package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.UrgentQuestStatusMessage;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class UrgentQuestManager {
	private static final int ACCENT = 0xFFFF3D3D;
	private static final double START_CHANCE = 0.35D;
	private static final double RED_OR_PROCEDURAL_START_CHANCE = 0.55D;
	private static final double REPEAT_PVP_RUNESTONE_CHANCE = 0.35D;
	private static final int WEAPON_HITS_REQUIRED = 3;
	private static final long WEAPON_HIT_WINDOW_TICKS = 8L * 20L;
	private static final long PVP_RETRIGGER_COOLDOWN_TICKS = 60L * 20L;

	private static final String ACTIVE = "sl_urgent_active";
	private static final String ID = "sl_urgent_id";
	private static final String KIND = "sl_urgent_kind";
	private static final String FAMILY = "sl_urgent_family";
	private static final String TITLE = "sl_urgent_title";
	private static final String OBJECTIVE = "sl_urgent_objective";
	private static final String PROGRESS = "sl_urgent_progress";
	private static final String TARGET = "sl_urgent_target";
	private static final String START_TICK = "sl_urgent_start_tick";
	private static final String TIME_LIMIT = "sl_urgent_time_limit";
	private static final String XP_REWARD = "sl_urgent_xp_reward";
	private static final String NO_SKILLS_FAILED = "sl_urgent_no_skills_failed";
	private static final String ACTIVE_TAG = "sl_urgent_active_tag";
	private static final String LAST_TAG = "sl_urgent_last_tag";
	private static final String DUNGEON_ID = "sl_urgent_dungeon_id";
	private static final String PROCEDURAL_DUNGEON = "slr_procedural_dungeon";
	private static final String PROCEDURAL_RED = "slr_procedural_red";
	private static final String PVP_TARGETS = "sl_urgent_pvp_targets";
	private static final String PVP_DEFEATED = "sl_urgent_pvp_defeated";
	private static final String PVP_ATTACKER = "sl_urgent_pvp_attacker";
	private static final String PVP_RUNESTONES = "sl_urgent_pvp_runestones";
	private static final String PVP_LAST_QUEST_TICK = "sl_urgent_pvp_last_quest_tick";
	private static final String PVP_FIRST_REWARD_CLAIMED = "sl_urgent_pvp_first_reward_claimed";

	private static final String KIND_KILL = "kill";
	private static final String KIND_CLEAR = "clear";
	private static final String KIND_NO_SKILLS = "no_skills";
	private static final String KIND_PVP = "pvp";

	private static final Map<AttackPair, Long> RECENT_CRITICALS = new HashMap<>();
	private static final Map<AttackPair, HitWindow> WEAPON_HITS = new HashMap<>();
	private static final Map<String, String> RUNESTONE_ALIASES = Map.ofEntries(
			Map.entry("back_step", "runestone_backstep"),
			Map.entry("cross_strike", "runestone_criticalstrike"),
			Map.entry("curse_smoke", "runestone_cursed_smoke"),
			Map.entry("ground_slam", "runestone_slam"),
			Map.entry("haste_buff", "runestone_haste"),
			Map.entry("hyper_focus", "runestone_hyperfocus"),
			Map.entry("monarch_s_domain", "runestone_monarchs_domain"),
			Map.entry("murderious_intent", "murderious_intent_stone"),
			Map.entry("physical_buff", "runestone_physical"),
			Map.entry("ruler_s_hand", "telekinesis_stone"),
			Map.entry("rulers_hand", "telekinesis_stone"),
			Map.entry("stealth", "stealth_stone"),
			Map.entry("slash_dash", "runestone_slashdash"),
			Map.entry("sword_of_light", "runestone_swordof_light"),
			Map.entry("water_slash", "runestone_waterslash"));

	private UrgentQuestManager() {
	}

	public static void markDungeonId(Entity entity, String dungeonId) {
		if (entity != null && dungeonId != null && !dungeonId.isBlank())
			entity.getPersistentData().putString(DUNGEON_ID, dungeonId);
	}

	public static void onSkillUsed(Entity entity, String skillName) {
		if (!(entity instanceof ServerPlayer player) || skillName == null || skillName.isBlank())
			return;
		if (!player.getPersistentData().getBoolean(ACTIVE))
			return;
		if (!KIND_NO_SKILLS.equals(player.getPersistentData().getString(KIND)))
			return;
		player.getPersistentData().putBoolean(NO_SKILLS_FAILED, true);
		fail(player, "Skill used: " + skillName);
	}

	@SubscribeEvent
	public static void onCriticalHit(CriticalHitEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer attacker) || !(event.getTarget() instanceof ServerPlayer victim))
			return;
		if (!event.isVanillaCritical() && event.getDamageModifier() <= 1.0F)
			return;
		RECENT_CRITICALS.put(new AttackPair(attacker.getUUID(), victim.getUUID()), attacker.level().getGameTime());
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer victim) || victim.level().isClientSide() || event.getAmount() <= 0.0F)
			return;
		ServerPlayer attacker = attackingPlayer(event.getSource());
		if (attacker == null || attacker == victim || sameParty(attacker, victim))
			return;
		if (victim.getPersistentData().getBoolean(ACTIVE))
			return;

		long now = victim.level().getGameTime();
		if (victim.getPersistentData().contains(PVP_LAST_QUEST_TICK) && now - victim.getPersistentData().getLong(PVP_LAST_QUEST_TICK) < PVP_RETRIGGER_COOLDOWN_TICKS)
			return;

		AttackPair pair = new AttackPair(attacker.getUUID(), victim.getUUID());
		pruneAggressionTracking(now);
		long criticalTick = RECENT_CRITICALS.getOrDefault(pair, Long.MIN_VALUE);
		boolean critical = now - criticalTick >= 0L && now - criticalTick <= 3L;
		if (critical || isOffensiveSkillDamage(event.getSource())) {
			startPvpQuest(victim, attacker);
			clearAggressionTracking(victim.getUUID());
			return;
		}

		if (!isWeaponPressure(event.getSource(), attacker))
			return;
		HitWindow previous = WEAPON_HITS.get(pair);
		int hits = previous != null && now - previous.firstTick <= WEAPON_HIT_WINDOW_TICKS ? previous.hits + 1 : 1;
		long firstTick = previous != null && now - previous.firstTick <= WEAPON_HIT_WINDOW_TICKS ? previous.firstTick : now;
		WEAPON_HITS.put(pair, new HitWindow(firstTick, now, hits));
		if (hits >= WEAPON_HITS_REQUIRED) {
			startPvpQuest(victim, attacker);
			clearAggressionTracking(victim.getUUID());
		}
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!(event.getEntity() instanceof ServerPlayer clone))
			return;
		CompoundTag originalRoot = event.getOriginal().getPersistentData();
		if (!originalRoot.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			return;
		CompoundTag originalPersisted = originalRoot.getCompound(Player.PERSISTED_NBT_TAG);
		if (originalPersisted.getBoolean(PVP_FIRST_REWARD_CLAIMED))
			persistentPlayerData(clone).putBoolean(PVP_FIRST_REWARD_CLAIMED, true);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			syncQuestStatus(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			syncQuestStatus(player);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player))
			return;
		if (player.tickCount % 20 != 0)
			return;
		boolean urgentActive = player.getPersistentData().getBoolean(ACTIVE);
		if (urgentActive)
			syncQuestStatus(player);
		if (urgentActive && KIND_PVP.equals(player.getPersistentData().getString(KIND)))
			return;
		if (!isDungeonDimension(player.level())) {
			if (player.getPersistentData().getBoolean(ACTIVE))
				fail(player, "Dungeon left");
			return;
		}
		String tag = player.getPersistentData().getString("dungeon_tag");
		if (tag.isBlank())
			return;
		if (player.getPersistentData().getBoolean(ACTIVE)) {
			checkTimeout(player);
			return;
		}
		if (tag.equals(player.getPersistentData().getString(LAST_TAG)))
			return;
		player.getPersistentData().putString(LAST_TAG, tag);
		double chance = player.getPersistentData().getBoolean(PROCEDURAL_DUNGEON) || player.getPersistentData().getBoolean(PROCEDURAL_RED) ? RED_OR_PROCEDURAL_START_CHANCE : START_CHANCE;
		if (player.getRandom().nextDouble() > chance)
			return;
		startRandomQuest(player, dungeonId(player), tag);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event == null || event.getEntity() == null || event.getSource() == null)
			return;
		ServerPlayer player = creditedPlayer(event.getEntity().level(), event.getSource().getEntity());
		Entity killed = event.getEntity();

		if (killed instanceof ServerPlayer killedPlayer) {
			if (player != null && player.getPersistentData().getBoolean(ACTIVE) && KIND_PVP.equals(player.getPersistentData().getString(KIND)) && recordPvpDefeat(player, killedPlayer))
				return;
			if (killedPlayer.getPersistentData().getBoolean(ACTIVE) && KIND_PVP.equals(killedPlayer.getPersistentData().getString(KIND)) && player != null && isPvpTarget(killedPlayer, player.getUUID())) {
				fail(killedPlayer, "You were defeated");
				return;
			}
		}

		if (player == null || !player.getPersistentData().getBoolean(ACTIVE))
			return;
		String kind = player.getPersistentData().getString(KIND);
		if (KIND_KILL.equals(kind)) {
			String family = player.getPersistentData().getString(FAMILY);
			if (!matchesFamily(killed, family))
				return;
			int progress = player.getPersistentData().getInt(PROGRESS) + 1;
			player.getPersistentData().putInt(PROGRESS, progress);
			int target = player.getPersistentData().getInt(TARGET);
			if (progress >= target)
				complete(player);
			else {
				syncQuestStatus(player);
				if (progress == 1 || progress % 5 == 0)
					progressPopup(player, progress, target);
			}
			return;
		}
		if ((KIND_CLEAR.equals(kind) || KIND_NO_SKILLS.equals(kind)) && isBoss(killed)) {
			if (KIND_NO_SKILLS.equals(kind) && player.getPersistentData().getBoolean(NO_SKILLS_FAILED))
				fail(player, "Skill restriction broken");
			else
				complete(player);
		}
	}

	private static void startPvpQuest(ServerPlayer victim, ServerPlayer attacker) {
		if (victim.getPersistentData().getBoolean(ACTIVE))
			return;
		List<ServerPlayer> targetPlayers = attackerPartyTargets(attacker, victim);
		if (targetPlayers.isEmpty())
			return;

		List<String> targetIds = targetPlayers.stream().map(player -> player.getUUID().toString()).toList();
		List<String> runestones = attackerRunestones(attacker);
		victim.getPersistentData().putBoolean(ACTIVE, true);
		victim.getPersistentData().putString(ID, "pvp_killing_intent");
		victim.getPersistentData().putString(KIND, KIND_PVP);
		victim.getPersistentData().putString(TITLE, "Killing Intent");
		victim.getPersistentData().putString(OBJECTIVE, "Defeat the hostile player group");
		victim.getPersistentData().putInt(PROGRESS, 0);
		victim.getPersistentData().putInt(TARGET, targetIds.size());
		victim.getPersistentData().putInt(TIME_LIMIT, 0);
		victim.getPersistentData().putInt(XP_REWARD, 0);
		victim.getPersistentData().putLong(START_TICK, victim.level().getGameTime());
		victim.getPersistentData().putLong(PVP_LAST_QUEST_TICK, victim.level().getGameTime());
		victim.getPersistentData().putString(PVP_TARGETS, String.join(",", targetIds));
		victim.getPersistentData().putString(PVP_DEFEATED, "");
		victim.getPersistentData().putString(PVP_ATTACKER, attacker.getUUID().toString());
		victim.getPersistentData().putString(PVP_RUNESTONES, String.join(",", runestones));
		syncQuestStatus(victim);

		Component undertext = Component.literal("System detected someone\n").withStyle(ChatFormatting.GRAY)
				.append(Component.literal("with killing intent towards the player\n").withStyle(ChatFormatting.DARK_RED))
				.append(Component.literal("Defeat them and ensure your safety\n").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("Defeat Players [0/" + targetIds.size() + "]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
		SystemNotifications.showNegativeTitleUnder(victim, ACCENT, 180, Component.literal("Warning!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), undertext);
	}

	private static boolean recordPvpDefeat(ServerPlayer questOwner, ServerPlayer defeatedPlayer) {
		if (!isPvpTarget(questOwner, defeatedPlayer.getUUID()))
			return false;
		Set<UUID> defeated = parseUuids(questOwner.getPersistentData().getString(PVP_DEFEATED));
		if (!defeated.add(defeatedPlayer.getUUID()))
			return true;
		questOwner.getPersistentData().putString(PVP_DEFEATED, joinUuids(defeated));
		int progress = defeated.size();
		int target = questOwner.getPersistentData().getInt(TARGET);
		questOwner.getPersistentData().putInt(PROGRESS, progress);
		if (progress >= target)
			completePvpQuest(questOwner);
		else {
			syncQuestStatus(questOwner);
			pvpProgressPopup(questOwner, progress, target);
		}
		return true;
	}

	private static void completePvpQuest(ServerPlayer player) {
		List<String> candidates = splitValues(player.getPersistentData().getString(PVP_RUNESTONES));
		boolean firstReward = !persistentPlayerData(player).getBoolean(PVP_FIRST_REWARD_CLAIMED);
		boolean awardRunestone = firstReward || player.getRandom().nextDouble() < REPEAT_PVP_RUNESTONE_CHANCE;
		String reward = fallbackPvpReward(player);
		if (awardRunestone) {
			List<String> newSkillCandidates = candidates.stream().filter(candidate -> !playerAlreadyHasRunestoneSkill(player, candidate)).toList();
			if (!newSkillCandidates.isEmpty()) {
				String selected = newSkillCandidates.get(player.getRandom().nextInt(newSkillCandidates.size()));
				reward = "ITEM:sololeveling:" + selected;
			}
			persistentPlayerData(player).putBoolean(PVP_FIRST_REWARD_CLAIMED, true);
		}

		clearActive(player);
		RewardManager.appendReward(player, reward);
		Component result = Component.literal("Reward added to System Rewards\n").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(RewardManager.displayName(reward).replace("\u00A7l", "")).withStyle(reward.startsWith("ITEM:") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD));
		SystemNotifications.showTitleUnder(player, 0xFF55FF9A, 130, Component.literal("URGENT QUEST COMPLETE").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), result);
	}

	private static void pvpProgressPopup(ServerPlayer player, int progress, int target) {
		Component undertext = Component.literal("Defeat Players [" + progress + "/" + target + "]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		SystemNotifications.showTitleUnder(player, ACCENT, 80, Component.literal("KILLING INTENT").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), undertext);
	}

	private static List<ServerPlayer> attackerPartyTargets(ServerPlayer attacker, ServerPlayer victim) {
		List<ServerPlayer> targets = new ArrayList<>();
		targets.add(attacker);
		String party = partyOf(attacker);
		if (party.isBlank() || attacker.getServer() == null)
			return targets;
		for (ServerPlayer candidate : attacker.getServer().getPlayerList().getPlayers()) {
			if (candidate != attacker && candidate != victim && party.equals(partyOf(candidate)))
				targets.add(candidate);
		}
		return targets;
	}

	private static List<String> attackerRunestones(ServerPlayer attacker) {
		Set<String> result = new HashSet<>();
		for (String skill : SkillListHelper.skills(attacker)) {
			String registryId = runestoneForSkill(skill);
			if (registryId != null)
				result.add(registryId);
		}
		return new ArrayList<>(result);
	}

	private static boolean playerAlreadyHasRunestoneSkill(ServerPlayer player, String runestoneRegistryId) {
		for (String skill : SkillListHelper.skills(player)) {
			String ownedRunestone = runestoneForSkill(skill);
			if (runestoneRegistryId.equals(ownedRunestone))
				return true;
		}
		return false;
	}

	private static String fallbackPvpReward(ServerPlayer player) {
		return player.getRandom().nextBoolean() ? "SP" + (5 + player.getRandom().nextInt(6)) : "GOLD" + (150 + player.getRandom().nextInt(151));
	}

	private static String runestoneForSkill(String skill) {
		if (skill == null || skill.isBlank())
			return null;
		String normalized = skill.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		String registryId = RUNESTONE_ALIASES.getOrDefault(normalized, "runestone_" + normalized);
		Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("sololeveling", registryId));
		return item != null && item != Items.AIR ? registryId : null;
	}

	private static boolean isPvpTarget(ServerPlayer questOwner, UUID targetId) {
		return parseUuids(questOwner.getPersistentData().getString(PVP_TARGETS)).contains(targetId);
	}

	private static Set<UUID> parseUuids(String encoded) {
		Set<UUID> result = new HashSet<>();
		for (String value : splitValues(encoded)) {
			try {
				result.add(UUID.fromString(value));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return result;
	}

	private static List<String> splitValues(String encoded) {
		if (encoded == null || encoded.isBlank())
			return List.of();
		List<String> result = new ArrayList<>();
		for (String value : encoded.split(",")) {
			if (!value.isBlank())
				result.add(value.trim());
		}
		return result;
	}

	private static String joinUuids(Set<UUID> values) {
		return values.stream().map(UUID::toString).sorted().reduce((left, right) -> left + "," + right).orElse("");
	}

	private static void startRandomQuest(ServerPlayer player, String dungeonId, String tag) {
		List<QuestDefinition> pool = questPool(dungeonId);
		if (pool.isEmpty())
			return;
		QuestDefinition quest = pool.get(player.getRandom().nextInt(pool.size()));
		player.getPersistentData().putBoolean(ACTIVE, true);
		player.getPersistentData().putString(ID, quest.id);
		player.getPersistentData().putString(KIND, quest.kind);
		player.getPersistentData().putString(FAMILY, quest.family);
		player.getPersistentData().putString(TITLE, quest.title);
		player.getPersistentData().putString(OBJECTIVE, quest.objective);
		player.getPersistentData().putInt(PROGRESS, 0);
		player.getPersistentData().putInt(TARGET, quest.target);
		player.getPersistentData().putInt(TIME_LIMIT, quest.timeLimitSeconds);
		player.getPersistentData().putInt(XP_REWARD, quest.xpReward);
		player.getPersistentData().putLong(START_TICK, player.level().getGameTime());
		player.getPersistentData().putBoolean(NO_SKILLS_FAILED, false);
		player.getPersistentData().putString(ACTIVE_TAG, tag);
		syncQuestStatus(player);
		SystemNotifications.showTitleUnder(player, ACCENT, 120, Component.literal("§4§lURGENT QUEST"), Component.literal("§c" + quest.objective));
	}

	private static List<QuestDefinition> questPool(String dungeonId) {
		return switch (dungeonId) {
			case "goblin_sewers" -> List.of(
					kill("sewer_goblin_sweep", "Goblin Extermination", "Kill 20 goblins in 100 seconds", "goblin", 20, 100, 180),
					clear("sewer_boss_rush", "Sewer Boss Rush", "Clear Goblin Sewers within 300 seconds", 300, 220),
					noSkills("sewer_no_skills", "Weapon Only", "Clear Goblin Sewers without using skills", 260));
			case "cemetery" -> List.of(
					kill("cemetery_undead_sweep", "Undead Sweep", "Kill 12 skeletons in 140 seconds", "skeleton", 12, 140, 220),
					clear("cemetery_clear_rush", "Graveyard Rush", "Clear the cemetery within 300 seconds", 300, 260),
					noSkills("cemetery_no_skills", "Cold Steel", "Clear the cemetery without using skills", 300));
			case "lab" -> List.of(
					kill("lab_cleanup", "Experiment Cleanup", "Kill 8 lab monsters in 160 seconds", "lab", 8, 160, 280),
					clear("lab_clear_rush", "Lab Shutdown", "Clear the lab within 300 seconds", 300, 340),
					noSkills("lab_no_skills", "Clean Hands", "Clear the lab without using skills", 380));
			case "lush" -> List.of(
					kill("lush_beast_hunt", "Beast Hunt", "Kill 12 beasts in 160 seconds", "beast", 12, 160, 260),
					clear("lush_clear_rush", "Overgrown Gate", "Clear the lush gate within 300 seconds", 300, 300),
					noSkills("lush_no_skills", "Wild Hunt", "Clear the lush gate without using skills", 340));
			case "golem_halls" -> List.of(
					kill("golem_hall_cleanup", "Stone Breaker", "Kill 8 golems in 180 seconds", "golem", 8, 180, 300),
					clear("golem_hall_clear_rush", "Hall Breaker", "Clear the golem halls within 300 seconds", 300, 360),
					noSkills("golem_hall_no_skills", "Pure Strength", "Clear the golem halls without using skills", 420));
			case "kargalgan_throne" -> List.of(
					kill("high_orc_sweep", "High Orc Sweep", "Kill 12 orcs in 160 seconds", "orc", 12, 160, 320),
					clear("kargalgan_clear_rush", "Throne Rush", "Clear Kargalgan's throne within 300 seconds", 300, 420),
					noSkills("kargalgan_no_skills", "Silent Throne", "Clear Kargalgan's throne without using skills", 480));
			case "ant_island" -> List.of(
					kill("ant_cull", "Ant Cull", "Kill 15 ants in 180 seconds", "ant", 15, 180, 360),
					clear("ant_boss_rush", "Commander Hunt", "Clear the ant gate within 300 seconds", 300, 460));
			case "red_gate", "procedural" -> List.of(
					kill("gate_massacre", "Gate Massacre", "Kill 20 dungeon monsters in 180 seconds", "any", 20, 180, 300),
					clear("gate_clear_rush", "Gate Rush", "Clear the dungeon within 300 seconds", 300, 360),
					noSkills("gate_no_skills", "Weapon Trial", "Clear the dungeon without using skills", 420));
			default -> List.of(
					kill("dungeon_hunt", "Dungeon Hunt", "Kill 15 dungeon monsters in 180 seconds", "any", 15, 180, 220),
					clear("dungeon_clear_rush", "Dungeon Rush", "Clear the dungeon within 300 seconds", 300, 280),
					noSkills("dungeon_no_skills", "No Skills", "Clear the dungeon without using skills", 320));
		};
	}

	private static QuestDefinition kill(String id, String title, String objective, String family, int target, int seconds, int xpReward) {
		return new QuestDefinition(id, KIND_KILL, family, title, objective, target, seconds, xpReward);
	}

	private static QuestDefinition clear(String id, String title, String objective, int seconds, int xpReward) {
		return new QuestDefinition(id, KIND_CLEAR, "", title, objective, 1, seconds, xpReward);
	}

	private static QuestDefinition noSkills(String id, String title, String objective, int xpReward) {
		return new QuestDefinition(id, KIND_NO_SKILLS, "", title, objective, 1, 0, xpReward);
	}

	private static void checkTimeout(ServerPlayer player) {
		int seconds = player.getPersistentData().getInt(TIME_LIMIT);
		if (seconds <= 0)
			return;
		long elapsed = (player.level().getGameTime() - player.getPersistentData().getLong(START_TICK)) / 20L;
		if (elapsed > seconds)
			fail(player, "Time expired");
	}

	private static void complete(ServerPlayer player) {
		String title = player.getPersistentData().getString(TITLE);
		int xpReward = player.getPersistentData().getInt(XP_REWARD);
		clearActive(player);
		if (xpReward > 0)
			RewardManager.appendReward(player, "XP" + xpReward);
		SystemNotifications.showTitleUnder(player, 0xFF55FF9A, 110, Component.literal("§a§lURGENT QUEST COMPLETE"), Component.literal("§7" + title + "\nReward added to System Rewards"));
	}

	private static void fail(ServerPlayer player, String reason) {
		String title = player.getPersistentData().getString(TITLE);
		clearActive(player);
		SystemNotifications.showNegativeTitleUnder(player, ACCENT, 90, Component.literal("§4§lURGENT QUEST FAILED"), Component.literal("§7" + title + " - " + reason));
	}

	private static void progressPopup(ServerPlayer player, int progress, int target) {
		String title = player.getPersistentData().getString(TITLE);
		SystemNotifications.showTitleUnder(player, ACCENT, 60, Component.literal("§4§l" + title.toUpperCase(Locale.ROOT)), Component.literal("§c" + progress + "/" + target));
	}

	private static void clearActive(ServerPlayer player) {
		player.getPersistentData().putBoolean(ACTIVE, false);
		player.getPersistentData().remove(ID);
		player.getPersistentData().remove(KIND);
		player.getPersistentData().remove(FAMILY);
		player.getPersistentData().remove(TITLE);
		player.getPersistentData().remove(OBJECTIVE);
		player.getPersistentData().remove(PROGRESS);
		player.getPersistentData().remove(TARGET);
		player.getPersistentData().remove(START_TICK);
		player.getPersistentData().remove(TIME_LIMIT);
		player.getPersistentData().remove(XP_REWARD);
		player.getPersistentData().remove(NO_SKILLS_FAILED);
		player.getPersistentData().remove(ACTIVE_TAG);
		player.getPersistentData().remove(PVP_TARGETS);
		player.getPersistentData().remove(PVP_DEFEATED);
		player.getPersistentData().remove(PVP_ATTACKER);
		player.getPersistentData().remove(PVP_RUNESTONES);
		syncQuestStatus(player);
	}

	private static void syncQuestStatus(ServerPlayer player) {
		boolean active = player.getPersistentData().getBoolean(ACTIVE);
		String title = active ? player.getPersistentData().getString(TITLE) : "";
		String objective = active ? player.getPersistentData().getString(OBJECTIVE) : "";
		String kind = active ? player.getPersistentData().getString(KIND) : "";
		int progress = active ? player.getPersistentData().getInt(PROGRESS) : 0;
		int target = active ? player.getPersistentData().getInt(TARGET) : 0;
		int remaining = -1;
		if (active) {
			int timeLimit = player.getPersistentData().getInt(TIME_LIMIT);
			if (timeLimit > 0) {
				long elapsed = Math.max(0L, (player.level().getGameTime() - player.getPersistentData().getLong(START_TICK)) / 20L);
				remaining = Math.max(0, timeLimit - (int) elapsed);
			}
		}
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new UrgentQuestStatusMessage(active, title, objective, kind, progress, target, remaining));
	}

	private static boolean isDungeonDimension(Level level) {
		ResourceLocation id = level.dimension().location();
		return "sololeveling".equals(id.getNamespace()) && id.getPath().startsWith("dungeon_dimension");
	}

	private static String dungeonId(ServerPlayer player) {
		String explicit = player.getPersistentData().getString(DUNGEON_ID);
		if (!explicit.isBlank())
			return explicit;
		if (player.getPersistentData().getBoolean(PROCEDURAL_DUNGEON))
			return player.getPersistentData().getBoolean(PROCEDURAL_RED) ? "red_gate" : "procedural";
		String path = player.level().dimension().location().getPath();
		return switch (path) {
			case "dungeon_dimension_d" -> "goblin_sewers";
			case "dungeon_dimension_b" -> "cemetery";
			case "dungeon_dimension_a" -> "lab";
			case "dungeon_dimension_s" -> "ant_island";
			case "dungeon_dimension_snow" -> "red_gate";
			default -> "generic";
		};
	}

	private static ServerPlayer creditedPlayer(Level level, Entity source) {
		if (source == null)
			return null;
		if (source instanceof ServerPlayer player)
			return player;
		if (source instanceof Projectile projectile && projectile.getOwner() != null)
			return creditedPlayer(level, projectile.getOwner());
		if (source instanceof TamableAnimal tame && tame.isTame() && tame.getOwner() instanceof ServerPlayer owner)
			return owner;
		UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(source);
		if (ownerId != null && level instanceof ServerLevel serverLevel && serverLevel.getPlayerByUUID(ownerId) instanceof ServerPlayer owner)
			return owner;
		return null;
	}

	private static ServerPlayer attackingPlayer(DamageSource source) {
		if (source == null)
			return null;
		Entity causing = source.getEntity();
		if (causing instanceof ServerPlayer player)
			return player;
		if (causing instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner)
			return owner;
		Entity direct = source.getDirectEntity();
		if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner)
			return owner;
		return null;
	}

	private static boolean isOffensiveSkillDamage(DamageSource source) {
		if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))
			return true;
		String id = source.getMsgId().toLowerCase(Locale.ROOT);
		return id.equals("mage") || id.equals("assassin") || id.equals("fighter") || id.equals("ranger") || id.equals("tanker") || id.equals("magic_beast") || id.equals("knight_critical");
	}

	private static boolean isWeaponPressure(DamageSource source, ServerPlayer attacker) {
		if (attacker.getMainHandItem().isEmpty())
			return false;
		return source.is(DamageTypes.PLAYER_ATTACK) || source.getDirectEntity() instanceof Projectile;
	}

	private static boolean sameParty(ServerPlayer first, ServerPlayer second) {
		String party = partyOf(first);
		return !party.isBlank() && party.equals(partyOf(second));
	}

	private static String partyOf(ServerPlayer player) {
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).party;
	}

	private static void pruneAggressionTracking(long now) {
		RECENT_CRITICALS.entrySet().removeIf(entry -> now - entry.getValue() > 20L);
		WEAPON_HITS.entrySet().removeIf(entry -> now - entry.getValue().lastTick > WEAPON_HIT_WINDOW_TICKS);
	}

	private static void clearAggressionTracking(UUID victimId) {
		RECENT_CRITICALS.keySet().removeIf(pair -> pair.victim.equals(victimId));
		WEAPON_HITS.keySet().removeIf(pair -> pair.victim.equals(victimId));
	}

	private static CompoundTag persistentPlayerData(ServerPlayer player) {
		CompoundTag root = player.getPersistentData();
		if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
		return root.getCompound(Player.PERSISTED_NBT_TAG);
	}

	private static boolean matchesFamily(Entity entity, String family) {
		if ("any".equals(family))
			return !isBoss(entity);
		String name = entity.getClass().getSimpleName().toLowerCase(Locale.ROOT);
		return switch (family) {
			case "goblin" -> name.contains("goblin") && !name.contains("king");
			case "skeleton" -> name.contains("skeleton");
			case "lab" -> name.contains("mutated") || name.contains("mini") || name.contains("golem") && !isBoss(entity);
			case "beast" -> name.contains("wolf") || name.contains("lycan") || name.contains("bear");
			case "golem" -> name.contains("golem") && !isBoss(entity);
			case "orc" -> name.contains("orc") && !name.contains("shadow");
			case "ant" -> name.contains("ant") && !name.contains("beru");
			default -> false;
		};
	}

	private static boolean isBoss(Entity entity) {
		String name = entity.getClass().getSimpleName();
		return name.equals("GoblinKingEntity") || name.equals("SpiderBossEntity") || name.equals("FangedKasakaEntity") || name.equals("IgrisEntity") || name.equals("BloodRedComIgrisEntity")
				|| name.equals("BarukaEntity") || name.equals("KargalganEntity") || name.equals("BeruBossEntity") || name.equals("AncientGolemEntity") || name.equals("GemGolemEntity")
				|| name.equals("FuturisticGolemEntity") || name.equals("KamishEntity") || name.equals("VulcanEntity") || name.equals("CerberusEntity") || name.equals("BaranEntity")
				|| name.equals("KaiselinEntity") || name.equals("KaiselEntity");
	}

	private record QuestDefinition(String id, String kind, String family, String title, String objective, int target, int timeLimitSeconds, int xpReward) {
	}

	private record AttackPair(UUID attacker, UUID victim) {
	}

	private record HitWindow(long firstTick, long lastTick, int hits) {
	}
}
