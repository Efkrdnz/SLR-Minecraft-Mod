package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.DungeonBuilderStatusMessage;
import net.solocraft.dungeon.builder.DungeonBuilderProjectData;
import net.solocraft.dungeon.builder.DungeonBuilderStudioService;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Runtime behavior and persistent marker for Dungeon Builder worlds. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DungeonBuilderMode {
	private static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(
			Registries.DIMENSION_TYPE, new ResourceLocation(SololevelingMod.MODID, "dungeon_builder"));
	private static final BlockPos PLATFORM_CENTER = new BlockPos(0, 64, 0);
	private static final int PLATFORM_RADIUS = 4;
	private static final Set<UUID> HUD_PLAYERS = new HashSet<>();
	private static int hudTick;

	private DungeonBuilderMode() {
	}

	public static boolean isActive(@Nullable LevelAccessor world) {
		return world instanceof Level level && level.dimensionTypeRegistration().is(DIMENSION_TYPE);
	}

	public static boolean isBuilderWorld(@Nullable MinecraftServer server) {
		return server != null && server.overworld() != null && isActive(server.overworld());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onServerStarted(ServerStartedEvent event) {
		HUD_PLAYERS.clear();
		hudTick = 0;
		ServerLevel level = event.getServer().overworld();
		if (!isActive(level)) {
			return;
		}

		DungeonBuilderSavedData data = DungeonBuilderSavedData.get(level);
		if (!data.defaultsConfigured()) {
			configureRules(level);
			data.markDefaultsConfigured();
		}
		event.getServer().setDefaultGameType(GameType.CREATIVE);
		level.setDefaultSpawnPos(PLATFORM_CENTER.above(), 0.0F);

		if (!data.platformCreated()) {
			createPlatform(level);
			data.markPlatformCreated();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && isActive(player.level()) && isBuilderWorld(player.getServer())) {
			player.setGameMode(GameType.CREATIVE);
			disablePlayerSystems(player);
			giveBuilderKit(player);
			showWelcome(player);
			sendHud(player);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && isActive(player.level()) && isBuilderWorld(player.getServer())) {
			player.setGameMode(GameType.CREATIVE);
			disablePlayerSystems(player);
			giveBuilderKit(player);
			sendHud(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		HUD_PLAYERS.remove(event.getEntity().getUUID());
		if (event.getEntity() instanceof ServerPlayer player)
			DungeonBuilderStudioService.playerLoggedOut(player);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || ++hudTick % 10 != 0)
			return;
		for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
			boolean active = isActive(player.level()) && isBuilderWorld(player.getServer());
			if (active) {
				sendHud(player);
			} else if (HUD_PLAYERS.remove(player.getUUID())) {
				SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
						DungeonBuilderStatusMessage.inactive());
			}
		}
	}

	private static void sendHud(ServerPlayer player) {
		HUD_PLAYERS.add(player.getUUID());
		DungeonBuilderProjectData.Project project = DungeonBuilderProjectData.get(player.serverLevel()).project(player);
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				DungeonBuilderStatusMessage.from(project));
	}

	private static void configureRules(ServerLevel level) {
		MinecraftServer server = level.getServer();
		GameRules rules = level.getGameRules();

		setFalse(rules, GameRules.RULE_DOMOBSPAWNING, server);
		setFalse(rules, GameRules.RULE_DO_PATROL_SPAWNING, server);
		setFalse(rules, GameRules.RULE_DO_TRADER_SPAWNING, server);
		setFalse(rules, GameRules.RULE_DO_WARDEN_SPAWNING, server);
		setFalse(rules, GameRules.RULE_DOINSOMNIA, server);
		setFalse(rules, GameRules.RULE_DAYLIGHT, server);
		setFalse(rules, GameRules.RULE_WEATHER_CYCLE, server);
		setTrue(rules, GameRules.RULE_KEEPINVENTORY, server);
		rules.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);

		setFalse(rules, SololevelingModGameRules.SOLO_DAILY_QUEST, server);
		setFalse(rules, SololevelingModGameRules.SOLO_DUNGEON_PROGRESSION_ONLY, server);
		setFalse(rules, SololevelingModGameRules.SOLO_FATIGUE, server);
		setFalse(rules, SololevelingModGameRules.SOLO_GATE_NOTIFICATION, server);
		setFalse(rules, SololevelingModGameRules.SOLO_GATE_SPAWNING, server);
		setFalse(rules, SololevelingModGameRules.SOLO_MISC_ITEMS, server);
		setFalse(rules, SololevelingModGameRules.SOLO_DUNGEON_BREAK, server);
		setFalse(rules, SololevelingModGameRules.SOLO_BLOOD_EFFECTS, server);
		setFalse(rules, SololevelingModGameRules.SOLO_PUNISHMENT, server);
	}

	private static void setFalse(GameRules rules, GameRules.Key<GameRules.BooleanValue> rule,
			MinecraftServer server) {
		rules.getRule(rule).set(false, server);
	}

	private static void setTrue(GameRules rules, GameRules.Key<GameRules.BooleanValue> rule,
			MinecraftServer server) {
		rules.getRule(rule).set(true, server);
	}

	private static void createPlatform(ServerLevel level) {
		for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
			for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
				BlockPos position = PLATFORM_CENTER.offset(x, 0, z);
				level.setBlock(position, Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	private static void disablePlayerSystems(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(variables -> {
			variables.Player = false;
			variables.CustomHUD = false;
			variables.combatmode = false;
			variables.questinfo = false;
			variables.ActiveDaily = false;
			variables.isdailytraining = false;
			variables.istraining = false;
			variables.dungeoning = false;
			variables.Fatigue = 0;
			variables.syncPlayerVariables(player);
		});
	}

	private static void giveBuilderKit(ServerPlayer player) {
		giveIfMissing(player, SololevelingModItems.DUNGEON_SURVEYOR_WAND.get());
		giveIfMissing(player, SololevelingModItems.DUNGEON_SOCKET_WAND.get());
		giveIfMissing(player, SololevelingModItems.DUNGEON_ENCOUNTER_WAND.get());
		giveIfMissing(player, SololevelingModItems.DUNGEON_FEATURE_WAND.get());
		giveIfMissing(player, SololevelingModItems.DUNGEON_BUILDER_WAND.get());
	}

	private static void giveIfMissing(ServerPlayer player, Item item) {
		ItemStack stack = new ItemStack(item);
		boolean alreadyOwned = false;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(item)) {
				alreadyOwned = true;
				break;
			}
		}
		if (!alreadyOwned && !player.getInventory().add(stack))
			player.drop(stack, false);
	}

	private static void showWelcome(ServerPlayer player) {
		if (player.getPersistentData().getBoolean("slr_dungeon_builder_welcome"))
			return;
		player.getPersistentData().putBoolean("slr_dungeon_builder_welcome", true);
		player.sendSystemMessage(Component.literal("Dungeon Builder ready").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		player.sendSystemMessage(Component.literal("Your five architect wands were added. The live checklist is on the right; run /dungeonbuilder tutorial for a three-room walkthrough.").withStyle(ChatFormatting.GRAY));
		if (!player.hasPermissions(2) && !player.getServer().isSingleplayerOwner(player.getGameProfile()))
			player.sendSystemMessage(Component.literal("An operator must grant you permission before the wands can edit or export projects.").withStyle(ChatFormatting.YELLOW));
	}

	private static final class DungeonBuilderSavedData extends SavedData {
		private static final String DATA_NAME = "sololeveling_dungeon_builder";
		private boolean platformCreated;
		private boolean defaultsConfigured;

		private static DungeonBuilderSavedData get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(
					DungeonBuilderSavedData::load, DungeonBuilderSavedData::new, DATA_NAME);
		}

		private boolean platformCreated() {
			return platformCreated;
		}

		private void markPlatformCreated() {
			platformCreated = true;
			setDirty();
		}

		private boolean defaultsConfigured() {
			return defaultsConfigured;
		}

		private void markDefaultsConfigured() {
			defaultsConfigured = true;
			setDirty();
		}

		@Nonnull
		@Override
		public CompoundTag save(@Nonnull CompoundTag tag) {
			tag.putBoolean("PlatformCreated", platformCreated);
			tag.putBoolean("DefaultsConfigured", defaultsConfigured);
			return tag;
		}

		private static DungeonBuilderSavedData load(CompoundTag tag) {
			DungeonBuilderSavedData data = new DungeonBuilderSavedData();
			data.platformCreated = tag.getBoolean("PlatformCreated");
			data.defaultsConfigured = tag.getBoolean("DefaultsConfigured");
			return data;
		}
	}
}
