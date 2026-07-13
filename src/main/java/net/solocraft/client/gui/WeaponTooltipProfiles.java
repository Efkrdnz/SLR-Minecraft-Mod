package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Weapon appraisal data shared by tooltip text and visual rendering. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WeaponTooltipProfiles {
	public static final int STEEL = 0;
	public static final int FROST = 1;
	public static final int FLAME = 2;
	public static final int VENOM = 3;
	public static final int VOID = 4;
	public static final int NATURE = 5;
	public static final int STORM = 6;
	public static final int DRAGON = 7;
	public static final int SPIRIT = 8;
	public static final int BLOOD = 9;
	public static final int ROYAL = 10;
	public static final int LUNAR = 11;
	public static final int TITAN = 12;

	private static final int E = 0;
	private static final int D = 1;
	private static final int C = 2;
	private static final int B = 3;
	private static final int A = 4;
	private static final int S = 5;
	private static final int RELIC = 6;
	private static final int NATIONAL = 7;

	private static final int E_COLOR = 0xA9ADB5;
	private static final int D_COLOR = 0x70C98A;
	private static final int C_COLOR = 0x65D8E8;
	private static final int B_COLOR = 0x5D86F7;
	private static final int A_COLOR = 0xBB6EFF;
	private static final int S_COLOR = 0xFFC94F;

	private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();

	static {
		// Association-standard weapons: restrained effects that grow cleaner and richer by rank.
		add("e_tier_sword", p("E", E, "Sword", STEEL, E_COLOR, 0x4A4E58, 1.0F, 0,
				l("An entry-grade blade issued to newly awakened", "hunters. Dependable, inexpensive, and replaceable.")));
		add("d_tier_sword", p("D", D, "Sword", STEEL, D_COLOR, 0x244B35, 1.2F, 0,
				l("A reinforced field sword made for routine gates.", "Its edge can endure prolonged low-rank combat.")));
		add("c_tier_sword", p("C", C, "Sword", STEEL, C_COLOR, 0x255D68, 1.4F, 0,
				l("A mana-treated sword trusted by veteran raiders.", "The blade remains stable under repeated reinforcement.")));
		add("b_tier_sword", p("B", B, "Sword", STEEL, B_COLOR, 0x24386F, 1.6F, 0,
				l("A high-grade sword forged around a mana core.", "Its balance responds naturally to a hunter's strength.")));
		add("a_tier_sword", p("A", A, "Sword", STEEL, A_COLOR, 0x532D78, 1.8F, 0,
				l("A rare weapon reserved for elite strike teams.", "Dense mana sharpens the edge beyond ordinary steel.")));
		add("s_tier_sword", p("S", S, "Sword", ROYAL, S_COLOR, 0x8A4D12, 2.0F, 0,
				l("A masterwork capable of surviving an S-rank gate.", "Only exceptional hunters can draw out its full force.")));

		add("sword_curved_d", p("E", E, "Katana", STEEL, E_COLOR, 0x444952, 2.2F, 0,
				l("A light curved blade sold to novice hunters.", "Its forgiving balance rewards careful technique.")));
		add("sword_warrior_d", p("D", D, "Katana", BLOOD, D_COLOR, 0x49302D, 2.4F, 0,
				l("A practical katana favored by close-range hunters.", "The thick spine withstands rough dungeon fighting.")));
		add("sword_twinwing_c", p("C", C, "Katana", SPIRIT, C_COLOR, 0x316C76, 2.6F, 0,
				l("Twin grooves guide mana along the cutting edge.", "Fast swings leave a faint wing-shaped afterimage.")));
		add("sword_nature_b", p("B", B, "Katana", NATURE, 0x6ED07B, 0x285838, 2.8F, 0,
				l("A living-wood hilt steadies this enchanted blade.", "Ambient mana gathers around it like drifting leaves.")));
		add("sword_enriched_b", p("A", A, "Katana", ROYAL, A_COLOR, 0x66448B, 3.0F, 0,
				l("A heavily enriched katana with exceptional density.", "Its compressed mana rewards precise, decisive cuts.")));
		add("katana_s", p("S", S, "Katana", BLOOD, S_COLOR, 0x7B1F22, 3.2F, 0,
				l("An S-rank katana polished to a mirror finish.", "The blade carries killing intent without losing control.")));
		add("katana_stier", p("S", RELIC, "Katana", ROYAL, 0xFFD76A, 0xA83B1E, 3.4F, 4,
				l("A peerless katana forged by the Dwarf King's", "finest smiths. Its wounds refuse to close cleanly."),
				l("Bleed", "Successful strikes can inflict bleeding.")));

		add("dagger_karambit_e", p("E", E, "Dagger", STEEL, E_COLOR, 0x454851, 4.0F, 0,
				l("A compact karambit for newly awakened assassins.", "Its hooked edge favors speed over stopping power.")));
		add("dagger_knight_d", p("D", D, "Dagger", STEEL, D_COLOR, 0x304A3D, 4.2F, 0,
				l("A sturdy sidearm patterned after a knight's blade.", "Reliable when a larger weapon cannot be drawn.")));
		add("dagger_chain_c", p("C", C, "Dagger", BLOOD, C_COLOR, 0x334E61, 4.4F, 0,
				l("Mana-conductive chainwork reinforces the grip.", "The weapon remains steady through rapid combinations.")));
		add("dagger_golden_b", p("B", B, "Dagger", ROYAL, 0xF0B94F, 0x75531C, 4.6F, 0,
				l("A ceremonial dagger made combat-ready by enchantment.", "Golden channels distribute mana across the edge.")));
		add("dagger_duolity_a", p("A", A, "Dagger", VOID, A_COLOR, 0x30284E, 4.8F, 0,
				l("A divided mana core holds two opposing currents.", "Its balance shifts between restraint and aggression.")));
		add("dagger_heat_a", p("S", S, "Dagger", FLAME, 0xFF9B42, 0x7A2013, 5.0F, 0,
				l("An S-rank dagger with a furnace-hot mana channel.", "The edge glows brighter as combat grows more intense.")));

		// Named weapons and boss relics.
		add("hammer", p("S", RELIC, "War Hammer", TITAN, 0xE0B76A, 0x59422C, 5.5F, 5,
				l("A giant's weapon reduced by dwarven craftsmanship.", "Its weight still rejects all but chosen wielders."),
				l("Titan Weight", "Delivers overwhelming impact through raw mass.")));
		add("war_axe", p("S", RELIC, "War Axe", TITAN, 0xE8A857, 0x7B2F24, 5.8F, 5,
				l("A giant-forged axe reshaped for human hands.", "The head retains the pressure of its original size."),
				l("Giant Cleaver", "Built to break guards with crushing force.")));
		add("ice_spear", p("MONARCH", RELIC, "Spear", FROST, 0xA9F4FF, 0x3C66B8, 6.1F, 0,
				l("A spear condensed from the Frost Monarch's mana.", "Its point radiates a cold untouched by mortal flame."),
				l("Permafrost", "Carries sovereign frost through every strike.")));
		add("frost_blade", p("S", RELIC, "Sword", FROST, 0x8DEBFF, 0x315CA8, 6.4F, 0,
				l("A frozen blade formed around an unmelting core.", "Wounds bloom with ice before the steel leaves them."),
				l("Frostbite", "Strikes can burden targets with supernatural cold.")));
		add("demon_kings_long_sword", p("S", RELIC, "Longsword", FLAME, 0xFFB13B, 0x721B28, 6.7F, 5,
				l("Baran's longsword, saturated with demonic lightning.", "Each swing calls the unrest of a burning storm."),
				l("Storm of Flames", "Unleashes lightning and fire around its target.")));
		add("dragon_shortsword", p("NATIONAL", NATIONAL, "Shortsword", DRAGON, 0xFFD24F, 0xC62F22, 7.0F, 6,
				l("A National Rank weapon carved from a sovereign", "dragon's fang. Its edge yields only to greater power."),
				l("Dragon Fang", "Scales with a wielder capable of mastering it.", "Twin Grip", "Can be wielded as part of a dagger pair.")));

		add("kamish_wrath", p("UNMEASURABLE", RELIC, "Dagger", DRAGON, 0xFFB32C, 0x9D171A, 7.3F, 5,
				l("One of two daggers forged from Kamish's sharpest", "fangs. Its mana sensitivity borders on the unnatural."),
				l("Dragon Resonance", "Its power rises with the wielder's capabilities.")));
		add("kamish_wrath_2", p("UNMEASURABLE", RELIC, "Dagger", DRAGON, 0xFF7936, 0x8D1437, 7.7F, 5,
				l("The twin fang to Kamish's Wrath, carrying the same", "savage edge through an opposing mana current."),
				l("Dragon Resonance", "Completes the paired current of Kamish's fangs.")));
		add("demon_kings_dagger", p("S", RELIC, "Dagger", FLAME, 0xFFAE35, 0x731728, 8.0F, 8,
				l("A dagger claimed from Baran, King of Demons.", "Its dormant flame answers when its twin is drawn."),
				l("Two as One", "Dual wielding adds the wielder's Strength.")));
		add("kasakas_venom_fangs", p("C", C, "Dagger", VENOM, 0x57E37D, 0x1B5B3F, 8.3F, 6,
				l("A dagger fashioned from Kasaka's venom fang.", "Residual poison still circulates through the weapon."),
				l("Paralyze", "Venom can seize a target's movement.", "Bleed", "The serrated fang leaves persistent wounds.")));
		add("kasakas_awakened_venom_fang", p("S", RELIC, "Dagger", VENOM, 0x8CFF62, 0x4E1A76, 8.6F, 6,
				l("Kasaka's fang after its venom core awakened.", "The poison now behaves like a living predator."),
				l("Awakened Venom", "Greatly intensifies paralysis and bleeding.")));
		add("barukas_dagger", p("A", A, "Dagger", FROST, 0xA1EFFF, 0x3D5F9F, 8.9F, 7,
				l("The ice-elf warlord Baruka carried this dagger.", "Weight-reducing magic makes the blade feel effortless."),
				l("Warlord's Agility", "The enchantment favors swift movement.")));
		add("knight_killer", p("B", B, "Dagger", STEEL, 0x7AA7FF, 0x3D425A, 9.2F, 4,
				l("A narrow dagger engineered to slip through armor.", "Its mana edge seeks seams in reinforced defenses."),
				l("Armor Breaker", "Deals 25% more damage to armored enemies.")));
		add("mythic_dagger", p("S", RELIC, "Dagger", VOID, 0xE078FF, 0x4A1766, 9.5F, 2,
				l("A spatial dagger whose edge exists a step aside", "from reality. Danger awakens its hidden movement."),
				l("Counter Shift", "Can evade a hit and move behind the attacker.")));
		add("gravity_dagger", p("A", A, "Dagger", VOID, 0xA377FF, 0x291B54, 9.8F, 0,
				l("A dagger built around a compressed gravity shard.", "Every wound briefly distorts the target's weight."),
				l("Gravity Well", "Strikes can disrupt and lift their target.")));
		add("emerald_dagger", p("A", A, "Dagger", LUNAR, 0x8EFFF0, 0x3659A1, 10.1F, 4,
				l("A blade forged from crystallized moonlight.", "It bends light and strikes through false angles."),
				l("Moonlight Refraction", "Fast cuts leave deceptive afterimages.")));

		add("mana_gun", p("A", A, "Mana Firearm", SPIRIT, 0x56DFFF, 0x284A9A, 10.4F, 0,
				l("A precision firearm that condenses mana into rounds.", "Output scales with the intelligence of its wielder."),
				l("Mana Chamber", "Fires without conventional ammunition.")));
		add("storm_griamore", p("S", RELIC, "Magic Grimoire", STORM, 0xD9F4FF, 0x5352BC, 10.7F, 5,
				l("A forbidden tome that commands wind and lightning.", "Each page turn draws the wielder into the storm."),
				l("Tempest Authority", "Channels violent atmospheric magic.")));
		add("spirit_bow", p("A", A, "Mana Bow", NATURE, 0x8DFFD0, 0x315F79, 11.0F, 2,
				l("An elven bow believed to originate beyond this realm.", "It shapes the wielder's mana into silent arrows."),
				l("Spirit Arrow", "Uses mana in place of physical ammunition.")));

		// Story and legacy weapons that are not always exposed in the creative tab.
		add("igrislongsword", p("A", A, "Longsword", BLOOD, 0xE85858, 0x541B2B, 11.3F, 0,
				l("A knight's longsword stained by unwavering loyalty.", "Its heavy edge was made for relentless duels.")));
		add("griamore", p("S", RELIC, "Magic Grimoire", FLAME, 0xFF8B38, 0x7C1724, 11.6F, 0,
				l("A grimoire whose pages never surrender their heat.", "Its script converts mana into consuming flame."),
				l("Inferno Script", "Amplifies fire magic cast by its holder.")));
		add("teleport_sai", p("A", A, "Throwing Sai", VOID, 0xB68CFF, 0x263A78, 11.9F, 1,
				l("A demonic sai anchored to its wielder's mana.", "Throwing it opens a path through folded space."),
				l("Spatial Pursuit", "Teleports the user to the marked enemy.")));
		add("kangs_dagger", p("B", B, "Dagger", BLOOD, 0xE85B79, 0x4E2135, 12.2F, 0,
				l("The dagger of an assassin who hunted other hunters.", "A cold residue of killing intent clings to the grip.")));
	}

	private WeaponTooltipProfiles() {
	}

	private static void add(String id, Profile profile) {
		PROFILES.put(id, profile);
	}

	private static Profile p(String rank, int tier, String type, int theme, int primary,
			int secondary, float seed, int legacyLines, List<String> lore) {
		return new Profile(rank, tier, type, theme, primary, secondary, seed, legacyLines, lore, List.of());
	}

	private static Profile p(String rank, int tier, String type, int theme, int primary,
			int secondary, float seed, int legacyLines, List<String> lore, List<String> traits) {
		return new Profile(rank, tier, type, theme, primary, secondary, seed, legacyLines, lore, traits);
	}

	private static List<String> l(String... lines) {
		return List.of(lines);
	}

	public static Profile find(ItemStack stack) {
		if (stack.isEmpty())
			return null;
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
		return id != null && SololevelingMod.MODID.equals(id.getNamespace()) ? PROFILES.get(id.getPath()) : null;
	}

	public static Map<String, Profile> all() {
		return Map.copyOf(PROFILES);
	}

	@SubscribeEvent
	public static void rebuildTooltip(ItemTooltipEvent event) {
		Profile profile = find(event.getItemStack());
		if (profile == null || event.getToolTip().isEmpty())
			return;

		List<Component> tooltip = event.getToolTip();
		for (int i = 0; i < profile.legacyLines() && tooltip.size() > 1; i++)
			tooltip.remove(1);

		TextColor primary = TextColor.fromRgb(profile.primaryColor());
		TextColor secondary = TextColor.fromRgb(profile.secondaryColor());
		tooltip.set(0, tooltip.get(0).copy().withStyle(style -> style.withColor(primary)
				.withBold(profile.tier() >= S)));

		int insert = 1;
		tooltip.add(insert++, field("ITEM CLASS", profile.rank(), primary));
		tooltip.add(insert++, field("TYPE", profile.type(), primary));
		tooltip.add(insert++, Component.empty());
		tooltip.add(insert++, Component.literal("APPRAISAL")
				.withStyle(Style.EMPTY.withColor(secondary).withBold(true)));
		for (String line : profile.lore()) {
			tooltip.add(insert++, Component.literal(line)
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xC9CDD6)).withItalic(true)));
		}
		if (!profile.traits().isEmpty()) {
			tooltip.add(insert++, Component.empty());
			for (int i = 0; i < profile.traits().size(); i += 2) {
				String name = profile.traits().get(i);
				String detail = i + 1 < profile.traits().size() ? profile.traits().get(i + 1) : "";
				tooltip.add(insert++, Component.literal("[" + name + "]")
						.withStyle(Style.EMPTY.withColor(primary).withBold(true)));
				tooltip.add(insert++, Component.literal("  " + detail)
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAEB7C6))));
			}
		}
	}

	private static Component field(String label, String value, TextColor color) {
		MutableComponent line = Component.literal(label + "  ")
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x7F8A9B)));
		line.append(Component.literal(value).withStyle(Style.EMPTY.withColor(color).withBold(true)));
		return line;
	}

	public record Profile(String rank, int tier, String type, int theme, int primaryColor,
			int secondaryColor, float seed, int legacyLines, List<String> lore, List<String> traits) {
	}
}
