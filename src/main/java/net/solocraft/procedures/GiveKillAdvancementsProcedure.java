// Tag-Based Boss Advancement System - Uses entity tags for automatic detection
package net.solocraft.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.Advancement;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

@Mod.EventBusSubscriber
public class GiveKillAdvancementsProcedure {
	// Your boss tag
	private static final TagKey<net.minecraft.world.entity.EntityType<?>> SOLO_BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("sololeveling", "soloboss"));
	// Simple mapping: entity registry name -> advancement ID
	private static final Map<String, String> BOSS_TO_ADVANCEMENT = new HashMap<>();
	static {
		// Just add the mappings here - when you add a new boss to the tag, add the advancement here
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
		// Add more as needed - this is the ONLY place you need to add new bosses
	}
	// Configuration
	private static final double PROXIMITY_RANGE = 50.0; // blocks

	@SubscribeEvent
	public static void onBossDeath(LivingDeathEvent event) {
		if (event.getEntity() == null)
			return;
		Entity deadEntity = event.getEntity();
		// Check if the dead entity is in the soloboss tag
		if (!isSoloBoss(deadEntity))
			return;
		// Get the entity's registry name
		ResourceLocation entityLocation = ForgeRegistries.ENTITY_TYPES.getKey(deadEntity.getType());
		if (entityLocation == null)
			return;
		String entityName = entityLocation.toString();
		// Get the corresponding advancement
		String advancementId = BOSS_TO_ADVANCEMENT.get(entityName);
		if (advancementId == null) {
			System.out.println("[SoloLeveling] No advancement configured for boss: " + entityName);
			return;
		}
		// Give advancement to all nearby players
		giveAdvancementToNearbyPlayers(deadEntity, advancementId);
	}

	private static boolean isSoloBoss(Entity entity) {
		// Check if entity type is in the soloboss tag
		return entity.getType().is(SOLO_BOSS_TAG);
	}

	private static void giveAdvancementToNearbyPlayers(Entity boss, String advancementId) {
		if (!(boss.level() instanceof Level))
			return;
		Level level = (Level) boss.level();
		if (level.getServer() == null)
			return;
		// Get the advancement
		Advancement advancement = level.getServer().getAdvancements().getAdvancement(new ResourceLocation(advancementId));
		if (advancement == null) {
			System.err.println("[SoloLeveling] Advancement not found: " + advancementId);
			return;
		}
		// Find all players within range
		List<ServerPlayer> nearbyPlayers = new ArrayList<>();
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (player.level() == level && player.distanceTo(boss) <= PROXIMITY_RANGE) {
				nearbyPlayers.add(player);
			}
		}
		if (nearbyPlayers.isEmpty())
			return;
		// Give advancement to all nearby players
		for (ServerPlayer player : nearbyPlayers) {
			giveAdvancementToPlayer(player, advancement, boss);
		}
		// Send group message
		String bossName = boss.getDisplayName().getString();
		Component message = Component.literal("§6" + bossName + " defeated! " + nearbyPlayers.size() + " player(s) earned the achievement!");
		for (ServerPlayer player : nearbyPlayers) {
			player.displayClientMessage(message, false);
		}
	}

	private static void giveAdvancementToPlayer(ServerPlayer player, Advancement advancement, Entity boss) {
		// Check if player already has the advancement
		if (player.getAdvancements().getOrStartProgress(advancement).isDone()) {
			return;
		}
		// Award all criteria for the advancement
		for (String criterionName : advancement.getCriteria().keySet()) {
			player.getAdvancements().award(advancement, criterionName);
		}
		// Optional: Send individual message
		player.displayClientMessage(Component.literal("§aAchievement unlocked: " + advancement.getDisplay().getTitle().getString()), true);
	}

	// ============ UTILITY METHODS ============
	// Method to add new boss-advancement mappings at runtime (optional)
	public static void addBossAdvancement(String entityName, String advancementId) {
		BOSS_TO_ADVANCEMENT.put(entityName, advancementId);
	}

	// Method to get all configured bosses
	public static Set<String> getConfiguredBosses() {
		return BOSS_TO_ADVANCEMENT.keySet();
	}

	// Method to check if a boss has an advancement configured
	public static boolean hasAdvancementConfigured(String entityName) {
		return BOSS_TO_ADVANCEMENT.containsKey(entityName);
	}

	// Debug method to list all entities in the soloboss tag
	public static void debugListSoloBosses(Level level) {
		System.out.println("[SoloLeveling] Entities in soloboss tag:");
		for (net.minecraft.world.entity.EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
			if (entityType.is(SOLO_BOSS_TAG)) {
				ResourceLocation location = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
				String advancementId = BOSS_TO_ADVANCEMENT.get(location.toString());
				System.out.println("  - " + location + " -> " + (advancementId != null ? advancementId : "NO ADVANCEMENT"));
			}
		}
	}
}
