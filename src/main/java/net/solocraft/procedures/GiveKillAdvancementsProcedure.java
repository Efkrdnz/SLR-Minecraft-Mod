package net.solocraft.procedures;

import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Awards public boss-history advancements without adding duplicate chat or
 * action-bar narration. Boss completion feedback is owned by the relevant
 * dungeon flow and is delivered through the System popup when available.
 */
@Mod.EventBusSubscriber
public final class GiveKillAdvancementsProcedure {
	private static final TagKey<EntityType<?>> SOLO_BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("sololeveling", "soloboss"));
	private static final Map<String, String> BOSS_TO_ADVANCEMENT = new HashMap<>();
	private static final double PROXIMITY_RANGE = 50.0D;

	static {
		BOSS_TO_ADVANCEMENT.put("sololeveling:fanged_kasaka", "sololeveling:kasakas_domain");
		BOSS_TO_ADVANCEMENT.put("sololeveling:igris", "sololeveling:blood_red_commander_igris");
		BOSS_TO_ADVANCEMENT.put("sololeveling:beru_boss", "sololeveling:ant_king");
		BOSS_TO_ADVANCEMENT.put("sololeveling:gem_golem", "sololeveling:gem_golem_adv");
		BOSS_TO_ADVANCEMENT.put("sololeveling:kamish", "sololeveling:kamish_adv");
		BOSS_TO_ADVANCEMENT.put("sololeveling:goblin_king", "sololeveling:goblin_king_adv");
		BOSS_TO_ADVANCEMENT.put("sololeveling:spider_boss", "sololeveling:giant_spider");
		BOSS_TO_ADVANCEMENT.put("sololeveling:ancient_golem", "sololeveling:ancient_golem_adv");
		BOSS_TO_ADVANCEMENT.put("sololeveling:skeleton_summoner", "sololeveling:skeleton_summoner_adv");
		BOSS_TO_ADVANCEMENT.put("sololeveling:kargalgan", "sololeveling:kargalgan_adv");
		BOSS_TO_ADVANCEMENT.put("sololeveling:baruka", "sololeveling:baruka_adv");
		BOSS_TO_ADVANCEMENT.put("sololeveling:futuristic_golem", "sololeveling:futuristic_golem_adv");
	}

	private GiveKillAdvancementsProcedure() {
	}

	@SubscribeEvent
	public static void onBossDeath(LivingDeathEvent event) {
		Entity boss = event.getEntity();
		if (boss.getPersistentData().getBoolean(DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG)
				|| !boss.getType().is(SOLO_BOSS_TAG))
			return;
		ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());
		String advancementId = entityId == null ? null : BOSS_TO_ADVANCEMENT.get(entityId.toString());
		if (advancementId == null)
			return;
		giveAdvancementToNearbyPlayers(boss, advancementId);
	}

	private static void giveAdvancementToNearbyPlayers(Entity boss, String advancementId) {
		Level level = boss.level();
		if (level.getServer() == null)
			return;
		Advancement advancement = level.getServer().getAdvancements()
				.getAdvancement(new ResourceLocation(advancementId));
		if (advancement == null)
			return;
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (player.level() != level || player.distanceTo(boss) > PROXIMITY_RANGE)
				continue;
			giveAdvancementToPlayer(player, advancement);
		}
	}

	private static void giveAdvancementToPlayer(ServerPlayer player, Advancement advancement) {
		if (player.getAdvancements().getOrStartProgress(advancement).isDone())
			return;
		for (String criterionName : advancement.getCriteria().keySet())
			player.getAdvancements().award(advancement, criterionName);
	}

	public static void addBossAdvancement(String entityName, String advancementId) {
		BOSS_TO_ADVANCEMENT.put(entityName, advancementId);
	}

	public static Set<String> getConfiguredBosses() {
		return BOSS_TO_ADVANCEMENT.keySet();
	}

	public static boolean hasAdvancementConfigured(String entityName) {
		return BOSS_TO_ADVANCEMENT.containsKey(entityName);
	}

	public static void debugListSoloBosses(Level level) {
		System.out.println("[SoloLeveling] Entities in soloboss tag:");
		for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
			if (!entityType.is(SOLO_BOSS_TAG))
				continue;
			ResourceLocation location = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
			String advancementId = location == null ? null : BOSS_TO_ADVANCEMENT.get(location.toString());
			System.out.println("  - " + location + " -> "
					+ (advancementId == null ? "NO ADVANCEMENT" : advancementId));
		}
	}
}
