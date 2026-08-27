package net.solocraft.util;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Resolves short-lived additions to System attributes without writing them into
 * the player's permanent stat capability.
 *
 * <p>The built-in sources are intentionally derived from state that Minecraft
 * already synchronizes (mob effects, held equipment, and PlayerVariables).
 * Consequently the server's combat calculation and the client's System panel
 * resolve the same values without a second mutable "temporary stat" store that
 * can drift after death, logout, effect replacement, or equipment changes.</p>
 *
 * <p>Future equipment and accessory systems can register a provider during
 * common setup. Providers must inspect state available on both logical sides if
 * their result is intended to be displayed by the client. Registration makes a
 * source available to the resolver and System UI; it does not automatically
 * update every gameplay formula. New consumers must read {@link #effectiveValue}
 * (or a stat-specific helper) instead of the permanent capability field.
 * Current built-in gameplay consumers cover Strength, Agility, and Intelligence;
 * providers for Perception or Vitality must wire their relevant gameplay
 * consumers as those sources are introduced.</p>
 */
public final class TemporaryStatBonusManager {
	public static final ResourceLocation EFFECT_PROVIDER =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "active_effects");
	public static final ResourceLocation EQUIPMENT_PROVIDER =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "equipment_sets");
	public static final ResourceLocation HASTE_BUFF_SOURCE =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "haste_buff");
	public static final ResourceLocation PHYSICAL_BUFF_SOURCE =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "physical_buff");
	public static final ResourceLocation TWO_AS_ONE_SOURCE =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "two_as_one");
	public static final ResourceLocation MANA_SENSITIVITY_SOURCE =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "mana_sensitivity");
	public static final ResourceLocation DEMONIC_ATTUNEMENT_SOURCE =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "demonic_attunement");
	public static final ResourceLocation AVARICIOUS_INSIGHT_SOURCE =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "avaricious_insight");
	public static final ResourceLocation TEMPEST_AUTHORITY_SOURCE =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "tempest_authority");
	public static final double HASTE_BUFF_AGILITY_BONUS = 30.0D;
	public static final double PHYSICAL_BUFF_STRENGTH_BONUS = 30.0D;
	public static final double TWO_AS_ONE_FLAT_BONUS = 20.0D;
	public static final double TWO_AS_ONE_PERCENT_BONUS = 0.20D;
	public static final double MANA_SENSITIVITY_STRENGTH_FLAT_BONUS = 10.0D;
	public static final double MANA_SENSITIVITY_STRENGTH_PERCENT_BONUS = 0.10D;
	public static final double MANA_SENSITIVITY_INTELLIGENCE_FLAT_BONUS = 10.0D;
	public static final double MANA_SENSITIVITY_INTELLIGENCE_PERCENT_BONUS = 0.10D;
	public static final double DEMONIC_ATTUNEMENT_FLAT_BONUS = 10.0D;
	public static final double DEMONIC_ATTUNEMENT_PERCENT_BONUS = 0.10D;
	public static final double AVARICIOUS_INSIGHT_FLAT_BONUS = 10.0D;
	public static final double AVARICIOUS_INSIGHT_PERCENT_BONUS = 0.10D;
	public static final double TEMPEST_AUTHORITY_FLAT_BONUS = 10.0D;
	public static final double TEMPEST_AUTHORITY_PERCENT_BONUS = 0.10D;

	private static final List<ProviderEntry> PROVIDERS = new CopyOnWriteArrayList<>();

	static {
		registerProvider(EFFECT_PROVIDER, TemporaryStatBonusManager::collectEffectBonuses);
		registerProvider(EQUIPMENT_PROVIDER, TemporaryStatBonusManager::collectEquipmentBonuses);
	}

	private TemporaryStatBonusManager() {
	}

	public enum Stat {
		STRENGTH("Strength"),
		AGILITY("Agility"),
		PERCEPTION("Perception"),
		VITALITY("Vitality"),
		INTELLIGENCE("Intelligence");

		private final String displayName;

		Stat(String displayName) {
			this.displayName = displayName;
		}

		public String displayName() {
			return displayName;
		}
	}

	/**
	 * One itemized contribution shown in the System-panel hover tooltip.
	 */
	public record BonusSource(ResourceLocation id, Component displayName, double amount) {
		public BonusSource {
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(displayName, "displayName");
			if (!Double.isFinite(amount))
				throw new IllegalArgumentException("Temporary stat bonus must be finite");
		}
	}

	@FunctionalInterface
	public interface BonusProvider {
		void collect(Entity entity, Stat stat, double baseValue, BonusSink sink);
	}

	@FunctionalInterface
	public interface BonusSink {
		void add(BonusSource source);

		default void add(ResourceLocation id, Component displayName, double amount) {
			add(new BonusSource(id, displayName, amount));
		}
	}

	/**
	 * Registers a reusable source of temporary stat bonuses.
	 *
	 * @throws IllegalStateException when the same provider id is registered twice
	 */
	public static synchronized void registerProvider(ResourceLocation id, BonusProvider provider) {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(provider, "provider");
		if (PROVIDERS.stream().anyMatch(entry -> entry.id().equals(id)))
			throw new IllegalStateException("Temporary stat provider is already registered: " + id);
		PROVIDERS.add(new ProviderEntry(id, provider));
	}

	public static double baseValue(Entity entity, Stat stat) {
		if (entity == null || stat == null)
			return 0.0D;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> switch (stat) {
					case STRENGTH -> variables.Strength;
					case AGILITY -> variables.Speed;
					case PERCEPTION -> variables.perception;
					case VITALITY -> variables.Vitality;
					case INTELLIGENCE -> variables.Intelligence;
				})
				.orElse(0.0D);
	}

	public static List<BonusSource> sources(Entity entity, Stat stat) {
		if (entity == null || stat == null)
			return List.of();

		double baseValue = baseValue(entity, stat);
		Map<ResourceLocation, BonusSource> combined = new LinkedHashMap<>();
		BonusSink sink = source -> {
			if (source == null || !Double.isFinite(source.amount()) || source.amount() <= 0.000001D)
				return;
			combined.merge(source.id(), source,
					(left, right) -> new BonusSource(left.id(), left.displayName(), left.amount() + right.amount()));
		};
		for (ProviderEntry entry : PROVIDERS)
			entry.provider().collect(entity, stat, baseValue, sink);
		return List.copyOf(new ArrayList<>(combined.values()));
	}

	public static double bonusValue(Entity entity, Stat stat) {
		return sources(entity, stat).stream().mapToDouble(BonusSource::amount).sum();
	}

	public static double effectiveValue(Entity entity, Stat stat) {
		return baseValue(entity, stat) + bonusValue(entity, stat);
	}

	public static double effectiveStrength(Entity entity) {
		return effectiveValue(entity, Stat.STRENGTH);
	}

	public static double effectiveAgility(Entity entity) {
		return effectiveValue(entity, Stat.AGILITY);
	}

	public static double effectivePerception(Entity entity) {
		return effectiveValue(entity, Stat.PERCEPTION);
	}

	public static double effectiveVitality(Entity entity) {
		return effectiveValue(entity, Stat.VITALITY);
	}

	public static double effectiveIntelligence(Entity entity) {
		return effectiveValue(entity, Stat.INTELLIGENCE);
	}

	public static boolean isTwoAsOneActive(Entity entity) {
		return entity instanceof LivingEntity living
				&& living.getMainHandItem().is(SololevelingModItems.DEMON_KINGS_DAGGER.get())
				&& living.getOffhandItem().is(SololevelingModItems.DEMON_KINGS_DAGGER.get());
	}

	public static String format(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.000001D)
			return Long.toString(Math.round(value));
		return new DecimalFormat("0.##").format(value);
	}

	private static void collectEffectBonuses(Entity entity, Stat stat, double baseValue, BonusSink sink) {
		if (!(entity instanceof LivingEntity living))
			return;

		if (stat == Stat.AGILITY) {
			MobEffectInstance haste = living.getEffect(SololevelingModMobEffects.HASTE_BUFF);
			if (haste != null) {
				sink.add(HASTE_BUFF_SOURCE, Component.literal("Haste Buff effect"),
						HASTE_BUFF_AGILITY_BONUS);
			}
		}
		if (stat == Stat.STRENGTH) {
			MobEffectInstance physical = living.getEffect(SololevelingModMobEffects.PHYSICAL_BUFF);
			if (physical != null) {
				sink.add(PHYSICAL_BUFF_SOURCE, Component.literal("Physical Buff effect"),
						PHYSICAL_BUFF_STRENGTH_BONUS);
			}
		}
	}

	private static void collectEquipmentBonuses(Entity entity, Stat stat, double baseValue, BonusSink sink) {
		if (!(entity instanceof LivingEntity living))
			return;

		if (stat == Stat.STRENGTH) {
			if (isTwoAsOneActive(living))
				sink.add(TWO_AS_ONE_SOURCE, Component.literal("Two as One"),
						scaledBonus(baseValue, TWO_AS_ONE_FLAT_BONUS, TWO_AS_ONE_PERCENT_BONUS));

			int kamishFangs = kamishWrathCount(living);
			if (kamishFangs > 0) {
				// Every fang combines permanent Strength and Intelligence.
				// Temporary sources never recursively amplify either component.
				double permanentIntelligence = TemporaryStatBonusManager.baseValue(
						entity, Stat.INTELLIGENCE);
				sink.add(MANA_SENSITIVITY_SOURCE, Component.literal("Mana Sensitivity"),
						kamishFangs * manaSensitivityBonus(baseValue, permanentIntelligence));
			}
			return;
		}
		if (stat != Stat.INTELLIGENCE)
			return;

		if (isHeld(living, SololevelingModItems.DEMON_KINGS_LONG_SWORD.get()))
			sink.add(DEMONIC_ATTUNEMENT_SOURCE, Component.literal("Demonic Attunement"),
					scaledBonus(baseValue, DEMONIC_ATTUNEMENT_FLAT_BONUS,
							DEMONIC_ATTUNEMENT_PERCENT_BONUS));
		if (isHeld(living, SololevelingModItems.ORB_OF_AVARICE.get()))
			sink.add(AVARICIOUS_INSIGHT_SOURCE, Component.literal("Avaricious Insight"),
					scaledBonus(baseValue, AVARICIOUS_INSIGHT_FLAT_BONUS,
							AVARICIOUS_INSIGHT_PERCENT_BONUS));
		if (isHeld(living, SololevelingModItems.STORM_GRIAMORE.get()))
			sink.add(TEMPEST_AUTHORITY_SOURCE, Component.literal("Tempest Authority"),
					scaledBonus(baseValue, TEMPEST_AUTHORITY_FLAT_BONUS,
							TEMPEST_AUTHORITY_PERCENT_BONUS));
	}

	private static int kamishWrathCount(LivingEntity living) {
		int count = isKamishWrath(living.getMainHandItem().getItem()) ? 1 : 0;
		return count + (isKamishWrath(living.getOffhandItem().getItem()) ? 1 : 0);
	}

	private static boolean isKamishWrath(Item item) {
		return item == SololevelingModItems.KAMISH_WRATH.get()
				|| item == SololevelingModItems.KAMISH_WRATH_2.get();
	}

	private static boolean isHeld(LivingEntity living, Item item) {
		return living.getMainHandItem().is(item) || living.getOffhandItem().is(item);
	}

	private static double scaledBonus(double baseValue, double flatBonus, double percentBonus) {
		return flatBonus + Math.floor(Math.max(0.0D, baseValue) * percentBonus);
	}

	private static double manaSensitivityBonus(double permanentStrength,
			double permanentIntelligence) {
		return scaledBonus(permanentStrength, MANA_SENSITIVITY_STRENGTH_FLAT_BONUS,
						MANA_SENSITIVITY_STRENGTH_PERCENT_BONUS)
				+ scaledBonus(permanentIntelligence, MANA_SENSITIVITY_INTELLIGENCE_FLAT_BONUS,
						MANA_SENSITIVITY_INTELLIGENCE_PERCENT_BONUS);
	}

	private record ProviderEntry(ResourceLocation id, BonusProvider provider) {
	}
}
