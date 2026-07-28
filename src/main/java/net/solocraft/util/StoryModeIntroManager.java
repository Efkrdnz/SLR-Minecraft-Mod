package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.AncientGolemEntity;
import net.solocraft.entity.CartenonGateEntity;
import net.solocraft.entity.HunterEntity;
import net.solocraft.entity.LiuSwordVfxEntity;
import net.solocraft.entity.StatueOfGodEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Server-authoritative Story Mode prologue.
 *
 * <p>The normal Cartenon flow calls the public hooks in this class. Keeping those
 * hooks here avoids special-case state in the gate entity or the temple builder.</p>
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class StoryModeIntroManager {
	public static final int ASSASSIN_CLASS_ID = 1;
	public static final int FIGHTER_CLASS_ID = 3;
	public static final int E_RANK_ID = 1;
	public static final int TEAM_SIZE = 6;

	public static final ResourceKey<Level> ANCIENT_GOLEM_DIMENSION = ResourceKey.create(
			Registries.DIMENSION, new ResourceLocation(SololevelingMod.MODID, "dungeon_dimension_c"));
	public static final BlockPos ANCIENT_GOLEM_ORIGIN = new BlockPos(0, 64, 0);
	public static final Vec3 ANCIENT_GOLEM_ENTRY = new Vec3(10.5D, 2.0D, 25.5D);

	private static final ResourceLocation ANCIENT_GOLEM_STRUCTURE =
			new ResourceLocation(SololevelingMod.MODID, "dungeon_ancientgolem");
	private static final ResourceLocation AWAKENED_ADVANCEMENT =
			new ResourceLocation(SololevelingMod.MODID, "awakened");
	private static final String OWNER_MARKER = "slr_story_intro_owner";
	private static final String OWNER_UUID = "slr_story_intro_owner_uuid";
	private static final String BOSS_MARKER = "slr_story_intro_boss";
	private static final String HUNTER_MARKER = "slr_story_intro_hunter";
	private static final String GATE_MARKER = "slr_story_intro_gate";
	private static final String GOD_MARKER = "slr_story_intro_god";
	private static final String STORY_STATUE_MARKER = "slr_story_intro_statue";
	private static final String STORY_LASER_DONE = "slr_story_intro_laser_done";
	private static final String STORY_ACTIVATION_AT = "slr_story_intro_activation_at";
	private static final String TEMPLE_FOLLOW_MARKER =
			"slr_story_intro_follow_owner";
	private static final String PROFILE_INDEX = "slr_story_intro_profile";
	private static final String INSTANCE_ID = "slr_story_intro_instance";
	private static final String DUNGEON_TAG_KEY = "dungeon_tag";
	private static final String STORY_DUNGEON_TAG = "story_intro_ancient_golem";
	private static final int TEMPLE_INSTANCE_SPACING = 512;
	private static final int TEMPLE_INSTANCE_COLUMNS = 32;
	private static final int TEMPLE_FLOOR_Y = 64;
	private static final int LASER_STAGGER_TICKS = 14;
	private static final int LASER_KILL_DELAY_TICKS = 10;
	private static final int LASER_TARGET_RECOVERY_TICKS = 40;
	private static final int STATUE_ACTIVATION_DELAY_TICKS = 5;
	private static final int BOSS_ENTITY_LOAD_GRACE_TICKS = 60;
	private static final int STATUE_SEQUENCE_MANAGER_INTERVAL_TICKS = 10;
	private static final int STORY_GOD_DUPLICATE_AUDIT_INTERVAL_TICKS = 100;

	private static final Vec3[] TEAM_ENTRY_OFFSETS = {
			new Vec3(-4.0D, 0.0D, -3.0D),
			new Vec3(-2.0D, 0.0D, -5.0D),
			new Vec3(0.0D, 0.0D, -4.0D),
			new Vec3(2.0D, 0.0D, -5.0D),
			new Vec3(4.0D, 0.0D, -3.0D),
			new Vec3(0.0D, 0.0D, 3.0D)
	};
	private static final Vec3[] GATE_FORMATION = {
			new Vec3(-4.5D, 0.0D, -2.5D),
			new Vec3(-1.5D, 0.0D, -4.0D),
			new Vec3(1.5D, 0.0D, -4.0D),
			new Vec3(4.5D, 0.0D, -2.5D),
			new Vec3(-2.5D, 0.0D, 3.0D),
			new Vec3(2.5D, 0.0D, 3.0D)
	};
	private static final Vec3[] TEMPLE_FORMATION = {
			new Vec3(-5.0D, 1.0D, 10.0D),
			new Vec3(-3.0D, 1.0D, 12.0D),
			new Vec3(-1.0D, 1.0D, 10.0D),
			new Vec3(1.0D, 1.0D, 12.0D),
			new Vec3(3.0D, 1.0D, 10.0D),
			new Vec3(5.0D, 1.0D, 12.0D)
	};

	private static final List<HunterProfile> HUNTER_PROFILES = List.of(
			new HunterProfile("C", "Fighter", 6.0D, 44.0D, 12.0D, 0.38D,
					"sololeveling:c_tier_sword", "minecraft:shield", 2, 5, 2, 1, 3, 1, 2, 1),
			new HunterProfile("D", "Assassin", 4.5D, 34.0D, 7.0D, 0.43D,
					"sololeveling:dagger_knight_d", "sololeveling:dagger_karambit_e", 4, 8, 3, 2, 5, 2, 4, 2),
			new HunterProfile("C", "Tanker", 4.5D, 58.0D, 18.0D, 0.33D,
					"sololeveling:war_axe", "minecraft:shield", 1, 11, 4, 3, 2, 1, 6, 1),
			new HunterProfile("D", "Fighter", 4.0D, 38.0D, 9.0D, 0.35D,
					"sololeveling:d_tier_sword", "minecraft:shield", 3, 3, 1, 2, 7, 2, 1, 2),
			new HunterProfile("C", "Assassin", 6.0D, 42.0D, 10.0D, 0.45D,
					"sololeveling:dagger_chain_c", "sololeveling:dagger_knight_d", 6, 13, 5, 4, 4, 1, 7, 1),
			new HunterProfile("B", "Healer", 2.0D, 56.0D, 15.0D, 0.33D,
					"minecraft:air", "sololeveling:storm_griamore", 5, 9, 2, 3, 6, 2, 3, 2)
	);

	private StoryModeIntroManager() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer)
			return;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(player.server);
		if (data.stage() == StoryModeIntroSavedData.Stage.NOT_STARTED
				&& player.server.overworld().getGameRules().getBoolean(
						SololevelingModGameRules.SOLO_LEVELING_STORY_MODE)) {
			int classId = chooseStartingClass(player);
			if (data.claimOwner(player.getUUID(), classId,
					player.server.overworld().getGameTime())) {
				initializePlayer(player, classId);
				player.sendSystemMessage(Component.literal(
						"Story Mode: you have entered the Ancient Golem dungeon as an E-rank hunter."));
			}
		}
		if (!data.isOwner(player.getUUID()) || !data.isActive())
			return;
		if (data.cartenonInstanceId() > 0
				&& CartenonProgressSavedData.get(player.serverLevel())
						.isResolved(player.getUUID())) {
			onAwakeningResolved(player,
					CartenonProgressSavedData.get(player.serverLevel())
							.accepted(player.getUUID()));
			return;
		}
		if (data.stage() == StoryModeIntroSavedData.Stage.PREPARING)
			initializePlayer(player, data.playerClassId());
		else
			markOwner(player);
		if (data.stage() == StoryModeIntroSavedData.Stage.PREPARING)
			prepareAncientGolemIntro(player, data);
		else
			resumeOwner(player, data);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(player.server);
		if (!data.isOwner(player.getUUID()) || !data.isActive())
			return;
		markOwner(player);
		player.server.execute(() -> resumeOwner(player, data));
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
			return;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(player.server);
		if (!data.isOwner(player.getUUID()) || !data.isActive())
			return;
		if (data.cartenonInstanceId() > 0
				&& data.stage().ordinal()
						>= StoryModeIntroSavedData.Stage.TEMPLE.ordinal()
				&& data.stage().ordinal()
						<= StoryModeIntroSavedData.Stage.PLAYER_HUNT.ordinal()
				&& player.tickCount % 20 == 0)
			releaseFrozenTempleHunters(player.server, data);
		switch (data.stage()) {
			case PREPARING -> {
				if (player.tickCount % 20 == 0)
					prepareAncientGolemIntro(player, data);
			}
			case ANCIENT_GOLEM -> {
				if (player.tickCount % 20 == 0)
					recoverAncientGolemStage(player, data);
			}
			case GATE_WAIT -> {
				if (player.tickCount % 20 == 0)
					recoverGateWaitStage(player, data);
			}
			case TEMPLE -> {
				if (player.tickCount % 20 == 0)
					ensureTempleParty(player, data);
				StatueOfGodEntity god = resolveStoryGod(player, data);
				if (god != null && isAtStoryTempleCenter(player))
					beginLaserExecution(player, god, data);
			}
			case LASER_EXECUTION -> tickLaserExecution(player, data);
			case WAITING_FOR_SNEAK -> tickWaitingForSneak(player, data);
			case STATUE_WAKING, STATUE_HUNT, PLAYER_HUNT -> {
				if (player.tickCount
						% STATUE_SEQUENCE_MANAGER_INTERVAL_TICKS == 0)
					tickStatueSequence(player, data);
			}
			default -> {
			}
		}
	}

	/** True only for this world's active prologue owner and tagged Ancient Golem. */
	public static boolean isStoryBoss(ServerPlayer owner, Entity boss) {
		if (owner == null || boss == null || !(boss.level() instanceof ServerLevel))
			return false;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(owner.server);
		if (!data.isActive() || !data.isOwner(owner.getUUID()))
			return false;
		if (data.stage() != StoryModeIntroSavedData.Stage.ANCIENT_GOLEM)
			return false;
		boolean uuidMatches = data.bossId() != null && data.bossId().equals(boss.getUUID());
		return uuidMatches || boss.getTags().contains(BOSS_MARKER)
				&& hasOwner(boss, owner.getUUID());
	}

	/** True when a duplicate death callback references the already-cleared Story boss. */
	public static boolean isHandledStoryBoss(ServerPlayer owner, Entity boss) {
		if (owner == null || boss == null || !(boss.level() instanceof ServerLevel))
			return false;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(owner.server);
		if (!data.isActive() || !data.isOwner(owner.getUUID())
				|| data.stage().ordinal()
						<= StoryModeIntroSavedData.Stage.ANCIENT_GOLEM.ordinal())
			return false;
		boolean uuidMatches = data.bossId() != null
				&& data.bossId().equals(boss.getUUID());
		return uuidMatches || boss.getTags().contains(BOSS_MARKER)
				&& hasOwner(boss, owner.getUUID());
	}

	/** Resolves the owner even when a tamed Hunter landed the boss's final hit. */
	@Nullable
	public static ServerPlayer storyOwnerForBoss(Entity boss) {
		if (boss == null || !(boss.level() instanceof ServerLevel level))
			return null;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(level);
		if (!data.isActive() || data.ownerId() == null)
			return null;
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(data.ownerId());
		return owner != null && isStoryBoss(owner, boss) ? owner : null;
	}

	/**
	 * Called after the story Cartenon gate has its position and instance id.
	 * The surviving party is moved into a stable waiting formation.
	 */
	public static void onCartenonGateCreated(ServerPlayer owner, CartenonGateEntity gate,
			int instanceId) {
		if (owner == null || gate == null)
			return;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(owner.server);
		if (!data.isActive() || !data.isOwner(owner.getUUID()))
			return;
		boolean firstGate = data.stage()
				== StoryModeIntroSavedData.Stage.ANCIENT_GOLEM;
		boolean sameInstanceRecovery = data.stage()
				== StoryModeIntroSavedData.Stage.GATE_WAIT
				&& data.cartenonInstanceId() == Math.max(1, instanceId);
		if (!firstGate && !sameInstanceRecovery)
			return;
		tagOwned(gate, GATE_MARKER, owner.getUUID());
		gate.getPersistentData().putInt(INSTANCE_ID, Math.max(1, instanceId));
		data.setGate(gate.getUUID(), Math.max(1, instanceId),
				owner.server.overworld().getGameTime());
		List<HunterEntity> hunters = findStoryHunters(owner.server, data);
		for (int index = 0; index < hunters.size(); index++)
			freezeAt(hunters.get(index), gate.position().add(
					GATE_FORMATION[index % GATE_FORMATION.length]));
	}

	/**
	 * Called immediately after the owner is teleported by Cartenon. Hunters use
	 * vanilla dimension cloning through a no-portal teleporter, which preserves
	 * their complete entity NBT and UUID.
	 */
	public static void onPlayerEnteredTemple(ServerPlayer owner, ServerLevel templeLevel,
			int instanceId) {
		if (owner == null || templeLevel == null)
			return;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(owner.server);
		if (!data.isActive() || !data.isOwner(owner.getUUID()))
			return;
		if (data.stage() != StoryModeIntroSavedData.Stage.GATE_WAIT
				|| data.cartenonInstanceId() != Math.max(1, instanceId))
			return;
		BlockPos origin = cartenonInstanceOrigin(instanceId);
		forceChunk(templeLevel, origin.relative(Direction.SOUTH, 145));
		ServerLevel sourceDungeon = owner.server.getLevel(ANCIENT_GOLEM_DIMENSION);
		if (sourceDungeon != null)
			loadDungeonChunks(sourceDungeon);
		List<UUID> movedIds = new ArrayList<>(data.hunterIds());
		List<HunterEntity> hunters = findStoryHunters(owner.server, data);
		for (int index = 0; index < hunters.size(); index++) {
			UUID originalId = hunters.get(index).getUUID();
			Vec3 destination = Vec3.atLowerCornerOf(origin).add(
					TEMPLE_FORMATION[index % TEMPLE_FORMATION.length]);
			HunterEntity moved = moveHunter(hunters.get(index), templeLevel, destination);
			if (moved != null) {
				moved.getPersistentData().putInt(INSTANCE_ID, Math.max(1, instanceId));
				enableTempleFollowing(moved);
				int storedIndex = movedIds.indexOf(originalId);
				if (storedIndex >= 0)
					movedIds.set(storedIndex, moved.getUUID());
				else
					movedIds.add(moved.getUUID());
			}
		}
		data.replaceHunterIds(movedIds);
		StatueOfGodEntity god = findAndMarkGodStatue(owner, templeLevel, instanceId);
		data.setTemple(Math.max(1, instanceId), god == null ? null : god.getUUID(),
				owner.server.overworld().getGameTime());
		if (god != null)
			data.trackGodStatue(god.getUUID(), god.blockPosition());
		owner.getPersistentData().putInt(INSTANCE_ID, Math.max(1, instanceId));
		ensureTempleParty(owner, data);
	}

	public static void onAwakeningResolved(ServerPlayer player, boolean accepted) {
		if (player == null)
			return;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(player.server);
		if (!data.isActive() || !data.isOwner(player.getUUID()))
			return;
		if (data.stage() != StoryModeIntroSavedData.Stage.PLAYER_HUNT
				|| hasLivingStoryHunter(player.server, data))
			return;
		if (accepted)
			grantAwakenedAdvancement(player);
		cleanActiveMarkers(player.server, data);
		player.getPersistentData().remove(OWNER_MARKER);
		player.getPersistentData().remove(OWNER_UUID);
		player.getPersistentData().remove(INSTANCE_ID);
		if (STORY_DUNGEON_TAG.equals(
				player.getPersistentData().getString(DUNGEON_TAG_KEY)))
			player.getPersistentData().remove(DUNGEON_TAG_KEY);
		data.setStage(StoryModeIntroSavedData.Stage.COMPLETE,
				player.server.overworld().getGameTime());
	}

	private static void grantAwakenedAdvancement(ServerPlayer player) {
		Advancement advancement = player.server.getAdvancements()
				.getAdvancement(AWAKENED_ADVANCEMENT);
		if (advancement == null)
			return;
		AdvancementProgress progress = player.getAdvancements()
				.getOrStartProgress(advancement);
		for (String criterion : progress.getRemainingCriteria())
			player.getAdvancements().award(advancement, criterion);
	}

	public static boolean isIntroActive(Entity entity) {
		if (entity == null || !(entity.level() instanceof ServerLevel level))
			return false;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(level);
		if (!data.isActive() || data.ownerId() == null)
			return false;
		if (entity instanceof ServerPlayer player)
			return data.isOwner(player.getUUID());
		return hasOwner(entity, data.ownerId())
				&& (entity.getTags().contains(BOSS_MARKER)
				|| entity.getTags().contains(HUNTER_MARKER)
				|| entity.getTags().contains(GATE_MARKER)
				|| entity.getTags().contains(GOD_MARKER));
	}

	public static boolean isStoryOwner(ServerPlayer player) {
		if (player == null)
			return false;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(player.server);
		return data.isActive() && data.isOwner(player.getUUID());
	}

	/**
	 * Normal Cartenon runs may awaken on any lethal source. The Story prologue
	 * only reaches that choice after its God has finished the party hunt and
	 * personally lands the lethal blow on the owner.
	 */
	public static boolean canTriggerAwakening(ServerPlayer player,
			DamageSource source) {
		if (player == null)
			return true;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(player.server);
		if (!data.isActive() || !data.isOwner(player.getUUID()))
			return true;
		if (!canResolveAwakening(player))
			return false;
		Entity attacker = source == null ? null : source.getEntity();
		return attacker instanceof StatueOfGodEntity god
				&& god.getPersistentData().getBoolean(STORY_STATUE_MARKER)
				&& god.getPersistentData().getInt(INSTANCE_ID)
						== data.cartenonInstanceId()
				&& hasOwner(god, player.getUUID());
	}

	public static boolean canResolveAwakening(ServerPlayer player) {
		if (player == null)
			return true;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(player.server);
		return !data.isActive() || !data.isOwner(player.getUUID())
				|| data.stage() == StoryModeIntroSavedData.Stage.PLAYER_HUNT
						&& !hasLivingStoryHunter(player.server, data);
	}

	public static boolean shouldSuppressClassSelection(ServerPlayer player) {
		if (player == null)
			return false;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(player.server);
		if (data.isActive() && data.isOwner(player.getUUID()))
			return true;
		return data.stage() == StoryModeIntroSavedData.Stage.NOT_STARTED
				&& player.server.overworld().getGameRules().getBoolean(
						SololevelingModGameRules.SOLO_LEVELING_STORY_MODE);
	}

	public static BlockPos cartenonInstanceOrigin(int instanceId) {
		int zeroBased = Math.max(0, instanceId - 1);
		int column = zeroBased % TEMPLE_INSTANCE_COLUMNS;
		int row = zeroBased / TEMPLE_INSTANCE_COLUMNS;
		return new BlockPos(column * TEMPLE_INSTANCE_SPACING, TEMPLE_FLOOR_Y,
				row * TEMPLE_INSTANCE_SPACING);
	}

	public static BlockPos cartenonTempleCenter(int instanceId) {
		return cartenonInstanceOrigin(instanceId).relative(Direction.SOUTH, 77).above();
	}

	public static boolean isAtStoryTempleCenter(Entity entity) {
		if (entity == null || !(entity.level() instanceof ServerLevel level)
				|| level.dimension() != CartenonTempleManager.CARTENON_DIMENSION)
			return false;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(level);
		if (!data.isActive() || data.cartenonInstanceId() <= 0)
			return false;
		Vec3 center = Vec3.atCenterOf(cartenonTempleCenter(data.cartenonInstanceId()));
		double dx = entity.getX() - center.x;
		double dz = entity.getZ() - center.z;
		return dx * dx + dz * dz <= 14.0D * 14.0D;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void protectStoryHunters(LivingAttackEvent event) {
		if (!(event.getEntity().level() instanceof ServerLevel level))
			return;
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(level);
		if (!data.isActive() || data.ownerId() == null)
			return;
		Entity attacker = event.getSource().getEntity();
		if (event.getEntity() instanceof HunterEntity hunter
				&& hunter.getTags().contains(HUNTER_MARKER)) {
			boolean scriptedStatueAttack = attacker instanceof StatueOfGodEntity god
					&& god.getPersistentData().getBoolean(STORY_STATUE_MARKER)
					&& hasOwner(god, data.ownerId())
					&& data.stage().ordinal()
							>= StoryModeIntroSavedData.Stage.LASER_EXECUTION.ordinal();
			// The party must survive the Ancient Golem and temple setup intact.
			// Once the scripted execution begins, only its Statue of God may hurt
			// these exact Hunters.
			if (!scriptedStatueAttack)
				event.setCanceled(true);
			return;
		}
		if (event.getEntity() instanceof ServerPlayer owner
				&& data.isOwner(owner.getUUID())
				&& attacker instanceof StatueOfGodEntity god
				&& god.getPersistentData().getBoolean(STORY_STATUE_MARKER)
				&& hasLivingStoryHunter(owner.server, data)) {
			// The statue is never allowed to skip the scripted party hunt.
			event.setCanceled(true);
		}
	}

	private static void beginLaserExecution(ServerPlayer owner, StatueOfGodEntity god,
			StoryModeIntroSavedData data) {
		ensureTempleParty(owner, data);
		List<HunterEntity> candidates = livingStoryHunters(owner.server, data).stream()
				.filter(hunter -> hunter.level() == god.level())
				.toList();
		if (candidates.isEmpty())
			return;

		int targetCount = Math.min(candidates.size(),
				1 + Math.floorMod(owner.getUUID().hashCode(), 2));
		int startIndex = Math.floorMod(owner.getUUID().hashCode()
				^ data.cartenonInstanceId(), candidates.size());
		List<UUID> targetIds = new ArrayList<>(targetCount);
		for (int offset = 0; offset < targetCount; offset++)
			targetIds.add(candidates.get((startIndex + offset) % candidates.size())
					.getUUID());

		god.getPersistentData().putBoolean(STORY_LASER_DONE, false);
		god.getPersistentData().remove(STORY_ACTIVATION_AT);
		god.getEntityData().set(StatueOfGodEntity.SHOOT, true);
		data.beginLasers(targetIds, owner.server.overworld().getGameTime());
		tickLaserExecution(owner, data);
	}

	private static void tickLaserExecution(ServerPlayer owner,
			StoryModeIntroSavedData data) {
		StatueOfGodEntity god = resolveStoryGod(owner, data);
		if (god == null || !(god.level() instanceof ServerLevel temple))
			return;
		loadTempleActorChunks(temple, data.cartenonInstanceId());
		List<UUID> targets = data.laserTargetIds();
		long gameTick = owner.server.overworld().getGameTime();
		long elapsed = Math.max(0L, gameTick - data.stageStartedTick());
		int pending = data.laserKilledCount();
		if (pending < targets.size()) {
			HunterEntity pendingTarget = findEntity(owner.server,
					targets.get(pending), HunterEntity.class);
			if (!isMatchingLaserTarget(pendingTarget, owner, data, temple)) {
				boolean missingOrDead = pendingTarget == null
						|| pendingTarget.isRemoved()
						|| !pendingTarget.isAlive();
				boolean beamPersisted = data.laserFiredCount() > pending;
				if (missingOrDead && beamPersisted) {
					// A validated beam was already persisted. If entity removal
					// reached disk first, reconcile the matching outcome instead
					// of killing an additional party member.
					data.setLaserKilledCount(pending + 1);
				} else if (beamPersisted
						? gameTick - data.laserLastFiredTick()
								>= LASER_TARGET_RECOVERY_TICKS
						: elapsed >= (long) pending * LASER_STAGGER_TICKS
								+ LASER_TARGET_RECOVERY_TICKS) {
					replaceMissingLaserTarget(owner, data, temple, pending);
					return;
				}
			}
		}

		int fired = data.laserFiredCount();
		if (fired < targets.size()
				&& elapsed >= (long) fired * LASER_STAGGER_TICKS) {
			HunterEntity target = findEntity(owner.server, targets.get(fired),
					HunterEntity.class);
			if (isMatchingLaserTarget(target, owner, data, temple)) {
				Vec3 eye = god.position().add(0.0D, 14.0D, 0.0D);
				Vec3 impact = target.position().add(0.0D,
						target.getBbHeight() * 0.58D, 0.0D);
				LiuSwordVfxEntity.spawnExecutionLink(temple, eye, impact,
						0xFF1010, 0x6E0000, 0.48F,
						LASER_KILL_DELAY_TICKS + 3);
				data.markLaserFired(fired + 1, gameTick);
			}
		}

		int killed = data.laserKilledCount();
		if (killed < targets.size()
				&& data.laserFiredCount() > killed
				&& gameTick - data.laserLastFiredTick()
						>= LASER_KILL_DELAY_TICKS) {
			HunterEntity target = findEntity(owner.server, targets.get(killed),
					HunterEntity.class);
			if (isMatchingLaserTarget(target, owner, data, temple)) {
				killByStatue(target, god);
				if (!target.isAlive())
					data.setLaserKilledCount(killed + 1);
			}
		}

		long finishedAt = targets.isEmpty() ? 0L
				: (long) (targets.size() - 1) * LASER_STAGGER_TICKS
						+ LASER_KILL_DELAY_TICKS + 1L;
		if (data.laserKilledCount() < targets.size() || elapsed < finishedAt)
			return;

		god.getEntityData().set(StatueOfGodEntity.SHOOT, false);
		god.getPersistentData().putBoolean(STORY_LASER_DONE, true);
		god.getPersistentData().putLong(STORY_ACTIVATION_AT,
				temple.getGameTime() + STATUE_ACTIVATION_DELAY_TICKS);
		data.setStage(StoryModeIntroSavedData.Stage.WAITING_FOR_SNEAK,
				owner.server.overworld().getGameTime());
	}

	private static boolean replaceMissingLaserTarget(ServerPlayer owner,
			StoryModeIntroSavedData data, ServerLevel temple, int targetIndex) {
		List<UUID> reservedTargets = data.laserTargetIds();
		for (HunterEntity candidate : livingStoryHunters(owner.server, data)) {
			if (candidate.level() != temple
					|| reservedTargets.contains(candidate.getUUID())
					|| !isMatchingStoryHunter(candidate, owner, data))
				continue;
			return data.replacePendingLaserTarget(targetIndex,
					candidate.getUUID());
		}
		return false;
	}

	private static boolean isMatchingStoryHunter(HunterEntity hunter,
			ServerPlayer owner, StoryModeIntroSavedData data) {
		return hunter != null && hunter.isAlive() && !hunter.isRemoved()
				&& hunter.getPersistentData().getBoolean(HUNTER_MARKER)
				&& hunter.getPersistentData().getInt(INSTANCE_ID)
						== data.cartenonInstanceId()
				&& hasOwner(hunter, owner.getUUID())
				&& data.hunterIds().contains(hunter.getUUID());
	}

	private static void tickWaitingForSneak(ServerPlayer owner,
			StoryModeIntroSavedData data) {
		StatueOfGodEntity god = resolveStoryGod(owner, data);
		if (god == null)
			return;
		String state = god.getPersistentData().getString("state");
		if ("waking".equals(state)) {
			data.setStage(StoryModeIntroSavedData.Stage.STATUE_WAKING,
					owner.server.overworld().getGameTime());
		} else if ("aggresive".equals(state)) {
			data.setStage(StoryModeIntroSavedData.Stage.STATUE_HUNT,
					owner.server.overworld().getGameTime());
		}
	}

	private static void tickStatueSequence(ServerPlayer owner,
			StoryModeIntroSavedData data) {
		StatueOfGodEntity god = resolveStoryGod(owner, data);
		if (god == null)
			return;
		String state = god.getPersistentData().getString("state");
		if (data.stage() == StoryModeIntroSavedData.Stage.STATUE_WAKING
				&& "aggresive".equals(state)) {
			data.setStage(StoryModeIntroSavedData.Stage.STATUE_HUNT,
					owner.server.overworld().getGameTime());
		}
		if (hasLivingStoryHunter(owner.server, data)) {
			if (data.stage() == StoryModeIntroSavedData.Stage.PLAYER_HUNT)
				data.setStage(StoryModeIntroSavedData.Stage.STATUE_HUNT,
						owner.server.overworld().getGameTime());
			return;
		}
		if (data.stage() != StoryModeIntroSavedData.Stage.PLAYER_HUNT)
			data.setStage(StoryModeIntroSavedData.Stage.PLAYER_HUNT,
					owner.server.overworld().getGameTime());
	}

	@Nullable
	private static StatueOfGodEntity resolveStoryGod(ServerPlayer owner,
			StoryModeIntroSavedData data) {
		ServerLevel temple = owner.server.getLevel(
				CartenonTempleManager.CARTENON_DIMENSION);
		if (temple == null || data.cartenonInstanceId() <= 0)
			return null;
		StatueOfGodEntity god = data.godStatueId() != null
				&& temple.getEntity(data.godStatueId())
						instanceof StatueOfGodEntity savedGod
								? savedGod : null;
		if (god != null && (god.level() != temple || god.isRemoved()
				|| !god.isAlive()))
			god = null;
		boolean recoveringGod = god == null;
		if (god == null && data.godStatuePosition() != null) {
			loadChunksAround(temple, data.godStatuePosition(), 1);
			god = data.godStatueId() != null
					&& temple.getEntity(data.godStatueId())
							instanceof StatueOfGodEntity loadedGod
									? loadedGod : null;
		}
		List<StatueOfGodEntity> ownedGods = List.of();
		if (god == null) {
			loadStoryGodRecoveryChunks(temple, owner, data);
			ownedGods = findLoadedStoryGods(temple, owner, data);
			god = chooseStoryGod(ownedGods, data);
		}
		if (god == null) {
			BlockPos expected = cartenonInstanceOrigin(
					data.cartenonInstanceId()).relative(Direction.SOUTH, 145);
			forceChunk(temple, expected);
			god = findAndMarkGodStatue(owner, temple,
					data.cartenonInstanceId());
		}
		if (god == null)
			return null;
		boolean auditDuplicates = recoveringGod
				|| owner.tickCount
						% STORY_GOD_DUPLICATE_AUDIT_INTERVAL_TICKS == 0;
		if (auditDuplicates && ownedGods.isEmpty())
			ownedGods = findLoadedStoryGods(temple, owner, data);
		if (auditDuplicates)
			retireDuplicateStoryGods(ownedGods, god);
		reconcileGodForStage(owner, god, data);
		if (recoveringGod || owner.tickCount % 20 == 0)
			data.trackGodStatue(god.getUUID(), god.blockPosition());
		return god;
	}

	private static void loadStoryGodRecoveryChunks(ServerLevel temple,
			ServerPlayer owner, StoryModeIntroSavedData data) {
		loadStoryGodCorridorChunks(temple, data.cartenonInstanceId());
		if (data.godStatuePosition() != null)
			loadChunksAround(temple, data.godStatuePosition(), 1);
		if (owner.serverLevel() == temple)
			loadChunksAround(temple, owner.blockPosition(), 1);
	}

	private static void loadStoryGodCorridorChunks(ServerLevel temple,
			int instanceId) {
		BlockPos origin = cartenonInstanceOrigin(instanceId);
		int minChunkX = (origin.getX() - 24) >> 4;
		int maxChunkX = (origin.getX() + 24) >> 4;
		int minChunkZ = (origin.getZ() - 8) >> 4;
		int maxChunkZ = (origin.getZ() + 170) >> 4;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
				temple.getChunk(chunkX, chunkZ);
		}
	}

	private static void loadChunksAround(ServerLevel level, BlockPos center,
			int chunkRadius) {
		int centerChunkX = center.getX() >> 4;
		int centerChunkZ = center.getZ() >> 4;
		for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius; dz++)
				level.getChunk(centerChunkX + dx, centerChunkZ + dz);
		}
	}

	private static List<StatueOfGodEntity> findLoadedStoryGods(
			ServerLevel temple, ServerPlayer owner,
			StoryModeIntroSavedData data) {
		Map<UUID, StatueOfGodEntity> found = new LinkedHashMap<>();
		BlockPos origin = cartenonInstanceOrigin(data.cartenonInstanceId());
		collectStoryGods(temple, owner, data,
				new AABB(origin.offset(-24, -8, -8),
						origin.offset(24, 48, 170)), found);
		if (data.godStatuePosition() != null)
			collectStoryGods(temple, owner, data,
					new AABB(data.godStatuePosition()).inflate(48.0D),
					found);
		if (owner.serverLevel() == temple)
			collectStoryGods(temple, owner, data,
					owner.getBoundingBox().inflate(48.0D), found);
		return new ArrayList<>(found.values());
	}

	private static void collectStoryGods(ServerLevel temple,
			ServerPlayer owner, StoryModeIntroSavedData data, AABB bounds,
			Map<UUID, StatueOfGodEntity> found) {
		for (StatueOfGodEntity candidate : temple.getEntitiesOfClass(
				StatueOfGodEntity.class, bounds, Entity::isAlive)) {
			boolean savedId = data.godStatueId() != null
					&& data.godStatueId().equals(candidate.getUUID());
			boolean matchingOwner = hasOwner(candidate, owner.getUUID())
					|| candidate.getPersistentData().hasUUID(OWNER_MARKER)
							&& owner.getUUID().equals(candidate.getPersistentData()
									.getUUID(OWNER_MARKER));
			boolean matchingInstance = candidate.getPersistentData()
					.getInt(INSTANCE_ID) == data.cartenonInstanceId();
			if (savedId || matchingOwner && matchingInstance
					&& (candidate.getTags().contains(GOD_MARKER)
							|| candidate.getPersistentData()
									.getBoolean(STORY_STATUE_MARKER)))
				found.putIfAbsent(candidate.getUUID(), candidate);
		}
	}

	@Nullable
	private static StatueOfGodEntity chooseStoryGod(
			List<StatueOfGodEntity> candidates,
			StoryModeIntroSavedData data) {
		if (candidates.isEmpty())
			return null;
		if (data.godStatueId() != null) {
			for (StatueOfGodEntity candidate : candidates) {
				if (data.godStatueId().equals(candidate.getUUID()))
					return candidate;
			}
		}
		Vec3 anchor = data.godStatuePosition() == null
				? Vec3.atCenterOf(cartenonInstanceOrigin(
						data.cartenonInstanceId()).relative(
								Direction.SOUTH, 145).above(6))
				: Vec3.atCenterOf(data.godStatuePosition());
		return candidates.stream().min(Comparator.comparingDouble(
				candidate -> candidate.distanceToSqr(anchor))).orElse(null);
	}

	private static void retireDuplicateStoryGods(
			List<StatueOfGodEntity> candidates, StatueOfGodEntity retained) {
		for (StatueOfGodEntity candidate : candidates) {
			if (candidate == retained)
				continue;
			candidate.getPersistentData().remove(STORY_STATUE_MARKER);
			candidate.getPersistentData().remove(OWNER_MARKER);
			candidate.getPersistentData().remove(STORY_LASER_DONE);
			candidate.getPersistentData().remove(STORY_ACTIVATION_AT);
			removeOwnedTags(candidate, GOD_MARKER);
			candidate.discard();
		}
	}

	private static void reconcileGodForStage(ServerPlayer owner,
			StatueOfGodEntity god, StoryModeIntroSavedData data) {
		tagOwned(god, GOD_MARKER, owner.getUUID());
		god.getPersistentData().putBoolean(STORY_STATUE_MARKER, true);
		god.getPersistentData().putUUID(OWNER_MARKER, owner.getUUID());
		god.getPersistentData().putInt(INSTANCE_ID,
				data.cartenonInstanceId());
		god.setPersistenceRequired();
		StoryModeIntroSavedData.Stage stage = data.stage();
		if (stage != StoryModeIntroSavedData.Stage.LASER_EXECUTION)
			god.getEntityData().set(StatueOfGodEntity.SHOOT, false);
		if (stage.ordinal()
				>= StoryModeIntroSavedData.Stage.WAITING_FOR_SNEAK.ordinal()) {
			god.getPersistentData().putBoolean(STORY_LASER_DONE, true);
			if (!god.getPersistentData().contains(STORY_ACTIVATION_AT))
				god.getPersistentData().putLong(STORY_ACTIVATION_AT,
						god.level().getGameTime());
		} else {
			god.getPersistentData().putBoolean(STORY_LASER_DONE, false);
			god.getPersistentData().remove(STORY_ACTIVATION_AT);
		}
		if (stage == StoryModeIntroSavedData.Stage.TEMPLE
				|| stage == StoryModeIntroSavedData.Stage.LASER_EXECUTION) {
			god.getPersistentData().putString("state", "throne");
			god.getPersistentData().putInt("IA", 0);
			god.getEntityData().set(StatueOfGodEntity.DATA_state, "throne");
			god.getEntityData().set(
					StatueOfGodEntity.DATA_story_upright, false);
			god.setNoAi(true);
			god.setTarget(null);
			god.getNavigation().stop();
		} else if (stage == StoryModeIntroSavedData.Stage.STATUE_WAKING
				&& !"waking".equals(
						god.getPersistentData().getString("state"))
				&& !"aggresive".equals(
						god.getPersistentData().getString("state"))) {
			god.getPersistentData().putString("state", "waking");
			god.getPersistentData().putInt("IA", 0);
			god.getEntityData().set(StatueOfGodEntity.DATA_state, "waking");
			god.getEntityData().set(
					StatueOfGodEntity.DATA_story_upright, false);
			god.setNoAi(true);
			god.setTarget(null);
			god.getNavigation().stop();
		} else if (stage == StoryModeIntroSavedData.Stage.STATUE_HUNT
				|| stage == StoryModeIntroSavedData.Stage.PLAYER_HUNT) {
			if (!"aggresive".equals(
					god.getPersistentData().getString("state"))) {
				god.getPersistentData().putString("state", "aggresive");
				god.getPersistentData().putInt("IA", 0);
				god.getEntityData().set(StatueOfGodEntity.DATA_state,
						"aggresive");
			}
			god.getEntityData().set(
					StatueOfGodEntity.DATA_story_upright, true);
			god.setNoAi(false);
		}
	}

	private static List<HunterEntity> livingStoryHunters(MinecraftServer server,
			StoryModeIntroSavedData data) {
		return findStoryHunters(server, data).stream()
				.filter(Entity::isAlive)
				.filter(entity -> entity.getPersistentData()
						.getBoolean(HUNTER_MARKER))
				.filter(entity -> data.ownerId() != null
						&& hasOwner(entity, data.ownerId()))
				.filter(entity -> data.cartenonInstanceId() <= 0
						|| entity.level().dimension()
								== CartenonTempleManager.CARTENON_DIMENSION
						&& entity.getPersistentData().getInt(INSTANCE_ID)
								== data.cartenonInstanceId())
				.toList();
	}

	private static boolean hasLivingStoryHunter(MinecraftServer server,
			StoryModeIntroSavedData data) {
		if (data.cartenonInstanceId() > 0) {
			ServerLevel temple = server.getLevel(
					CartenonTempleManager.CARTENON_DIMENSION);
			if (temple == null)
				return false;
			for (UUID hunterId : data.hunterIds()) {
				if (temple.getEntity(hunterId) instanceof HunterEntity hunter
						&& hunter.isAlive()
						&& hunter.getPersistentData().getBoolean(
								HUNTER_MARKER)
						&& hunter.getPersistentData().getInt(INSTANCE_ID)
								== data.cartenonInstanceId()
						&& data.ownerId() != null
						&& hasOwner(hunter, data.ownerId()))
					return true;
			}
			return false;
		}
		return !livingStoryHunters(server, data).isEmpty();
	}

	private static boolean isMatchingLaserTarget(@Nullable HunterEntity target,
			ServerPlayer owner, StoryModeIntroSavedData data, ServerLevel temple) {
		return target != null && target.isAlive() && target.level() == temple
				&& target.getPersistentData().getBoolean(HUNTER_MARKER)
				&& target.getPersistentData().getInt(INSTANCE_ID)
						== data.cartenonInstanceId()
				&& hasOwner(target, owner.getUUID())
				&& data.hunterIds().contains(target.getUUID());
	}

	private static void killByStatue(HunterEntity target, StatueOfGodEntity god) {
		var source = target.level().damageSources().mobAttack(god);
		target.invulnerableTime = 0;
		target.setInvulnerable(false);
		target.hurt(source, Math.max(10_000.0F, target.getMaxHealth() * 20.0F));
		if (!target.isAlive())
			return;
		target.setHealth(0.0F);
		target.die(source);
		if (target.isAlive())
			target.remove(Entity.RemovalReason.KILLED);
	}

	private static void recoverAncientGolemStage(ServerPlayer owner,
			StoryModeIntroSavedData data) {
		ServerLevel dungeon = owner.server.getLevel(ANCIENT_GOLEM_DIMENSION);
		if (dungeon == null)
			return;
		loadDungeonChunks(dungeon);
		tagStoryReturnPortals(dungeon);
		List<HunterEntity> hunters = ensureInitialTeam(owner, dungeon, data);
		CartenonGateEntity existingGate = findRecoverableStoryGate(
				owner, dungeon, data);
		if (existingGate != null && existingGate.getInstanceId() > 0) {
			AncientGolemEntity staleBoss = findAndMarkBoss(owner, dungeon);
			if (staleBoss != null)
				staleBoss.discard();
			onCartenonGateCreated(owner, existingGate,
					existingGate.getInstanceId());
			for (int index = 0; index < hunters.size(); index++)
				freezeAt(hunters.get(index), existingGate.position().add(
						GATE_FORMATION[index % GATE_FORMATION.length]));
			return;
		}
		AncientGolemEntity boss = findAndMarkBoss(owner, dungeon);
		if (boss == null && bossRecoveryGraceElapsed(owner, data))
			boss = spawnReplacementBoss(owner, dungeon);
		if (boss != null)
			data.setBossId(boss.getUUID());
	}

	private static void recoverGateWaitStage(ServerPlayer owner,
			StoryModeIntroSavedData data) {
		if (data.cartenonInstanceId() > 0
				&& owner.serverLevel().dimension()
						== CartenonTempleManager.CARTENON_DIMENSION) {
			onPlayerEnteredTemple(owner, owner.serverLevel(),
					data.cartenonInstanceId());
			return;
		}
		ServerLevel dungeon = owner.server.getLevel(ANCIENT_GOLEM_DIMENSION);
		if (dungeon == null)
			return;
		loadDungeonChunks(dungeon);
		tagStoryReturnPortals(dungeon);
		List<HunterEntity> hunters = ensureInitialTeam(owner, dungeon, data);
		CartenonGateEntity gate = findRecoverableStoryGate(
				owner, dungeon, data);
		if (gate == null)
			gate = spawnReplacementGate(owner, dungeon,
					data.cartenonInstanceId());
		if (gate == null)
			return;
		if (!gate.getTags().contains(GATE_MARKER)
				|| !hasOwner(gate, owner.getUUID())
				|| data.gateId() == null
				|| !data.gateId().equals(gate.getUUID())) {
			onCartenonGateCreated(owner, gate, data.cartenonInstanceId());
			return;
		}
		for (int index = 0; index < hunters.size(); index++)
			freezeAt(hunters.get(index), gate.position().add(
					GATE_FORMATION[index % GATE_FORMATION.length]));
	}

	@Nullable
	private static CartenonGateEntity findRecoverableStoryGate(
			ServerPlayer owner, ServerLevel dungeon,
			StoryModeIntroSavedData data) {
		if (data.gateId() != null
				&& dungeon.getEntity(data.gateId())
						instanceof CartenonGateEntity savedGate)
			return savedGate;
		return dungeon.getEntitiesOfClass(CartenonGateEntity.class,
				dungeonBounds().inflate(24.0D),
				entity -> entity.getInstanceId() > 0
						&& (entity.getTags().contains(GATE_MARKER)
								&& hasOwner(entity, owner.getUUID())
								|| entity.isAllowed(owner.getUUID()))).stream()
				.findFirst().orElse(null);
	}

	private static void ensureTempleParty(ServerPlayer owner,
			StoryModeIntroSavedData data) {
		if (data.cartenonInstanceId() <= 0)
			return;
		ServerLevel temple = owner.server.getLevel(
				CartenonTempleManager.CARTENON_DIMENSION);
		if (temple == null)
			return;
		loadTempleActorChunks(temple, data.cartenonInstanceId());
		ServerLevel dungeon = owner.server.getLevel(ANCIENT_GOLEM_DIMENSION);
		if (dungeon != null)
			loadDungeonChunks(dungeon);

		Map<Integer, HunterEntity> byProfile = new LinkedHashMap<>();
		for (HunterEntity hunter : findStoryHunters(owner.server, data))
			addHunterByProfile(byProfile, hunter, owner.getUUID());
		if (dungeon != null) {
			for (HunterEntity hunter : dungeon.getEntitiesOfClass(
					HunterEntity.class, dungeonBounds().inflate(16.0D),
					entity -> entity.isAlive()
							&& entity.getTags().contains(HUNTER_MARKER)
							&& hasOwner(entity, owner.getUUID())))
				addHunterByProfile(byProfile, hunter, owner.getUUID());
		}

		BlockPos origin = cartenonInstanceOrigin(data.cartenonInstanceId());
		List<UUID> movedIds = new ArrayList<>(TEAM_SIZE);
		List<HunterEntity> movedHunters = new ArrayList<>(TEAM_SIZE);
		for (int profileIndex = 0; profileIndex < TEAM_SIZE; profileIndex++) {
			Vec3 destination = Vec3.atLowerCornerOf(origin).add(
					TEMPLE_FORMATION[profileIndex]);
			HunterEntity hunter = byProfile.get(profileIndex);
			boolean placedAtEntry = hunter == null
					|| hunter.level() != temple;
			if (hunter == null)
				hunter = spawnHunterAt(owner, temple, profileIndex, destination);
			else if (hunter.level() != temple)
				hunter = moveHunter(hunter, temple, destination);
			if (hunter == null)
				continue;
			tagOwned(hunter, HUNTER_MARKER, owner.getUUID());
			hunter.getPersistentData().putInt(PROFILE_INDEX, profileIndex);
			hunter.getPersistentData().putInt(INSTANCE_ID,
					data.cartenonInstanceId());
			hunter.getPersistentData().putString(DUNGEON_TAG_KEY,
					STORY_DUNGEON_TAG);
			hunter.setPersistenceRequired();
			if (placedAtEntry || hunter.isNoAi()
					|| !hunter.getPersistentData().getBoolean(
							TEMPLE_FOLLOW_MARKER))
				enableTempleFollowing(hunter);
			movedIds.add(hunter.getUUID());
			movedHunters.add(hunter);
		}
		String allies = buildAllies(owner.getUUID(), movedHunters);
		for (HunterEntity hunter : movedHunters)
			hunter.getEntityData().set(HunterEntity.DATA_Allies, allies);
		data.replaceHunterIds(movedIds);
	}

	private static void addHunterByProfile(Map<Integer, HunterEntity> byProfile,
			HunterEntity hunter, UUID ownerId) {
		if (hunter == null || !hunter.isAlive()
				|| !hunter.getTags().contains(HUNTER_MARKER)
				|| !hasOwner(hunter, ownerId))
			return;
		int profile = hunter.getPersistentData().getInt(PROFILE_INDEX);
		if (profile < 0 || profile >= TEAM_SIZE)
			return;
		HunterEntity existing = byProfile.putIfAbsent(profile, hunter);
		if (existing != null && existing != hunter)
			retireDuplicateStoryHunter(hunter);
	}

	private static void retireDuplicateStoryHunter(HunterEntity hunter) {
		hunter.setNoAi(false);
		hunter.setTarget(null);
		removeOwnedTags(hunter, HUNTER_MARKER);
		if (STORY_DUNGEON_TAG.equals(
				hunter.getPersistentData().getString(DUNGEON_TAG_KEY)))
			hunter.getPersistentData().remove(DUNGEON_TAG_KEY);
		hunter.discard();
	}

	@Nullable
	private static AncientGolemEntity spawnReplacementBoss(ServerPlayer owner,
			ServerLevel dungeon) {
		AncientGolemEntity existing = findAndMarkBoss(owner, dungeon);
		if (existing != null)
			return existing;
		AncientGolemEntity boss = SololevelingModEntities.ANCIENT_GOLEM.get()
				.create(dungeon);
		if (boss == null)
			return null;
		Vec3 spawn = Vec3.atLowerCornerOf(ANCIENT_GOLEM_ORIGIN)
				.add(98.86D, 2.0D, 24.69D);
		boss.moveTo(spawn.x, spawn.y, spawn.z, 90.0F, 0.0F);
		boss.finalizeSpawn(dungeon, dungeon.getCurrentDifficultyAt(
				BlockPos.containing(spawn)), MobSpawnType.EVENT, null, null);
		tagOwned(boss, BOSS_MARKER, owner.getUUID());
		boss.getPersistentData().putString(DUNGEON_TAG_KEY,
				STORY_DUNGEON_TAG);
		boss.setPersistenceRequired();
		if (!dungeon.addFreshEntity(boss))
			return null;
		SololevelingMod.LOGGER.warn(
				"Story Mode restored its missing Ancient Golem boss.");
		return boss;
	}

	@Nullable
	private static CartenonGateEntity spawnReplacementGate(ServerPlayer owner,
			ServerLevel dungeon, int instanceId) {
		if (instanceId <= 0)
			return null;
		CartenonGateEntity gate = SololevelingModEntities.CARTENON_GATE.get()
				.create(dungeon);
		if (gate == null)
			return null;
		gate.configure(owner.getUUID(), Set.of(owner.getUUID()), instanceId);
		BlockPos bossCenter = ANCIENT_GOLEM_ORIGIN.offset(99, 2, 25);
		BlockPos gatePos = findStoryGatePosition(dungeon, bossCenter);
		gate.moveTo(gatePos.getX() + 0.5D, gatePos.getY(),
				gatePos.getZ() + 0.5D, owner.getYRot() + 180.0F, 0.0F);
		if (!dungeon.addFreshEntity(gate))
			return null;
		onCartenonGateCreated(owner, gate, instanceId);
		SololevelingMod.LOGGER.warn(
				"Story Mode restored its missing Cartenon gate.");
		return gate;
	}

	private static BlockPos findStoryGatePosition(ServerLevel level,
			BlockPos center) {
		int[][] offsets = {
				{3, 0}, {-3, 0}, {0, 3}, {0, -3},
				{4, 4}, {-4, 4}, {4, -4}, {-4, -4}, {0, 0}
		};
		for (int[] offset : offsets) {
			for (int dy = 3; dy >= -3; dy--) {
				BlockPos candidate = center.offset(offset[0], dy, offset[1]);
				if (level.getBlockState(candidate.below()).isFaceSturdy(
						level, candidate.below(), Direction.UP)
						&& level.getBlockState(candidate).getCollisionShape(
								level, candidate).isEmpty()
						&& level.getBlockState(candidate.above())
								.getCollisionShape(level,
										candidate.above()).isEmpty()
						&& level.getBlockState(candidate.above(2))
								.getCollisionShape(level,
										candidate.above(2)).isEmpty())
					return candidate;
			}
		}
		return center.above();
	}

	private static void prepareAncientGolemIntro(ServerPlayer player,
			StoryModeIntroSavedData data) {
		ServerLevel dungeon = player.server.getLevel(ANCIENT_GOLEM_DIMENSION);
		if (dungeon == null) {
			SololevelingMod.LOGGER.error(
					"Story Mode cannot start: dimension {} is unavailable.",
					ANCIENT_GOLEM_DIMENSION.location());
			return;
		}
		loadDungeonChunks(dungeon);
		player.getPersistentData().putString(DUNGEON_TAG_KEY, STORY_DUNGEON_TAG);
		if (!data.dungeonPlaced() && appearsDungeonAlreadyPlaced(dungeon))
			data.markDungeonPlaced();
		if (!data.dungeonPlaced()) {
			StructureTemplate template = dungeon.getStructureManager()
					.get(ANCIENT_GOLEM_STRUCTURE).orElse(null);
			if (template == null || template.getSize().getX() <= 0) {
				SololevelingMod.LOGGER.error(
						"Story Mode cannot start: structure {} is unavailable.",
						ANCIENT_GOLEM_STRUCTURE);
				return;
			}
			boolean placed = template.placeInWorld(dungeon, ANCIENT_GOLEM_ORIGIN,
					ANCIENT_GOLEM_ORIGIN,
					new StructurePlaceSettings().setRotation(Rotation.NONE)
							.setMirror(Mirror.NONE).setIgnoreEntities(false),
					dungeon.random, 3);
			if (!placed) {
				SololevelingMod.LOGGER.error(
						"Story Mode could not place {} at {}.",
						ANCIENT_GOLEM_STRUCTURE, ANCIENT_GOLEM_ORIGIN);
				return;
			}
			data.markDungeonPlaced();
		}
		tagStoryReturnPortals(dungeon);
		AncientGolemEntity boss = findAndMarkBoss(player, dungeon);
		if (boss == null) {
			if (!bossRecoveryGraceElapsed(player, data))
				return;
			repairStoryDungeonBlocks(dungeon);
			boss = findAndMarkBoss(player, dungeon);
			if (boss == null)
				boss = spawnReplacementBoss(player, dungeon);
			if (boss == null) {
				SololevelingMod.LOGGER.warn(
						"Story Mode is waiting for its Ancient Golem boss at {}.",
						ANCIENT_GOLEM_ORIGIN);
				return;
			}
		}
		data.setBossId(boss.getUUID());
		List<HunterEntity> hunters = ensureInitialTeam(player, dungeon, data);
		if (hunters.size() < TEAM_SIZE)
			return;
		data.setStage(StoryModeIntroSavedData.Stage.ANCIENT_GOLEM,
				player.server.overworld().getGameTime());
		Vec3 entry = dungeonEntry();
		player.stopRiding();
		player.teleportTo(dungeon, entry.x, entry.y, entry.z, -90.0F, 0.0F);
	}

	private static void repairStoryDungeonBlocks(ServerLevel dungeon) {
		StructureTemplate template = dungeon.getStructureManager()
				.get(ANCIENT_GOLEM_STRUCTURE).orElse(null);
		if (template == null || template.getSize().getX() <= 0)
			return;
		template.placeInWorld(dungeon, ANCIENT_GOLEM_ORIGIN,
				ANCIENT_GOLEM_ORIGIN,
				new StructurePlaceSettings().setRotation(Rotation.NONE)
						.setMirror(Mirror.NONE).setIgnoreEntities(true),
				dungeon.random, 3);
	}

	private static void tagStoryReturnPortals(ServerLevel dungeon) {
		for (Entity entity : dungeon.getEntitiesOfClass(Entity.class,
				dungeonBounds().inflate(3.0D),
				candidate -> candidate.getType()
						== SololevelingModEntities.PORTAL_12.get())) {
			entity.getPersistentData().putString(DUNGEON_TAG_KEY,
					STORY_DUNGEON_TAG);
		}
	}

	private static AncientGolemEntity findAndMarkBoss(ServerPlayer owner,
			ServerLevel dungeon) {
		AABB bounds = dungeonBounds().inflate(3.0D);
		StoryModeIntroSavedData data = StoryModeIntroSavedData.get(owner.server);
		Vec3 expected = Vec3.atLowerCornerOf(ANCIENT_GOLEM_ORIGIN)
				.add(98.86D, 2.0D, 24.69D);
		List<AncientGolemEntity> bosses = new ArrayList<>(
				dungeon.getEntitiesOfClass(AncientGolemEntity.class,
						bounds, entity -> !entity.isRemoved()));
		if (data.bossId() != null) {
			AncientGolemEntity tracked = findEntity(owner.server,
					data.bossId(), AncientGolemEntity.class);
			if (tracked != null && tracked.level() == dungeon
					&& !tracked.isRemoved()
					&& !bosses.contains(tracked))
				bosses.add(tracked);
		}
		bosses.sort(Comparator
				.<AncientGolemEntity>comparingInt(entity -> {
					if (data.bossId() != null
							&& data.bossId().equals(entity.getUUID()))
						return 0;
					if (entity.getTags().contains(BOSS_MARKER)
							&& hasOwner(entity, owner.getUUID()))
						return 1;
					return 2;
				})
				.thenComparingDouble(entity -> entity.distanceToSqr(expected)));
		AncientGolemEntity boss = bosses.isEmpty() ? null : bosses.get(0);
		for (int index = 1; index < bosses.size(); index++)
			bosses.get(index).discard();
		if (bosses.size() > 1)
			SololevelingMod.LOGGER.warn(
					"Story Mode removed {} duplicate Ancient Golem boss(es).",
					bosses.size() - 1);
		if (boss != null) {
			data.noteBossObserved();
			tagOwned(boss, BOSS_MARKER, owner.getUUID());
			boss.getPersistentData().putString(DUNGEON_TAG_KEY,
					STORY_DUNGEON_TAG);
			boss.setPersistenceRequired();
		}
		return boss;
	}

	private static boolean bossRecoveryGraceElapsed(ServerPlayer owner,
			StoryModeIntroSavedData data) {
		return data.bossRecoveryGraceElapsed(
				owner.server.overworld().getGameTime(),
				BOSS_ENTITY_LOAD_GRACE_TICKS);
	}

	private static List<HunterEntity> ensureInitialTeam(ServerPlayer owner,
			ServerLevel dungeon, StoryModeIntroSavedData data) {
		Map<Integer, HunterEntity> byProfile = new LinkedHashMap<>();
		for (UUID hunterId : data.hunterIds()) {
			HunterEntity hunter = findEntity(owner.server, hunterId,
					HunterEntity.class);
			if (hunter != null && hunter.level() != dungeon) {
				int profile = hunter.getPersistentData().getInt(PROFILE_INDEX);
				if (profile >= 0 && profile < TEAM_SIZE)
					hunter = moveHunter(hunter, dungeon,
							dungeonEntry().add(TEAM_ENTRY_OFFSETS[profile]));
			}
			addHunterByProfile(byProfile, hunter, owner.getUUID());
		}
		for (HunterEntity hunter : dungeon.getEntitiesOfClass(HunterEntity.class,
				dungeonBounds().inflate(16.0D),
				entity -> entity.getTags().contains(HUNTER_MARKER)
						&& hasOwner(entity, owner.getUUID()))) {
			addHunterByProfile(byProfile, hunter, owner.getUUID());
		}
		for (int index = 0; index < TEAM_SIZE; index++) {
			if (byProfile.containsKey(index))
				continue;
			HunterEntity hunter = spawnHunter(owner, dungeon, index);
			if (hunter != null)
				byProfile.put(index, hunter);
		}
		List<HunterEntity> result = byProfile.entrySet().stream()
				.sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
		data.replaceHunterIds(result.stream().map(Entity::getUUID).toList());
		String allies = buildAllies(owner.getUUID(), result);
		for (HunterEntity hunter : result)
			{
				hunter.getEntityData().set(HunterEntity.DATA_Allies, allies);
				hunter.getPersistentData().putString(DUNGEON_TAG_KEY,
						STORY_DUNGEON_TAG);
			}
		return result;
	}

	@Nullable
	private static HunterEntity spawnHunter(ServerPlayer owner, ServerLevel dungeon,
			int profileIndex) {
		return spawnHunterAt(owner, dungeon, profileIndex,
				dungeonEntry().add(TEAM_ENTRY_OFFSETS[profileIndex]));
	}

	@Nullable
	private static HunterEntity spawnHunterAt(ServerPlayer owner, ServerLevel level,
			int profileIndex, Vec3 spawn) {
		HunterEntity hunter = SololevelingModEntities.HUNTER.get().create(level);
		if (hunter == null)
			return null;
		hunter.moveTo(spawn.x, spawn.y, spawn.z, -90.0F, 0.0F);
		hunter.finalizeSpawn(level, level.getCurrentDifficultyAt(
				BlockPos.containing(spawn)), MobSpawnType.EVENT, null, null);
		applyProfile(hunter, HUNTER_PROFILES.get(profileIndex));
		hunter.tame(owner);
		hunter.setPersistenceRequired();
		hunter.setNoAi(false);
		hunter.setInvulnerable(false);
		tagOwned(hunter, HUNTER_MARKER, owner.getUUID());
		hunter.getPersistentData().putString(DUNGEON_TAG_KEY,
				STORY_DUNGEON_TAG);
		hunter.getPersistentData().putInt(PROFILE_INDEX, profileIndex);
		return level.addFreshEntity(hunter) ? hunter : null;
	}

	private static void applyProfile(HunterEntity hunter, HunterProfile profile) {
		hunter.getEntityData().set(HunterEntity.DATA_Rank, profile.rank());
		hunter.getEntityData().set(HunterEntity.DATA_HunterClass, profile.hunterClass());
		hunter.getEntityData().set(HunterEntity.DATA_Eyes, profile.eyes());
		hunter.getEntityData().set(HunterEntity.DATA_TopIn, profile.topIn());
		hunter.getEntityData().set(HunterEntity.DATA_TopOut, profile.topOut());
		hunter.getEntityData().set(HunterEntity.DATA_Bottom, profile.bottom());
		hunter.getEntityData().set(HunterEntity.DATA_Foot, profile.foot());
		hunter.getEntityData().set(HunterEntity.DATA_EyeBs, profile.eyeBase());
		hunter.getEntityData().set(HunterEntity.DATA_Hair, profile.hair());
		hunter.getEntityData().set(HunterEntity.DATA_Mouth, profile.mouth());
		hunter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
				stack(profile.mainHand()));
		hunter.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,
				stack(profile.offHand()));
		setBaseAttribute(hunter, Attributes.ATTACK_DAMAGE, profile.attack());
		setBaseAttribute(hunter, Attributes.MAX_HEALTH, profile.health());
		setBaseAttribute(hunter, Attributes.ARMOR, profile.armor());
		setBaseAttribute(hunter, Attributes.MOVEMENT_SPEED, profile.speed());
		setBaseAttribute(hunter, Attributes.FOLLOW_RANGE, 64.0D);
		hunter.setHealth(hunter.getMaxHealth());
		hunter.getPersistentData().putDouble("int", rankPower(profile.rank()) * 12.0D);
	}

	private static void initializePlayer(ServerPlayer player, int classId) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.HunterRank = E_RANK_ID;
					capability.prevRank = E_RANK_ID;
					capability.Level = 1;
					capability.prevLevel = 1;
					capability.Classes = classId;
					capability.ranking = "E";
					capability.Player = false;
					capability.syncPlayerVariables(player);
				});
		markOwner(player);
	}

	private static int chooseStartingClass(ServerPlayer player) {
		long choice = player.getUUID().getMostSignificantBits()
				^ player.getUUID().getLeastSignificantBits()
				^ player.server.overworld().getSeed();
		return (Long.bitCount(choice) & 1) == 0 ? FIGHTER_CLASS_ID : ASSASSIN_CLASS_ID;
	}

	private static void resumeOwner(ServerPlayer player, StoryModeIntroSavedData data) {
		StoryModeIntroSavedData.Stage stage = data.stage();
		if (stage == StoryModeIntroSavedData.Stage.PREPARING) {
			prepareAncientGolemIntro(player, data);
			return;
		}
		if (stage == StoryModeIntroSavedData.Stage.ANCIENT_GOLEM
				|| stage == StoryModeIntroSavedData.Stage.GATE_WAIT) {
			player.getPersistentData().putString(DUNGEON_TAG_KEY,
					STORY_DUNGEON_TAG);
			if (player.serverLevel().dimension() != ANCIENT_GOLEM_DIMENSION) {
				ServerLevel dungeon = player.server.getLevel(ANCIENT_GOLEM_DIMENSION);
				if (dungeon != null) {
					loadDungeonChunks(dungeon);
					Vec3 returnPosition = dungeonEntry();
					if (stage == StoryModeIntroSavedData.Stage.GATE_WAIT
							&& data.gateId() != null) {
						Entity gate = dungeon.getEntity(data.gateId());
						if (gate != null)
							returnPosition = gate.position().add(0.0D, 0.0D,
									3.5D);
					}
					player.teleportTo(dungeon, returnPosition.x,
							returnPosition.y, returnPosition.z,
							-90.0F, 0.0F);
				}
			}
			return;
		}
		if (data.cartenonInstanceId() > 0
				&& player.serverLevel().dimension() != CartenonTempleManager.CARTENON_DIMENSION) {
			ServerLevel temple = player.server.getLevel(
					CartenonTempleManager.CARTENON_DIMENSION);
			if (temple != null) {
				BlockPos entry = cartenonInstanceOrigin(data.cartenonInstanceId())
						.relative(Direction.SOUTH, 8).above();
				player.teleportTo(temple, entry.getX() + 0.5D, entry.getY(),
						entry.getZ() + 0.5D, 0.0F, 0.0F);
			}
		}
	}

	private static List<HunterEntity> findStoryHunters(MinecraftServer server,
			StoryModeIntroSavedData data) {
		Map<UUID, HunterEntity> found = new LinkedHashMap<>();
		if (data.cartenonInstanceId() > 0) {
			ServerLevel temple = server.getLevel(
					CartenonTempleManager.CARTENON_DIMENSION);
			if (temple != null)
				loadTempleActorChunks(temple, data.cartenonInstanceId());
		} else {
			ServerLevel dungeon = server.getLevel(ANCIENT_GOLEM_DIMENSION);
			if (dungeon != null)
				loadDungeonChunks(dungeon);
		}
		for (UUID hunterId : data.hunterIds()) {
			HunterEntity hunter = findEntity(server, hunterId, HunterEntity.class);
			if (hunter != null)
				found.putIfAbsent(hunter.getUUID(), hunter);
		}
		if (data.cartenonInstanceId() > 0) {
			ServerLevel temple = server.getLevel(
					CartenonTempleManager.CARTENON_DIMENSION);
			if (temple != null) {
				BlockPos origin = cartenonInstanceOrigin(
						data.cartenonInstanceId());
				AABB partyBounds = new AABB(origin.offset(-24, -8, -8),
						origin.offset(24, 24, 32));
				for (HunterEntity hunter : temple.getEntitiesOfClass(
						HunterEntity.class, partyBounds,
						entity -> entity.getTags().contains(HUNTER_MARKER)
								&& data.ownerId() != null
								&& hasOwner(entity, data.ownerId())
								&& entity.getPersistentData().getInt(INSTANCE_ID)
										== data.cartenonInstanceId()))
					found.putIfAbsent(hunter.getUUID(), hunter);
			}
		} else {
			ServerLevel dungeon = server.getLevel(ANCIENT_GOLEM_DIMENSION);
			if (dungeon != null) {
				for (HunterEntity hunter : dungeon.getEntitiesOfClass(
						HunterEntity.class, dungeonBounds().inflate(16.0D),
						entity -> entity.getTags().contains(HUNTER_MARKER)
								&& data.ownerId() != null
								&& hasOwner(entity, data.ownerId())))
					found.putIfAbsent(hunter.getUUID(), hunter);
			}
		}
		List<HunterEntity> result = new ArrayList<>(found.values());
		result.sort(Comparator.comparingInt(
				entity -> entity.getPersistentData().getInt(PROFILE_INDEX)));
		return result;
	}

	@Nullable
	private static StatueOfGodEntity findAndMarkGodStatue(ServerPlayer owner,
			ServerLevel temple, int instanceId) {
		BlockPos origin = cartenonInstanceOrigin(instanceId);
		BlockPos expected = origin.relative(Direction.SOUTH, 145).above(6);
		AABB bounds = new AABB(origin.offset(-18, -3, 128),
				origin.offset(18, 40, 155));
		StatueOfGodEntity god = temple.getEntitiesOfClass(StatueOfGodEntity.class,
				bounds, Entity::isAlive).stream()
				.min(Comparator.comparingDouble(entity -> entity.distanceToSqr(
						Vec3.atCenterOf(expected)))).orElse(null);
		if (god == null)
			god = spawnReplacementGodStatue(temple, expected);
		if (god != null) {
			tagOwned(god, GOD_MARKER, owner.getUUID());
			god.getPersistentData().putBoolean(STORY_STATUE_MARKER, true);
			god.getPersistentData().putUUID(OWNER_MARKER, owner.getUUID());
			god.getPersistentData().putInt(INSTANCE_ID, Math.max(1, instanceId));
			god.getPersistentData().putBoolean(STORY_LASER_DONE, false);
			god.getPersistentData().remove(STORY_ACTIVATION_AT);
			god.setPersistenceRequired();
		}
		return god;
	}

	@Nullable
	private static StatueOfGodEntity spawnReplacementGodStatue(ServerLevel temple,
			BlockPos expected) {
		StatueOfGodEntity god = SololevelingModEntities.STATUE_OF_GOD.get()
				.create(temple);
		if (god == null)
			return null;
		float yaw = 180.0F;
		god.moveTo(expected.getX() + 0.5D, expected.getY(),
				expected.getZ() + 0.5D, yaw, 0.0F);
		god.finalizeSpawn(temple, temple.getCurrentDifficultyAt(expected),
				MobSpawnType.STRUCTURE, null, null);
		god.setYRot(yaw);
		god.setYBodyRot(yaw);
		god.setYHeadRot(yaw);
		god.getPersistentData().putString("state", "throne");
		god.getPersistentData().putInt("IA", 0);
		god.getPersistentData().putFloat("CartenonHomeYaw", yaw);
		god.getPersistentData().putBoolean("CartenonTempleStatue", true);
		god.getEntityData().set(StatueOfGodEntity.DATA_state, "throne");
		god.getEntityData().set(StatueOfGodEntity.DATA_default_x,
				expected.getX());
		god.getEntityData().set(StatueOfGodEntity.DATA_default_y,
				expected.getY());
		god.getEntityData().set(StatueOfGodEntity.DATA_default_z,
				expected.getZ());
		god.setNoAi(true);
		god.setPersistenceRequired();
		if (!temple.addFreshEntity(god))
			return null;
		SololevelingMod.LOGGER.warn(
				"Story Mode restored its missing Statue of God.");
		return god;
	}

	@Nullable
	private static HunterEntity moveHunter(HunterEntity hunter,
			ServerLevel destinationLevel, Vec3 destination) {
		if (hunter.level() == destinationLevel) {
			hunter.moveTo(destination.x, destination.y, destination.z,
					hunter.getYRot(), hunter.getXRot());
			return hunter;
		}
		ITeleporter teleporter = new ITeleporter() {
			@Override
			public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
					Function<ServerLevel, PortalInfo> defaultPortalInfo) {
				return new PortalInfo(destination, Vec3.ZERO, entity.getYRot(),
						entity.getXRot());
			}

			@Override
			public Entity placeEntity(Entity entity, ServerLevel currentWorld,
					ServerLevel destWorld, float yaw,
					Function<Boolean, Entity> repositionEntity) {
				Entity moved = repositionEntity.apply(false);
				moved.moveTo(destination.x, destination.y, destination.z,
						yaw, entity.getXRot());
				return moved;
			}

			@Override
			public boolean playTeleportSound(ServerPlayer player,
					ServerLevel sourceWorld, ServerLevel destWorld) {
				return false;
			}
		};
		Entity moved = hunter.changeDimension(destinationLevel, teleporter);
		return moved instanceof HunterEntity movedHunter ? movedHunter : null;
	}

	private static void freezeAt(HunterEntity hunter, Vec3 position) {
		hunter.getPersistentData().remove(TEMPLE_FOLLOW_MARKER);
		hunter.moveTo(position.x, position.y, position.z,
				hunter.getYRot(), hunter.getXRot());
		hunter.setNoAi(true);
		hunter.setTarget(null);
		hunter.getNavigation().stop();
		hunter.setDeltaMovement(Vec3.ZERO);
		hunter.fallDistance = 0.0F;
	}

	private static void enableTempleFollowing(HunterEntity hunter) {
		hunter.getPersistentData().putBoolean(TEMPLE_FOLLOW_MARKER, true);
		hunter.setOrderedToSit(false);
		hunter.setNoAi(false);
		hunter.setTarget(null);
		hunter.getNavigation().stop();
		hunter.setDeltaMovement(Vec3.ZERO);
		hunter.fallDistance = 0.0F;
	}

	private static void releaseFrozenTempleHunters(MinecraftServer server,
			StoryModeIntroSavedData data) {
		for (HunterEntity hunter : findStoryHunters(server, data)) {
			if (hunter.level().dimension()
						!= CartenonTempleManager.CARTENON_DIMENSION)
				continue;
			boolean needsRelease = hunter.isNoAi()
					|| hunter.isOrderedToSit()
					|| !hunter.getPersistentData().getBoolean(
							TEMPLE_FOLLOW_MARKER);
			if (needsRelease)
				enableTempleFollowing(hunter);
		}
	}

	private static void cleanActiveMarkers(MinecraftServer server,
			StoryModeIntroSavedData data) {
		ServerLevel dungeon = server.getLevel(ANCIENT_GOLEM_DIMENSION);
		if (dungeon != null)
			loadDungeonChunks(dungeon);
		ServerLevel temple = null;
		if (data.cartenonInstanceId() > 0) {
			temple = server.getLevel(
					CartenonTempleManager.CARTENON_DIMENSION);
			if (temple != null) {
				loadTempleActorChunks(temple,
						data.cartenonInstanceId());
				loadStoryGodCorridorChunks(temple,
						data.cartenonInstanceId());
				if (data.godStatuePosition() != null)
					loadChunksAround(temple,
							data.godStatuePosition(), 1);
			}
		}
		for (HunterEntity hunter : findStoryHunters(server, data)) {
			hunter.setNoAi(false);
			hunter.getPersistentData().remove(TEMPLE_FOLLOW_MARKER);
			removeOwnedTags(hunter, HUNTER_MARKER);
		}
		Entity boss = data.bossId() == null ? null
				: findEntity(server, data.bossId(), Entity.class);
		if (boss != null)
			removeOwnedTags(boss, BOSS_MARKER);
		Entity gate = data.gateId() == null ? null
				: findEntity(server, data.gateId(), Entity.class);
		if (gate != null)
			removeOwnedTags(gate, GATE_MARKER);
		Map<UUID, StatueOfGodEntity> gods = new LinkedHashMap<>();
		StatueOfGodEntity savedGod = data.godStatueId() == null ? null
				: findEntity(server, data.godStatueId(),
						StatueOfGodEntity.class);
		if (savedGod != null)
			gods.put(savedGod.getUUID(), savedGod);
		if (temple != null && data.ownerId() != null
				&& data.cartenonInstanceId() > 0) {
			BlockPos origin = cartenonInstanceOrigin(
					data.cartenonInstanceId());
			for (StatueOfGodEntity candidate : temple.getEntitiesOfClass(
					StatueOfGodEntity.class,
					new AABB(origin.offset(-24, -8, -8),
							origin.offset(24, 48, 170)),
					Entity::isAlive)) {
				if (candidate.getPersistentData().getInt(INSTANCE_ID)
								== data.cartenonInstanceId()
						&& (hasOwner(candidate, data.ownerId())
								|| candidate.getPersistentData()
										.getBoolean(STORY_STATUE_MARKER)))
					gods.putIfAbsent(candidate.getUUID(), candidate);
			}
		}
		for (StatueOfGodEntity statue : gods.values())
			resetStoryGod(statue);
	}

	private static void resetStoryGod(StatueOfGodEntity statue) {
		statue.getEntityData().set(StatueOfGodEntity.SHOOT, false);
		statue.getPersistentData().remove(STORY_STATUE_MARKER);
		statue.getPersistentData().remove(OWNER_MARKER);
		statue.getPersistentData().remove(STORY_LASER_DONE);
		statue.getPersistentData().remove(STORY_ACTIVATION_AT);
		statue.getPersistentData().putString("state", "throne");
		statue.getPersistentData().putInt("IA", 0);
		statue.getEntityData().set(StatueOfGodEntity.DATA_state, "throne");
		statue.getEntityData().set(
				StatueOfGodEntity.DATA_story_upright, false);
		statue.setTarget(null);
		statue.getNavigation().stop();
		statue.setDeltaMovement(Vec3.ZERO);
		statue.setNoAi(true);
		removeOwnedTags(statue, GOD_MARKER);
	}

	private static <T extends Entity> T findEntity(MinecraftServer server, UUID id,
			Class<T> expectedType) {
		if (server == null || id == null)
			return null;
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(id);
			if (expectedType.isInstance(entity))
				return expectedType.cast(entity);
		}
		return null;
	}

	private static void tagOwned(Entity entity, String marker, UUID ownerId) {
		entity.addTag(marker);
		entity.getPersistentData().putBoolean(marker, true);
		entity.getPersistentData().putUUID(OWNER_UUID, ownerId);
	}

	private static boolean hasOwner(Entity entity, UUID ownerId) {
		return entity != null && ownerId != null
				&& entity.getPersistentData().hasUUID(OWNER_UUID)
				&& ownerId.equals(entity.getPersistentData().getUUID(OWNER_UUID));
	}

	private static void removeOwnedTags(Entity entity, String marker) {
		entity.removeTag(marker);
		entity.getPersistentData().remove(marker);
		entity.getPersistentData().remove(OWNER_UUID);
		entity.getPersistentData().remove(INSTANCE_ID);
	}

	private static void markOwner(ServerPlayer player) {
		player.getPersistentData().putBoolean(OWNER_MARKER, true);
		player.getPersistentData().putUUID(OWNER_UUID, player.getUUID());
	}

	private static Vec3 dungeonEntry() {
		return Vec3.atLowerCornerOf(ANCIENT_GOLEM_ORIGIN).add(ANCIENT_GOLEM_ENTRY);
	}

	private static AABB dungeonBounds() {
		return new AABB(ANCIENT_GOLEM_ORIGIN,
				ANCIENT_GOLEM_ORIGIN.offset(126, 22, 51));
	}

	private static void loadDungeonChunks(ServerLevel level) {
		int minChunkX = ANCIENT_GOLEM_ORIGIN.getX() >> 4;
		int minChunkZ = ANCIENT_GOLEM_ORIGIN.getZ() >> 4;
		int maxChunkX = (ANCIENT_GOLEM_ORIGIN.getX() + 125) >> 4;
		int maxChunkZ = (ANCIENT_GOLEM_ORIGIN.getZ() + 50) >> 4;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
				level.getChunk(chunkX, chunkZ);
		}
	}

	private static void loadTempleActorChunks(ServerLevel level, int instanceId) {
		BlockPos origin = cartenonInstanceOrigin(instanceId);
		forceChunk(level, origin);
		for (Vec3 offset : TEMPLE_FORMATION)
			forceChunk(level, BlockPos.containing(
					Vec3.atLowerCornerOf(origin).add(offset)));
		forceChunk(level, cartenonTempleCenter(instanceId));
		forceChunk(level, origin.relative(Direction.SOUTH, 145).above(6));
	}

	private static boolean appearsDungeonAlreadyPlaced(ServerLevel level) {
		int solidSamples = 0;
		int[][] samples = {
				{10, 0, 25},
				{38, 0, 25},
				{68, 0, 25},
				{98, 0, 25},
				{108, 0, 47}
		};
		for (int[] sample : samples) {
			if (!level.getBlockState(ANCIENT_GOLEM_ORIGIN.offset(
					sample[0], sample[1], sample[2])).isAir())
				solidSamples++;
		}
		if (solidSamples >= 3)
			return true;
		return !level.getEntitiesOfClass(AncientGolemEntity.class,
				dungeonBounds().inflate(3.0D), entity -> true).isEmpty();
	}

	private static void forceChunk(ServerLevel level, BlockPos pos) {
		level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
	}

	private static ItemStack stack(String id) {
		Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
		return new ItemStack(item == null ? Items.AIR : item);
	}

	private static void setBaseAttribute(LivingEntity entity, Attribute attribute,
			double value) {
		var instance = entity.getAttribute(attribute);
		if (instance != null)
			instance.setBaseValue(value);
	}

	private static int rankPower(String rank) {
		return switch (rank) {
			case "B" -> 3;
			case "C" -> 2;
			default -> 1;
		};
	}

	private static String buildAllies(UUID owner, List<HunterEntity> hunters) {
		StringBuilder builder = new StringBuilder(owner.toString());
		for (HunterEntity hunter : hunters)
			builder.append(',').append(hunter.getUUID());
		return builder.toString();
	}

	private record HunterProfile(String rank, String hunterClass, double attack,
			double health, double armor, double speed, String mainHand,
			String offHand, int eyes, int topOut, int bottom, int foot,
			int hair, int eyeBase, int topIn, int mouth) {
	}
}
