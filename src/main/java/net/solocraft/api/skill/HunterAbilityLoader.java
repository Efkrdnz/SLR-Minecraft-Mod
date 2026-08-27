package net.solocraft.api.skill;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import net.solocraft.SololevelingMod;
import net.solocraft.api.AbilityCost;

/**
 * Reads ability definitions from {@code data/<namespace>/sololeveling/abilities}.
 *
 * <p>An ability is described entirely in JSON -- name, tooltip lines, colour,
 * cost band, cooldown, and the class it belongs to -- and names the class that
 * runs it. Description is data, behaviour is code, and neither is expressed in
 * the other's language.
 *
 * <pre>
 * {
 *   "name": "Cinder Slash",
 *   "summary": "A forward cut that leaves embers on the target.",
 *   "detail": "Burns for 3 seconds | Emberline only",
 *   "accent": "red",
 *   "cost": "LOW",
 *   "cooldown_ticks": 60,
 *   "owning_class": "yourmod:runeblade",
 *   "executor": "net.yourmod.abilities.CinderSlash"
 * }
 * </pre>
 *
 * <p>A file that cannot be read is reported by name and skipped. One malformed
 * definition must not cost an addon the rest of its abilities, and silence would
 * leave an author with an ability that simply never appears.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class HunterAbilityLoader extends SimpleJsonResourceReloadListener {
	/** Where addons put their files, under their own namespace. */
	public static final String DIRECTORY = "sololeveling/abilities";

	public HunterAbilityLoader() {
		super(new com.google.gson.Gson(), DIRECTORY);
	}

	@SubscribeEvent
	public static void onAddReloadListener(AddReloadListenerEvent event) {
		event.addListener(new HunterAbilityLoader());
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
			ProfilerFiller profiler) {
		// Withdraw the previous load first, so a reload replaces rather than
		// collides with what it added last time.
		HunterAbilityRegistry.clearDataDefinitions();

		int loaded = 0;
		for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			ResourceLocation id = file.getKey();
			try {
				HunterAbility ability = parse(id, GsonHelper.convertToJsonObject(
						file.getValue(), "ability"));
				String executor = GsonHelper.getAsString(
						file.getValue().getAsJsonObject(), "executor");
				HunterAbilityRegistry.register(ability, executor);
				loaded++;
			} catch (JsonParseException | IllegalArgumentException exception) {
				SololevelingMod.LOGGER.error("Skipping ability definition {}: {}",
						id, exception.getMessage());
			}
		}
		if (loaded > 0)
			SololevelingMod.LOGGER.info("Loaded {} contributed ability definition(s)", loaded);
	}

	private static HunterAbility parse(ResourceLocation id, JsonObject json) {
		String name = GsonHelper.getAsString(json, "name");
		String summary = GsonHelper.getAsString(json, "summary");
		String detail = GsonHelper.getAsString(json, "detail", "");
		int cooldown = GsonHelper.getAsInt(json, "cooldown_ticks", 0);

		ChatFormatting accent = accent(GsonHelper.getAsString(json, "accent", "white"));
		AbilityCost cost = cost(GsonHelper.getAsString(json, "cost", "LOW"));

		ResourceLocation owningClass = null;
		if (json.has("owning_class")) {
			String raw = GsonHelper.getAsString(json, "owning_class");
			owningClass = ResourceLocation.tryParse(raw);
			if (owningClass == null)
				throw new IllegalArgumentException("owning_class \"" + raw + "\" is not a valid id");
		}

		HunterAbility.Mode mode = mode(GsonHelper.getAsString(json, "mode", "instant"));
		int upkeep = GsonHelper.getAsInt(json, "upkeep_per_second", 0);
		return new HunterAbility(id, name, summary, detail, accent, cost, cooldown, owningClass,
				mode, upkeep, icon(json));
	}

	/**
	 * The HUD slot texture. Optional: an ability without one shows the same empty
	 * frame the mod draws for every slot today.
	 */
	private static ResourceLocation icon(JsonObject json) {
		String raw = GsonHelper.getAsString(json, "icon", "");
		if (raw == null || raw.isBlank())
			return null;
		ResourceLocation icon = ResourceLocation.tryParse(raw.trim());
		if (icon == null)
			throw new IllegalArgumentException("icon \"" + raw
					+ "\" is not a valid texture path");
		return icon;
	}

	private static HunterAbility.Mode mode(String raw) {
		try {
			return HunterAbility.Mode.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("mode \"" + raw + "\" is not instant or toggle");
		}
	}

	private static ChatFormatting accent(String raw) {
		ChatFormatting value = ChatFormatting.getByName(raw);
		// A silent fallback to white would look like a colour that simply did not
		// take, which is far harder to diagnose than being told the name is wrong.
		if (value == null || !value.isColor())
			throw new IllegalArgumentException("accent \"" + raw + "\" is not a colour name");
		return value;
	}

	private static AbilityCost cost(String raw) {
		try {
			return AbilityCost.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("cost \"" + raw
					+ "\" is not one of NOMINAL, LOW, MEDIUM, HIGH, APEX");
		}
	}
}
