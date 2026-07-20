package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.DungeonBuilderClientState;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.dungeon.builder.DungeonBuilderProjectData;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Small server-authoritative snapshot used only by the Dungeon Builder HUD. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DungeonBuilderStatusMessage {
	public static final int OK = 0;
	public static final int TODO = 1;
	public static final int ERROR = 2;
	public static final int INFO = 3;
	public static final int WARNING = 4;
	private static final int MAX_LINES = 12;

	private final View view;

	public DungeonBuilderStatusMessage(View view) {
		this.view = view == null ? View.inactive() : view;
	}

	public DungeonBuilderStatusMessage(FriendlyByteBuf buffer) {
		boolean active = buffer.readBoolean();
		if (!active) {
			view = View.inactive();
			return;
		}
		String projectId = buffer.readUtf(128);
		String type = buffer.readUtf(64);
		String ranks = buffer.readUtf(32);
		String group = buffer.readUtf(64);
		String pool = buffer.readUtf(128);
		String bounds = buffer.readUtf(128);
		String pending = buffer.readUtf(96);
		int regionCount = buffer.readVarInt();
		int socketCount = buffer.readVarInt();
		int markerCount = buffer.readVarInt();
		int encounterCount = buffer.readVarInt();
		int errors = buffer.readVarInt();
		int warnings = buffer.readVarInt();
		int lineCount = Math.min(MAX_LINES, Math.max(0, buffer.readVarInt()));
		List<StatusLine> lines = new ArrayList<>(lineCount);
		for (int index = 0; index < lineCount; index++)
			lines.add(new StatusLine(buffer.readUnsignedByte(), buffer.readUtf(64), buffer.readUtf(192)));
		view = new View(true, projectId, type, ranks, group, pool, bounds, pending,
				regionCount, socketCount, markerCount, encounterCount, errors, warnings, lines);
	}

	public static void buffer(DungeonBuilderStatusMessage message, FriendlyByteBuf buffer) {
		View view = message.view;
		buffer.writeBoolean(view.active());
		if (!view.active())
			return;
		buffer.writeUtf(view.projectId(), 128);
		buffer.writeUtf(view.type(), 64);
		buffer.writeUtf(view.ranks(), 32);
		buffer.writeUtf(view.group(), 64);
		buffer.writeUtf(view.pool(), 128);
		buffer.writeUtf(view.bounds(), 128);
		buffer.writeUtf(view.pending(), 96);
		buffer.writeVarInt(view.regionCount());
		buffer.writeVarInt(view.socketCount());
		buffer.writeVarInt(view.markerCount());
		buffer.writeVarInt(view.encounterCount());
		buffer.writeVarInt(view.errors());
		buffer.writeVarInt(view.warnings());
		List<StatusLine> lines = view.lines().stream().limit(MAX_LINES).toList();
		buffer.writeVarInt(lines.size());
		for (StatusLine line : lines) {
			buffer.writeByte(line.status());
			buffer.writeUtf(line.label(), 64);
			buffer.writeUtf(line.detail(), 192);
		}
	}

	public static void handler(DungeonBuilderStatusMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> DungeonBuilderClientState.update(message.view)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(DungeonBuilderStatusMessage.class,
				DungeonBuilderStatusMessage::buffer, DungeonBuilderStatusMessage::new,
				DungeonBuilderStatusMessage::handler);
	}

	public static DungeonBuilderStatusMessage inactive() {
		return new DungeonBuilderStatusMessage(View.inactive());
	}

	public static DungeonBuilderStatusMessage from(DungeonBuilderProjectData.Project project) {
		if (project == null)
			return inactive();
		List<DungeonBuilderProjectData.Issue> validation = project.validate();
		int errors = (int) validation.stream()
				.filter(issue -> issue.severity() == DungeonBuilderProjectData.Severity.ERROR).count();
		int warnings = validation.size() - errors;
		long roomRegions = project.regions().stream().filter(region -> region.type().equals("room")).count();
		long spawnMarkers = project.markers().stream().filter(marker -> marker.type().equals("mob_spawn")
				|| marker.type().equals("elite_spawn")).count();
		long unassignedSpawns = project.markers().stream().filter(marker -> marker.type().equals("spawn_point")).count();
		long bossMarkers = project.markers().stream().filter(marker -> marker.type().equals("boss_spawn")).count();
		long triggerRegions = project.regions().stream().filter(region -> region.type().equals("trigger_region")).count();
		long outside = validation.stream().filter(issue -> issue.message().toLowerCase(Locale.ROOT)
				.contains("outside the structure bounds")).count();

		List<StatusLine> lines = new ArrayList<>();
		DungeonBuilderProjectData.Bounds structure = project.structureBounds();
		String bounds;
		if (structure == null) {
			bounds = "not selected";
			lines.add(line(ERROR, "Structure", "Select two outer corners with the Surveyor Wand."));
		} else {
			BlockPos size = structure.size();
			bounds = size.getX() + "x" + size.getY() + "x" + size.getZ() + " at "
					+ structure.min().toShortString();
			lines.add(line(OK, "Structure", size.getX() + " x " + size.getY() + " x " + size.getZ()));
		}
		lines.add(project.roomSnapshot().isPresent()
				? line(OK, "Snapshot", "Captured; update explicitly after block edits.")
				: line(TODO, "Snapshot", "Press N and choose Capture Room."));
		lines.add(roomRegions == 1
				? line(OK, "Room Bounds", "One walkable volume selected.")
				: line(ERROR, "Room Bounds", roomRegions == 0
						? "Select the inside of this one room."
						: roomRegions + " volumes found; select again to replace them."));
		if (outside > 0)
			lines.add(line(ERROR, "Outside bounds", outside + " saved element" + (outside == 1 ? " is" : "s are")
					+ " outside this project's structure."));

		if (project.kind() == DungeonBuilderProjectData.ProjectKind.MODULE) {
			int sockets = project.sockets().size();
			switch (project.roomRole()) {
				case START, BOSS -> lines.add(sockets == 1
						? line(OK, "Sockets", "Exactly one doorway connector.")
						: line(ERROR, "Sockets", "This role needs exactly 1; currently " + sockets + "."));
				case NORMAL, CORRIDOR -> lines.add(sockets >= 2
						? line(OK, "Sockets", sockets + " connectors (entrance + exit).")
						: line(ERROR, "Sockets", "Add an entrance and exit; currently " + sockets + "."));
				default -> lines.add(sockets > 0
						? line(OK, "Sockets", sockets + " connector" + (sockets == 1 ? "" : "s") + ".")
						: line(ERROR, "Sockets", "Add at least one connector."));
			}
			if (project.roomRole() == DungeonBuilderProjectData.RoomRole.START) {
				lines.add(marker(project, "player_start", "Player Start", "Mark where players arrive."));
				boolean exit = hasMarker(project, "exit") || hasMarker(project, "return_portal");
				lines.add(exit ? line(OK, "Return Portal", "Return position saved.")
						: line(ERROR, "Return Portal", "Place it in the start room with the Feature Wand."));
			} else if (project.roomRole() == DungeonBuilderProjectData.RoomRole.BOSS) {
				lines.add(bossMarkers > 0 ? line(OK, "Boss Spawn", bossMarkers + " position saved.")
						: unassignedSpawns > 0
								? line(TODO, "Boss Spawn", "Assign a generic point as BOSS in Studio.")
								: line(ERROR, "Boss Spawn", "Place a point with the Encounter Wand."));
				lines.add(triggerRegions > 0 ? line(INFO, "Boss Activation", "Delayed trigger volume saved.")
						: line(INFO, "Boss Activation", "Automatic; a trigger is optional."));
			} else {
				lines.add(spawnMarkers > 0 ? line(OK, "Mob Spawns", spawnMarkers + " position" + (spawnMarkers == 1 ? "" : "s") + ".")
						: unassignedSpawns > 0
								? line(TODO, "Spawn Points", unassignedSpawns + " waiting for Studio assignment.")
								: line(TODO, "Mob Spawns", "Optional: place generic spawn points."));
				lines.add(triggerRegions > 0 ? line(INFO, "Activation", "A delayed trigger volume is available.")
						: line(INFO, "Activation", "Automatic; no trigger is required."));
			}
		} else {
			lines.add(marker(project, "player_start", "Player Start", "Mark where players arrive."));
			boolean exit = hasMarker(project, "exit") || hasMarker(project, "return_portal");
			lines.add(exit ? line(OK, "Return Portal", "Return position saved.")
					: line(ERROR, "Return Portal", "Place an Exit or Return Portal."));
			lines.add(bossMarkers > 0 ? line(OK, "Boss Spawn", bossMarkers + " position saved.")
					: unassignedSpawns > 0
							? line(TODO, "Boss Spawn", "Assign a generic point as BOSS in Studio.")
							: line(ERROR, "Boss Spawn", "Preset dungeons need a boss position."));
		}

		lines.add(errors == 0 ? line(OK, "Validation", warnings == 0 ? "Ready to export." : warnings + " warning(s).")
				: line(ERROR, "Validation", errors + " error(s), " + warnings + " warning(s)."));
		for (DungeonBuilderProjectData.Issue issue : validation) {
			if (lines.size() >= MAX_LINES)
				break;
			if (issue.message().toLowerCase(Locale.ROOT).contains("outside the structure bounds"))
				continue;
			lines.add(line(issue.severity() == DungeonBuilderProjectData.Severity.ERROR ? ERROR : WARNING,
					issue.severity() == DungeonBuilderProjectData.Severity.ERROR ? "Fix" : "Warning",
					issue.message()));
		}

		String type = project.kind().name().toLowerCase(Locale.ROOT);
		if (project.kind() == DungeonBuilderProjectData.ProjectKind.MODULE)
			type += " / " + project.roomRole().name().toLowerCase(Locale.ROOT);
		String ranks = Arrays.stream(ProceduralDungeonRank.values()).filter(project.allowedRanks()::contains)
				.map(Enum::name).collect(Collectors.joining(","));
		String group = project.activeEncounterGroup();
		String pool = project.encounters().stream().filter(encounter -> encounter.id().equals(group))
				.map(DungeonBuilderProjectData.Encounter::pool).findFirst().orElse(project.defaultMobPool());
		String pending = project.pendingPosition() == null ? "" : "First corner: " + project.pendingPosition().toShortString();
		View view = new View(true, project.id(), type, ranks, group, pool, bounds, pending,
				project.regions().size(), project.sockets().size(), project.markers().size(),
				project.encounters().size(), errors, warnings, lines);
		return new DungeonBuilderStatusMessage(view);
	}

	private static boolean hasMarker(DungeonBuilderProjectData.Project project, String type) {
		return project.markers().stream().anyMatch(marker -> marker.type().equals(type));
	}

	private static StatusLine marker(DungeonBuilderProjectData.Project project, String type,
			String label, String missing) {
		return hasMarker(project, type) ? line(OK, label, "Position saved.") : line(ERROR, label, missing);
	}

	private static StatusLine line(int status, String label, String detail) {
		return new StatusLine(status, label, detail);
	}

	public View view() {
		return view;
	}

	public record StatusLine(int status, String label, String detail) {
		public StatusLine {
			status = Math.max(OK, Math.min(WARNING, status));
			label = limit(label, 64);
			detail = limit(detail, 192);
		}
	}

	public record View(boolean active, String projectId, String type, String ranks,
			String group, String pool, String bounds, String pending, int regionCount,
			int socketCount, int markerCount, int encounterCount, int errors, int warnings,
			List<StatusLine> lines) {
		public View {
			projectId = limit(projectId, 128);
			type = limit(type, 64);
			ranks = limit(ranks, 32);
			group = limit(group, 64);
			pool = limit(pool, 128);
			bounds = limit(bounds, 128);
			pending = limit(pending, 96);
			lines = lines == null ? List.of() : List.copyOf(lines);
		}

		public static View inactive() {
			return new View(false, "", "", "", "", "", "", "",
					0, 0, 0, 0, 0, 0, List.of());
		}
	}

	private static String limit(String value, int maximum) {
		String safe = value == null ? "" : value;
		return safe.length() <= maximum ? safe : safe.substring(0, maximum);
	}
}
