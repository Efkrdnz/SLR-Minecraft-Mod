package net.solocraft.api.hunter;

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
 * Reads class presentations from {@code data/<namespace>/sololeveling/classes}.
 *
 * <p>A class is registered in code -- identity has to exist before an ability
 * file can point at it -- and describes itself here. Shipping one of these files
 * is also what makes the class rollable by the Evaluator.
 *
 * <pre>
 * {
 *   "class": "yourmod:necromancer",
 *   "description": "Authority borrowed from what is already buried.",
 *   "color": "#8FE3B0"
 * }
 * </pre>
 *
 * <p>A file naming a class nobody registered is reported and skipped. Silently
 * ignoring it would leave an author with a class that never appears and no clue
 * why.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class HunterClassLoader extends SimpleJsonResourceReloadListener {
	public static final String DIRECTORY = "sololeveling/classes";

	public HunterClassLoader() {
		super(new com.google.gson.Gson(), DIRECTORY);
	}

	@SubscribeEvent
	public static void onAddReloadListener(AddReloadListenerEvent event) {
		event.addListener(new HunterClassLoader());
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
			ProfilerFiller profiler) {
		Map<ResourceLocation, HunterClassPresentation> loaded = new LinkedHashMap<>();

		for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			try {
				HunterClassPresentation presentation = parse(
						GsonHelper.convertToJsonObject(file.getValue(), "class"));
				if (HunterClassRegistry.byId(presentation.classId()).isEmpty()) {
					SololevelingMod.LOGGER.error(
							"Class presentation {} names {}, which no mod registered",
							file.getKey(), presentation.classId());
					continue;
				}
				loaded.put(presentation.classId(), presentation);
			} catch (JsonParseException | IllegalArgumentException exception) {
				SololevelingMod.LOGGER.error("Skipping class presentation {}: {}",
						file.getKey(), exception.getMessage());
			}
		}

		HunterClassRegistry.replacePresentations(loaded);
		if (!loaded.isEmpty())
			SololevelingMod.LOGGER.info("Loaded {} contributed class presentation(s); "
					+ "the Evaluator can now draw them", loaded.size());
	}

	private static HunterClassPresentation parse(JsonObject json) {
		String rawClass = GsonHelper.getAsString(json, "class");
		ResourceLocation classId = ResourceLocation.tryParse(rawClass);
		if (classId == null)
			throw new IllegalArgumentException("class \"" + rawClass + "\" is not a valid id");

		String description = GsonHelper.getAsString(json, "description");
		int color = color(GsonHelper.getAsString(json, "color", ""));
		return new HunterClassPresentation(classId, description, color);
	}

	/** Accepts {@code #RRGGBB}, {@code RRGGBB}, or a plain number. */
	private static int color(String raw) {
		String value = raw == null ? "" : raw.trim();
		if (value.isEmpty())
			return HunterClassPresentation.DEFAULT_COLOR;
		try {
			int parsed = value.startsWith("#")
					? Integer.parseInt(value.substring(1), 16)
					: Integer.parseInt(value, value.length() > 6 ? 10 : 16);
			// Opaque unless the file said otherwise, so a six-digit colour is not
			// silently invisible.
			return (parsed & 0xFF000000) == 0 ? 0xFF000000 | parsed : parsed;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("color \"" + raw + "\" is not a hex colour");
		}
	}
}
