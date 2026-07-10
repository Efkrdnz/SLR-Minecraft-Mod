package net.solocraft.dungeon;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.Portal1Entity;
import net.solocraft.guild.GuildGateHelper;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.UrgentQuestManager;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProceduralDungeonGateHandler {
	private static final String PROCEDURAL_GATE = "slr_procedural_gate";
	private static final String PROCEDURAL_RED = "slr_procedural_red_gate";
	private static final String RANK = "slr_procedural_rank";
	private static final String THEME = "slr_procedural_theme";
	private static final String COMPLEXITY = "slr_procedural_complexity";
	private static final String GENERATED = "slr_procedural_generated";
	private static final String START_X = "slr_procedural_start_x";
	private static final String START_Y = "slr_procedural_start_y";
	private static final String START_Z = "slr_procedural_start_z";
	private static final String PROCEDURAL_DUNGEON = "slr_procedural_dungeon";
	private static final float RED_GATE_CHANCE = 0.02F;

	private ProceduralDungeonGateHandler() {
	}

	public static boolean isProceduralGate(Entity gate) {
		return gate != null && gate.getPersistentData().getBoolean(PROCEDURAL_GATE);
	}

	public static boolean isProceduralRedGate(Entity gate) {
		return gate != null && gate.getPersistentData().getBoolean(PROCEDURAL_RED);
	}

	public static void enter(LevelAccessor world, double x, double y, double z, Entity gate, Entity sourceentity) {
		if (gate == null || sourceentity == null)
			return;
		if (isMagicReader(sourceentity)) {
			showMagicReading(gate, sourceentity);
			return;
		}
		if (GuildGateHelper.prepareGateEntry(world, gate, sourceentity))
			return;
		if (!(sourceentity instanceof ServerPlayer player))
			return;
		if (isProceduralRedGate(gate) && isLocked(gate))
			return;
		boolean turnsRed = !gate.getPersistentData().getBoolean(GENERATED) && shouldTurnRed(world);
		if (turnsRed)
			turnRed(world, gate);
		List<ServerPlayer> entrants = turnsRed ? nearbyPartyMembers(world, gate, player) : List.of(player);
		for (ServerPlayer entrant : entrants)
			prepareEntrant(world, x, y, z, gate, entrant);
		Runnable teleport = () -> teleportEntrants(gate, entrants);
		if (turnsRed) {
			for (ServerPlayer entrant : entrants)
				showRedGateTitle(entrant);
			SololevelingMod.queueServerWork(10, teleport);
		} else {
			teleport.run();
		}
	}

	private static boolean shouldTurnRed(LevelAccessor world) {
		return !SololevelingModVariables.MapVariables.get(world).RedGate && RandomSource.create().nextFloat() < RED_GATE_CHANCE;
	}

	private static boolean isLocked(Entity gate) {
		return gate instanceof Portal1Entity portal && portal.getEntityData().get(Portal1Entity.DATA_usedbefore);
	}

	private static void turnRed(LevelAccessor world, Entity gate) {
		gate.getPersistentData().putBoolean(PROCEDURAL_RED, true);
		gate.getPersistentData().putBoolean("slr_is_red_gate", true);
		gate.getPersistentData().putString(THEME, DungeonTheme.ICE.name());
		if (gate instanceof Portal1Entity portal) {
			portal.getEntityData().set(Portal1Entity.DATA_usedbefore, true);
			portal.setTexture("21");
		}
		SololevelingModVariables.MapVariables.get(world).RedGate = true;
		SololevelingModVariables.MapVariables.get(world).syncData(world);
	}

	private static List<ServerPlayer> nearbyPartyMembers(LevelAccessor world, Entity gate, ServerPlayer player) {
		String party = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).party;
		if (party.equals(""))
			return List.of(player);
		List<ServerPlayer> entrants = new ArrayList<>();
		for (Entity candidate : new ArrayList<>(world.players())) {
			if (!(candidate instanceof ServerPlayer partyMember))
				continue;
			String candidateParty = partyMember.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).party;
			if (party.equals(candidateParty) && partyMember.distanceTo(gate) <= 10)
				entrants.add(partyMember);
		}
		return entrants.isEmpty() ? List.of(player) : entrants;
	}

	private static void prepareEntrant(LevelAccessor world, double x, double y, double z, Entity gate, ServerPlayer entrant) {
		dismissOwnedShadows(world, x, y, z, entrant);
		saveReturnPosition(entrant);
		entrant.getPersistentData().putString("dungeon_tag", gate.getStringUUID());
		entrant.getPersistentData().putBoolean(PROCEDURAL_DUNGEON, true);
		entrant.getPersistentData().putBoolean(PROCEDURAL_RED, isProceduralRedGate(gate));
		UrgentQuestManager.markDungeonId(entrant, isProceduralRedGate(gate) ? "red_gate" : "procedural");
		entrant.setNoGravity(true);
	}

	private static void showRedGateTitle(ServerPlayer player) {
		if (player.level().isClientSide() || player.getServer() == null)
			return;
		player.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, player.position(), player.getRotationVector(), player.serverLevel(), 4, player.getName().getString(), player.getDisplayName(),
				player.getServer(), player), "/title @s title [\"\",{\"text\":\"Red Gate?\",\"color\":\"red\"}]");
	}

	private static void teleportEntrants(Entity gate, List<ServerPlayer> entrants) {
		ResourceKey<Level> destination = destinationFor(gate);
		ServerPlayer firstPlayer = entrants.stream().filter(entrant -> entrant != null && !entrant.level().isClientSide()).findFirst().orElse(null);
		if (firstPlayer == null)
			return;
		ServerLevel nextLevel = firstPlayer.server.getLevel(destination);
		if (nextLevel == null)
			return;

		BlockPos targetPos = storedTarget(gate);
		for (ServerPlayer entrant : entrants)
			entrant.teleportTo(nextLevel, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, entrant.getYRot(), entrant.getXRot());
		SololevelingMod.queueServerWork(5, () -> {
			if (!gate.getPersistentData().getBoolean(GENERATED)) {
				ProceduralDungeonResult result = ProceduralDungeonGenerator.generate(nextLevel, targetPos, settingsFor(gate), firstPlayer);
				gate.getPersistentData().putBoolean(GENERATED, true);
				gate.getPersistentData().putDouble(START_X, result.startPos.getX() + 0.5);
				gate.getPersistentData().putDouble(START_Y, result.startPos.getY());
				gate.getPersistentData().putDouble(START_Z, result.startPos.getZ() + 0.5);
			}
			double startX = gate.getPersistentData().getDouble(START_X);
			double startY = gate.getPersistentData().getDouble(START_Y);
			double startZ = gate.getPersistentData().getDouble(START_Z);
			for (ServerPlayer entrant : entrants) {
				if (entrant.level().dimension() == destination)
					entrant.connection.teleport(startX, startY, startZ, entrant.getYRot(), entrant.getXRot());
			}
		});
	}

	private static boolean isMagicReader(Entity entity) {
		return (entity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.MAGIC_READER.get();
	}

	private static void showMagicReading(Entity gate, Entity sourceentity) {
		if (!(sourceentity instanceof Player player) || player.level().isClientSide())
			return;
		if (isProceduralRedGate(gate)) {
			String[] readings = {"9999", "ERROR", "N/A", "Cannot Read!"};
			player.displayClientMessage(Component.literal("Magic Reading: " + readings[Mth.nextInt(RandomSource.create(), 0, readings.length - 1)]), false);
			return;
		}
		ProceduralDungeonRank rank = rankFor(gate);
		int min = switch (rank) {
			case E -> 101;
			case D -> 201;
			case C -> 401;
			case B -> 601;
			case A -> 801;
			case S -> 1001;
		};
		int max = switch (rank) {
			case E -> 199;
			case D -> 399;
			case C -> 599;
			case B -> 799;
			case A -> 999;
			case S -> 1499;
		};
		player.displayClientMessage(Component.literal("Magic Reading: " + Mth.nextInt(RandomSource.create(), min, max)), false);
	}

	private static void dismissOwnedShadows(LevelAccessor world, double x, double y, double z, Entity sourceentity) {
		final Vec3 center = new Vec3(x, y, z);
		List<Entity> found = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(250), e -> true).stream().sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center))).toList();
		for (Entity entity : found) {
			if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))
					&& (entity instanceof TamableAnimal tame && sourceentity instanceof LivingEntity owner && tame.isOwnedBy(owner))) {
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
	}

	private static void saveReturnPosition(Entity entity) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = entity.getX();
			capability.DunY = entity.getY();
			capability.DunZ = entity.getZ();
			capability.dungeoning = true;
			capability.BossKilled = false;
			capability.syncPlayerVariables(entity);
		});
	}

	private static BlockPos storedTarget(Entity gate) {
		return BlockPos.containing(gate.getPersistentData().getDouble("tpx"), gate.getPersistentData().getDouble("tpy"), gate.getPersistentData().getDouble("tpz"));
	}

	private static ProceduralDungeonSettings settingsFor(Entity gate) {
		return new ProceduralDungeonSettings(rankFor(gate), DungeonTheme.fromString(gate.getPersistentData().getString(THEME)), gate.getPersistentData().getInt(COMPLEXITY));
	}

	private static ProceduralDungeonRank rankFor(Entity gate) {
		return ProceduralDungeonRank.fromString(gate.getPersistentData().getString(RANK));
	}

	private static ResourceKey<Level> destinationFor(Entity gate) {
		if (isProceduralRedGate(gate))
			return ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow"));
		return switch (rankFor(gate)) {
			case E, D -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_d"));
			case C -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_c"));
			case B -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_b"));
			case A -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_a"));
			case S -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_s"));
		};
	}
}
