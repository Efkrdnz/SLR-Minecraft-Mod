package net.solocraft.api.vessel;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import net.solocraft.SololevelingMod;

/**
 * Reads Monarch presentations from {@code data/<namespace>/sololeveling/vessels}.
 *
 * <p>The vessel itself is registered in code, because identity has to exist
 * before a data file can point at it. This file only decides how it looks.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class VesselPresentationLoader extends SimpleJsonResourceReloadListener {
	public static final String DIRECTORY = "sololeveling/vessels";

	public VesselPresentationLoader() {
		super(new com.google.gson.Gson(), DIRECTORY);
	}

	@SubscribeEvent
	public static void onAddReloadListener(AddReloadListenerEvent event) {
		event.addListener(new VesselPresentationLoader());
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
			ProfilerFiller profiler) {
		Map<ResourceLocation, VesselPresentation> loaded = new LinkedHashMap<>();

		for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			try {
				VesselPresentation presentation = parse(
						GsonHelper.convertToJsonObject(file.getValue(), "vessel"));
				Vessel vessel = VesselRegistry.byId(presentation.vesselId()).orElse(null);
				if (vessel == null) {
					SololevelingMod.LOGGER.error(
							"Vessel presentation {} names {}, which no mod registered",
							file.getKey(), presentation.vesselId());
					continue;
				}
				// Rulers present identically to each other on purpose; that sameness
				// is what separates the two columns. Saying so beats a file that
				// looks correct and changes nothing.
				if (vessel.kind() != Vessel.Kind.MONARCH) {
					SololevelingMod.LOGGER.error(
							"Vessel presentation {} names the Ruler {}; Rulers all "
									+ "present alike by design and cannot be themed",
							file.getKey(), presentation.vesselId());
					continue;
				}
				loaded.put(presentation.vesselId(), presentation);
			} catch (JsonParseException | IllegalArgumentException exception) {
				SololevelingMod.LOGGER.error("Skipping vessel presentation {}: {}",
						file.getKey(), exception.getMessage());
			}
		}

		VesselRegistry.replacePresentations(loaded);
		if (!loaded.isEmpty())
			SololevelingMod.LOGGER.info("Loaded {} contributed Monarch presentation(s)",
					loaded.size());
	}

	private static VesselPresentation parse(JsonObject json) {
		String rawVessel = GsonHelper.getAsString(json, "vessel");
		ResourceLocation vesselId = ResourceLocation.tryParse(rawVessel);
		if (vesselId == null)
			throw new IllegalArgumentException("vessel \"" + rawVessel + "\" is not a valid id");

		int color = color(GsonHelper.getAsString(json, "color", ""));
		VesselPresentation.Backdrop backdrop = VesselPresentation.Backdrop.parse(
				GsonHelper.getAsString(json, "backdrop", ""));
		return new VesselPresentation(vesselId, color, backdrop);
	}

	/** Accepts {@code #RRGGBB}, {@code RRGGBB}, or a plain number. */
	private static int color(String raw) {
		String value = raw == null ? "" : raw.trim();
		if (value.isEmpty())
			return VesselPresentation.DEFAULT_COLOR;
		try {
			int parsed = value.startsWith("#")
					? Integer.parseInt(value.substring(1), 16)
					: Integer.parseInt(value, value.length() > 6 ? 10 : 16);
			return (parsed & 0xFF000000) == 0 ? 0xFF000000 | parsed : parsed;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("color \"" + raw + "\" is not a hex colour");
		}
	}
}
