package net.solocraft.dungeon;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.data.DungeonDataManager;
import net.solocraft.dungeon.data.DungeonDataSnapshot;
import net.solocraft.entity.DatapackGateEntity;
import net.solocraft.network.DatapackGateSelectionStateMessage;

import net.minecraftforge.network.PacketDistributor;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Server-authoritative bridge between a configurable datapack gate and its
 * client-only selection screen.
 */
public final class DatapackGateSelectionService {
	private static final double MAX_INTERACTION_DISTANCE_SQR = 8.0D * 8.0D;

	private DatapackGateSelectionService() {
	}

	/** Opens a fresh, server-filtered view for the gate the player interacted with. */
	public static void requestOpen(ServerPlayer player, DatapackGateEntity gate) {
		if (!validInteraction(player, gate)) {
			close(player, gate == null ? new UUID(0L, 0L) : gate.getUUID(),
					"This gate is no longer available.");
			return;
		}
		sendState(player, gate, "");
	}

	/**
	 * Accepts one selection from the screen. Every value is resolved again from
	 * live server state; the client snapshot is never treated as authority.
	 */
	public static void select(ServerPlayer player, UUID gateId, long expectedRevision,
			String dungeonText, String rankText) {
		if (player == null || gateId == null)
			return;

		Entity found = player.serverLevel().getEntity(gateId);
		if (!(found instanceof DatapackGateEntity gate) || !validInteraction(player, gate)) {
			close(player, gateId, "This gate is no longer close enough to configure.");
			return;
		}

		DungeonDataSnapshot snapshot = DungeonDataManager.snapshot();
		if (snapshot.revision() != expectedRevision) {
			sendState(player, gate, "Datapacks changed while this screen was open. Choose again.");
			return;
		}

		ResourceLocation dungeonId = ResourceLocation.tryParse(clean(dungeonText, 192));
		ProceduralDungeonRank rank = parseRank(rankText);
		if (dungeonId == null || rank == null) {
			sendState(player, gate, "That dungeon or rank selection is invalid.");
			return;
		}

		var definition = snapshot.dungeon(dungeonId);
		if (definition.isEmpty() || !definition.get().supportsRank(rank)) {
			sendState(player, gate, "That dungeon no longer supports the selected gate rank.");
			return;
		}

		DatapackDungeonGateHandler.BindingResult result =
				DatapackDungeonGateHandler.bindSelection(player, gate, dungeonId, rank, expectedRevision);
		if (!result.success()) {
			sendState(player, gate, result.message());
			return;
		}

		close(player, gateId, result.message());
		if (result.message() != null && !result.message().isBlank())
			player.displayClientMessage(Component.literal(result.message()), true);
	}

	private static void sendState(ServerPlayer player, DatapackGateEntity gate, String notice) {
		DungeonDataSnapshot snapshot = DungeonDataManager.snapshot();
		List<DatapackGateSelectionStateMessage.Option> options = new ArrayList<>();
		for (ResourceLocation id : snapshot.dungeonIds()) {
			if (options.size() >= DatapackGateSelectionStateMessage.MAX_OPTIONS)
				break;
			snapshot.dungeon(id).ifPresent(definition -> {
				List<String> ranks = definition.allowedRanks().stream()
						.sorted(Comparator.comparingInt(ProceduralDungeonRank::ordinal))
						.map(Enum::name)
						.toList();
				if (!ranks.isEmpty()) {
					options.add(new DatapackGateSelectionStateMessage.Option(
							id.toString(), definition.kind().name(), definition.roomCount().min(),
							definition.roomCount().max(), ranks));
				}
			});
		}

		String message = notice;
		if (options.isEmpty() && message.isBlank())
			message = "No valid datapack dungeons are currently loaded.";
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new DatapackGateSelectionStateMessage(true, gate.getUUID(), snapshot.revision(),
						options, message));
	}

	private static void close(ServerPlayer player, UUID gateId, String notice) {
		if (player == null)
			return;
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new DatapackGateSelectionStateMessage(false, gateId,
						DungeonDataManager.snapshot().revision(), List.of(), notice));
	}

	private static boolean validInteraction(ServerPlayer player, DatapackGateEntity gate) {
		return player != null && gate != null && !gate.isRemoved()
				&& player.level() == gate.level()
				&& player.distanceToSqr(gate) <= MAX_INTERACTION_DISTANCE_SQR;
	}

	private static ProceduralDungeonRank parseRank(String value) {
		String clean = clean(value, 8).toUpperCase(Locale.ROOT);
		for (ProceduralDungeonRank rank : ProceduralDungeonRank.values()) {
			if (rank.name().equals(clean))
				return rank;
		}
		return null;
	}

	private static String clean(String value, int maximum) {
		if (value == null)
			return "";
		String clean = value.replace('\u0000', ' ').trim();
		return clean.length() <= maximum ? clean : clean.substring(0, maximum);
	}
}
