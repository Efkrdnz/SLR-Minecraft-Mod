package net.solocraft.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import net.solocraft.SololevelingMod;
import net.solocraft.api.AbilityCost;
import net.solocraft.api.hunter.HunterClassPresentation;
import net.solocraft.api.hunter.HunterClassRegistry;
import net.solocraft.api.skill.HunterAbility;
import net.solocraft.api.skill.HunterAbilityRegistry;
import net.solocraft.api.vessel.VesselPresentation;
import net.solocraft.api.vessel.VesselRegistry;
import net.solocraft.network.compat.NetworkEvent;
import net.solocraft.network.compat.PacketDistributor;

/**
 * Mirrors datapack ability definitions onto the client.
 *
 * <p>Data packs load on the server only, but an ability's name, colour, and
 * tooltip are all drawn on the client. Without this a contributed ability on a
 * dedicated server would show an unmarked name and an empty tooltip, while
 * working perfectly in singleplayer -- where the integrated server hides the
 * problem.
 *
 * <p>Only the description travels. The executor stays server-side, because the
 * client never decides that an ability fired.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class AbilityDefinitionSyncMessage {
	/** A guard against a pack with a runaway generator, not a design limit. */
	private static final int MAX_DEFINITIONS = 512;

	private static final int MAX_NAME = 128;
	private static final int MAX_TEXT = 512;
	private static final int MAX_ID = 256;

	private final List<HunterAbility> definitions;
	private final List<HunterClassPresentation> classes;
	private final List<VesselPresentation> vessels;

	public AbilityDefinitionSyncMessage(List<HunterAbility> definitions,
			List<HunterClassPresentation> classes, List<VesselPresentation> vessels) {
		this.definitions = definitions == null ? List.of() : definitions;
		this.classes = classes == null ? List.of() : classes;
		this.vessels = vessels == null ? List.of() : vessels;
	}

	public AbilityDefinitionSyncMessage(FriendlyByteBuf buffer) {
		int count = Math.min(buffer.readVarInt(), MAX_DEFINITIONS);
		List<HunterAbility> read = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			ResourceLocation id = ResourceLocation.parse(buffer.readUtf(MAX_ID));
			String name = buffer.readUtf(MAX_NAME);
			String summary = buffer.readUtf(MAX_TEXT);
			String detail = buffer.readUtf(MAX_TEXT);
			ChatFormatting accent = readAccent(buffer.readUtf(MAX_NAME));
			AbilityCost cost = readCost(buffer.readUtf(MAX_NAME));
			int cooldown = buffer.readVarInt();
			String owner = buffer.readUtf(MAX_ID);
			ResourceLocation owningClass = owner.isEmpty() ? null : ResourceLocation.tryParse(owner);
			HunterAbility.Mode mode = readMode(buffer.readUtf(MAX_NAME));
			int upkeep = buffer.readVarInt();
			// The HUD is client-side, so the slot texture has to come across with the
			// rest of the declaration or the slot would draw an empty frame.
			String rawIcon = buffer.readUtf(MAX_ID);
			ResourceLocation icon = rawIcon.isEmpty() ? null : ResourceLocation.tryParse(rawIcon);
			read.add(new HunterAbility(id, name, summary, detail, accent, cost, cooldown,
					owningClass, mode, upkeep, icon));
		}
		this.definitions = List.copyOf(read);

		int classCount = Math.min(buffer.readVarInt(), MAX_DEFINITIONS);
		List<HunterClassPresentation> readClasses = new ArrayList<>(classCount);
		for (int i = 0; i < classCount; i++) {
			ResourceLocation classId = ResourceLocation.parse(buffer.readUtf(MAX_ID));
			String description = buffer.readUtf(MAX_TEXT);
			int colour = buffer.readInt();
			readClasses.add(new HunterClassPresentation(classId, description, colour));
		}
		this.classes = List.copyOf(readClasses);

		int vesselCount = Math.min(buffer.readVarInt(), MAX_DEFINITIONS);
		List<VesselPresentation> readVessels = new ArrayList<>(vesselCount);
		for (int i = 0; i < vesselCount; i++) {
			ResourceLocation vesselId = ResourceLocation.parse(buffer.readUtf(MAX_ID));
			int colour = buffer.readInt();
			readVessels.add(new VesselPresentation(vesselId, colour,
					readBackdrop(buffer.readUtf(MAX_NAME))));
		}
		this.vessels = List.copyOf(readVessels);
	}

	public static void buffer(AbilityDefinitionSyncMessage message, FriendlyByteBuf buffer) {
		List<HunterAbility> sending = message.definitions.size() > MAX_DEFINITIONS
				? message.definitions.subList(0, MAX_DEFINITIONS)
				: message.definitions;
		buffer.writeVarInt(sending.size());
		for (HunterAbility ability : sending) {
			buffer.writeUtf(ability.id().toString(), MAX_ID);
			buffer.writeUtf(ability.name(), MAX_NAME);
			buffer.writeUtf(ability.summary(), MAX_TEXT);
			buffer.writeUtf(ability.detail(), MAX_TEXT);
			buffer.writeUtf(ability.accent().getName(), MAX_NAME);
			buffer.writeUtf(ability.cost().name(), MAX_NAME);
			buffer.writeVarInt(ability.cooldownTicks());
			buffer.writeUtf(ability.isClassless() ? "" : ability.owningClass().toString(), MAX_ID);
			buffer.writeUtf(ability.mode().name(), MAX_NAME);
			buffer.writeVarInt(ability.upkeepPerSecond());
			buffer.writeUtf(ability.hasIcon() ? ability.icon().toString() : "", MAX_ID);
		}

		buffer.writeVarInt(message.classes.size());
		for (HunterClassPresentation presentation : message.classes) {
			buffer.writeUtf(presentation.classId().toString(), MAX_ID);
			buffer.writeUtf(presentation.description(), MAX_TEXT);
			buffer.writeInt(presentation.color());
		}

		buffer.writeVarInt(message.vessels.size());
		for (VesselPresentation presentation : message.vessels) {
			buffer.writeUtf(presentation.vesselId().toString(), MAX_ID);
			buffer.writeInt(presentation.color());
			buffer.writeUtf(presentation.backdrop().name(), MAX_NAME);
		}
	}

	public static void handler(AbilityDefinitionSyncMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			// Replace rather than merge: this is the server's complete list, and a
			// definition removed by a reload has to disappear here too.
			// Presentation from the message, behaviour kept from whatever is already
			// registered. A clear-and-re-register here also runs against the
			// integrated server in single-player, which silently stripped every
			// executor the datapack had just supplied.
			HunterAbilityRegistry.replaceDataDefinitions(message.definitions);

			// Class presentations ride the same message: both come from data packs
			// and both are needed before the client draws anything.
			Map<ResourceLocation, HunterClassPresentation> presentations =
				new LinkedHashMap<>();
			for (HunterClassPresentation presentation : message.classes)
				presentations.put(presentation.classId(), presentation);
			HunterClassRegistry.replacePresentations(presentations);

			// The vessel selection screen is client-side, so a Monarch's colour and
			// backdrop have to arrive before it can draw either.
			Map<ResourceLocation, VesselPresentation> vesselThemes = new LinkedHashMap<>();
			for (VesselPresentation presentation : message.vessels)
				vesselThemes.put(presentation.vesselId(), presentation);
			VesselRegistry.replacePresentations(vesselThemes);
		});
		context.setPacketHandled(true);
	}

	/**
	 * Sent on login and after every reload, which is exactly when the server's
	 * list can differ from what a client already has.
	 */
	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		AbilityDefinitionSyncMessage message = new AbilityDefinitionSyncMessage(
				HunterAbilityRegistry.dataDefinitions(),
				HunterClassRegistry.presentations(),
				VesselRegistry.presentations());

		ServerPlayer target = event.getPlayer();
		if (target != null) {
			SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> target), message);
			return;
		}
		for (ServerPlayer player : event.getPlayerList().getPlayers())
			SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), message);
	}

	private static VesselPresentation.Backdrop readBackdrop(String raw) {
		try {
			return VesselPresentation.Backdrop.valueOf(raw);
		} catch (IllegalArgumentException exception) {
			return VesselPresentation.Backdrop.SHADOW;
		}
	}

	private static HunterAbility.Mode readMode(String raw) {
		try {
			return HunterAbility.Mode.valueOf(raw);
		} catch (IllegalArgumentException exception) {
			return HunterAbility.Mode.INSTANT;
		}
	}

	private static ChatFormatting readAccent(String raw) {
		ChatFormatting accent = ChatFormatting.getByName(raw);
		return accent == null || !accent.isColor() ? ChatFormatting.WHITE : accent;
	}

	private static AbilityCost readCost(String raw) {
		try {
			return AbilityCost.valueOf(raw);
		} catch (IllegalArgumentException exception) {
			return AbilityCost.LOW;
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(AbilityDefinitionSyncMessage.class,
				AbilityDefinitionSyncMessage::buffer, AbilityDefinitionSyncMessage::new,
				AbilityDefinitionSyncMessage::handler);
	}
}
