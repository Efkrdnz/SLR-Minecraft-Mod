package net.solocraft.api.skill;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.solocraft.SololevelingMod;
import net.solocraft.api.AbilityCooldowns;
import net.solocraft.api.HunterMana;
import net.solocraft.api.vessel.VesselState;

/**
 * Abilities contributed by addons, presented exactly as the mod's own.
 *
 * <p>Once registered, an ability behaves like a shipped one everywhere: it is
 * taught by a runestone, appears in the skill list, can be equipped to a slot,
 * shows a tooltip in the mod's format, names itself in the top-left overlay, and
 * casts from the skill key. No command and no extra keybind is involved, because
 * a player should not be able to tell which mod an ability came from by how they
 * use it.
 *
 * <p>The one visible difference is deliberate: contributed names are shown with
 * {@link #ADDON_MARKER} in front, so it is always clear where an ability came
 * from without having to look it up.
 *
 * <p>Most abilities arrive through {@link HunterAbilityLoader} reading JSON. The
 * direct {@code register} calls exist for addons that would rather declare in
 * code.
 */
public final class HunterAbilityRegistry {
	/** Shown before every contributed ability name. */
	public static final String ADDON_MARKER = "(ADN)";

	private static final Map<String, Registration> BY_NAME = new LinkedHashMap<>();

	/**
	 * Names that came from a datapack rather than from code.
	 *
	 * <p>Tracked so a reload can withdraw exactly what the last load added.
	 * Clearing everything would delete registrations no reload created.
	 */
	private static final Set<String> FROM_DATA = new LinkedHashSet<>();

	private HunterAbilityRegistry() {
	}

	/**
	 * Registers an ability the mod will cast for you.
	 *
	 * <p>Call during mod construction. The executor runs on the server after the
	 * mod has confirmed the hunter learned the ability, holds the right class, is
	 * off cooldown, and could pay for it -- so it only has to produce the effect.
	 *
	 * @throws IllegalArgumentException if the name is already taken
	 */
	public static synchronized HunterAbility register(HunterAbility ability,
			AbilityExecutor executor) {
		return put(ability, Registration.of(requireAbility(ability), executor));
	}

	/** Registers without behaviour; subscribe to {@link HunterSkillCastEvent} instead. */
	public static HunterAbility register(HunterAbility ability) {
		return register(ability, (AbilityExecutor) null);
	}

	/**
	 * Withdraws every definition the previous datapack load added.
	 *
	 * <p>Called before a reload repopulates them. Code registrations survive,
	 * because a reload did not create them and must not remove them.
	 */
	public static synchronized void clearDataDefinitions() {
		FROM_DATA.forEach(BY_NAME::remove);
		FROM_DATA.clear();
	}

	/** Names contributed by the current datapack load, for syncing to clients. */
	public static synchronized List<HunterAbility> dataDefinitions() {
		List<HunterAbility> abilities = new ArrayList<>();
		for (String name : FROM_DATA) {
			Registration registration = BY_NAME.get(name);
			if (registration != null)
				abilities.add(registration.ability());
		}
		return List.copyOf(abilities);
	}

	/**
	 * Registers an ability whose behaviour lives in a named class.
	 *
	 * <p>Used by the JSON loader. The class is resolved on first cast rather than
	 * now, so an addon's classes need not be loadable at the moment its data file
	 * is read.
	 */
	public static synchronized HunterAbility register(HunterAbility ability,
			String executorClassName) {
		HunterAbility registered = put(ability,
				Registration.deferred(requireAbility(ability), executorClassName));
		FROM_DATA.add(registered.name());
		return registered;
	}

	private static HunterAbility requireAbility(HunterAbility ability) {
		if (ability == null)
			throw new IllegalArgumentException("Ability must not be null");
		return ability;
	}

	private static HunterAbility put(HunterAbility ability, Registration registration) {
		String key = ability.name();
		Registration existing = BY_NAME.get(key);
		if (existing != null)
			throw new IllegalArgumentException("An ability named \"" + key
					+ "\" is already registered by " + existing.ability().id());
		BY_NAME.put(key, registration);
		return ability;
	}

	public static Optional<HunterAbility> byName(String name) {
		Registration registration = lookup(name);
		return registration == null ? Optional.empty() : Optional.of(registration.ability());
	}

	public static boolean isContributed(String name) {
		return lookup(name) != null;
	}

	public static List<HunterAbility> all() {
		List<HunterAbility> abilities = new ArrayList<>();
		for (Registration registration : BY_NAME.values())
			abilities.add(registration.ability());
		return List.copyOf(abilities);
	}

	/** Abilities usable by one class, including those open to every class. */
	public static List<HunterAbility> forClass(ResourceLocation classId) {
		List<HunterAbility> abilities = new ArrayList<>();
		for (Registration registration : BY_NAME.values()) {
			HunterAbility ability = registration.ability();
			if (ability.isClassless() || ability.owningClass().equals(classId))
				abilities.add(ability);
		}
		return List.copyOf(abilities);
	}

	/**
	 * The name as every list, slot, and overlay should show it.
	 *
	 * @return the marked name, or the input unchanged when not contributed
	 */
	public static String displayName(String name) {
		Registration registration = lookup(name);
		return registration == null ? name : ADDON_MARKER + " " + registration.ability().name();
	}

	/** Accent colour packed as RGB, or -1 when this registry does not own the name. */
	public static int color(String name) {
		Registration registration = lookup(name);
		if (registration == null)
			return -1;
		Integer colour = registration.ability().accent().getColor();
		return colour == null ? 0xFFFFFF : colour;
	}

	/** Tooltip lines in the mod's own format, or empty when not contributed. */
	public static List<Component> tooltip(String name) {
		Registration registration = lookup(name);
		if (registration == null)
			return List.of();
		HunterAbility ability = registration.ability();

		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(displayName(ability.name()))
				.withStyle(ability.accent(), ChatFormatting.BOLD));
		lines.add(Component.literal(ability.summary()).withStyle(ChatFormatting.GRAY));
		if (!ability.detail().isBlank())
			lines.add(Component.literal(ability.detail()).withStyle(ChatFormatting.YELLOW));
		return lines;
	}

	/**
	 * Runs a contributed ability, applying the checks the mod applies to its own.
	 *
	 * <p>Called from the skill dispatch after nothing built in matched.
	 *
	 * @return true when this registry owned the skill, whether or not it fired.
	 *         A false return means the dispatch should keep looking.
	 */
	public static boolean cast(Entity caster, String name) {
		Registration registration = lookup(name);
		if (registration == null)
			return false;
		if (!(caster instanceof ServerPlayer player))
			return true;   // owned, but the client does not decide casts

		HunterAbility ability = registration.ability();

		// owning_class is descriptive, not a gate. Learning an ability is what
		// grants it, and the runestone already decides who gets to learn.
		if (!HunterSkills.hasLearned(player, ability.name())) {
			refuse(player, "You have not learned " + displayName(ability.name()) + ".");
			return true;
		}

		String owner = ability.id().getNamespace();
		String key = ability.id().getPath();
		// Turning a form off is not a cast: it spends no mana, runs no executor and
		// produces no effect, so it must not be gated on the cooldown that arming it
		// set. With the gate first, any toggle declaring a cooldown trapped the
		// player inside its own form -- still paying upkeep -- until that cooldown
		// expired. Creative hid it, because CooldownManager bypasses there.
		if (ability.isToggle() && VesselState.isFormActive(player, ability.formId())) {
			deactivate(player, registration, ability);
			return true;
		}

		// Past this point the press is a real cast, so cooldown_ticks means what an
		// author expects: how long until it can be turned back ON.
		if (AbilityCooldowns.isOnCooldown(player, owner, key)) {
			refuse(player, displayName(ability.name()) + " is recovering ("
					+ AbilityCooldowns.remainingSeconds(player, owner, key) + "s).");
			return true;
		}

		// Checked, not charged. What it costs depends on what the effect reaches,
		// so the price is settled below -- but a cast must not start on empty.
		int floor = HunterMana.cost(player, ability.cost());
		if (!HunterMana.canAfford(player, floor)) {
			refuse(player, "Not enough mana (" + floor + ").");
			return true;
		}

		AbilityExecutor.AbilityContext context =
				new AbilityExecutor.AbilityContext(player, ability);
		AbilityExecutor executor = registration.executor();
		if (executor != null) {
			try {
				executor.execute(context);
			} catch (RuntimeException exception) {
				// An addon throwing must not take the skill system down with it, and
				// must not charge for an effect that never finished.
				SololevelingMod.LOGGER.error("Contributed ability {} failed", ability.id(), exception);
				return true;
			}
		}

		if (ability.isToggle())
			VesselState.setFormActive(player, ability.formId(), true);

		int cost = HunterMana.cost(player, ability.cost(), context.stageReached(),
				context.acceptedTargetCount(), context.executionModifierUsed());
		if (cost > 0)
			HunterMana.spend(player, cost);
		if (ability.cooldownTicks() > 0)
			AbilityCooldowns.set(player, owner, key, ability.cooldownTicks());
		return true;
	}

	/**
	 * Ends a toggle. Safe to call when it is already off.
	 *
	 * <p>The form is cleared first, so an executor that throws on the way out
	 * still leaves the hunter out of the form rather than stuck in one nothing
	 * is maintaining.
	 */
	public static void deactivate(ServerPlayer player, String abilityName) {
		Registration registration = lookup(abilityName);
		if (registration != null)
			deactivate(player, registration, registration.ability());
	}

	private static void deactivate(ServerPlayer player, Registration registration,
				HunterAbility ability) {
		VesselState.setFormActive(player, ability.formId(), false);
		AbilityExecutor executor = registration.executor();
		if (executor == null)
			return;
		try {
			executor.deactivate(new AbilityExecutor.AbilityContext(player, ability));
		} catch (RuntimeException exception) {
			SololevelingMod.LOGGER.error("Contributed ability {} failed while ending",
				ability.id(), exception);
		}
	}

	/** Every registered toggle, for the upkeep loop. */
	public static synchronized List<HunterAbility> toggles() {
		List<HunterAbility> toggles = new ArrayList<>();
		for (Registration registration : BY_NAME.values())
			if (registration.ability().isToggle())
				toggles.add(registration.ability());
		return List.copyOf(toggles);
	}

	private static Registration lookup(String name) {
		return name == null ? null : BY_NAME.get(name.trim());
	}

	private static void refuse(ServerPlayer player, String message) {
		player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), true);
	}

	/**
	 * One registered ability and the behaviour it runs.
	 *
	 * <p>A JSON-declared ability names its class as a string. Resolving that
	 * lazily keeps data loading independent of class loading, and means one addon
	 * shipping a broken executor cannot stop the others from registering.
	 */
	private static final class Registration {
		private final HunterAbility ability;
		private final String executorClassName;
		private volatile AbilityExecutor executor;
		private volatile boolean unresolvable;

		private Registration(HunterAbility ability, AbilityExecutor executor,
				String executorClassName) {
			this.ability = ability;
			this.executor = executor;
			this.executorClassName = executorClassName;
		}

		static Registration of(HunterAbility ability, AbilityExecutor executor) {
			return new Registration(ability, executor, null);
		}

		static Registration deferred(HunterAbility ability, String executorClassName) {
			return new Registration(ability, null, executorClassName);
		}

		HunterAbility ability() {
			return ability;
		}

		AbilityExecutor executor() {
			if (executor != null || unresolvable || executorClassName == null)
				return executor;
			synchronized (this) {
				if (executor == null && !unresolvable)
					executor = resolve();
			}
			return executor;
		}

		private AbilityExecutor resolve() {
			try {
				Class<?> type = Class.forName(executorClassName, true,
						HunterAbilityRegistry.class.getClassLoader());

				// Written for this API: implements the interface directly.
				if (AbilityExecutor.class.isAssignableFrom(type))
					return (AbilityExecutor) type.getDeclaredConstructor().newInstance();

				// Written in MCreator: a procedure class with a static execute.
				// Supported so an ability can be built visually and then simply
				// named in JSON, with no hand-written Java at all.
				AbilityExecutor adapted = adaptProcedure(type);
				if (adapted != null)
					return adapted;

				SololevelingMod.LOGGER.error("Ability {} names executor {}, which is neither an "
						+ "AbilityExecutor nor a procedure class with a static execute method",
						ability.id(), executorClassName);
				unresolvable = true;
				return null;
			} catch (ReflectiveOperationException | RuntimeException exception) {
				SololevelingMod.LOGGER.error("Ability {} could not build executor {}",
						ability.id(), executorClassName, exception);
				unresolvable = true;
				return null;
			}
		}

		/**
		 * Adapts an MCreator procedure class.
		 *
		 * <p>MCreator generates a class with a static {@code execute} whose
		 * parameters depend on what the procedure asked for. The three shapes
		 * below are the ones a player-triggered procedure produces, and together
		 * account for the overwhelming majority of procedures in this mod.
		 */
		private AbilityExecutor adaptProcedure(Class<?> type) {
			Method positional = staticExecute(type, LevelAccessor.class,
					double.class, double.class, double.class, Entity.class);
			if (positional != null)
				return context -> invoke(positional, context.level(),
						context.player().getX(), context.player().getY(), context.player().getZ(),
						context.player());

			Method levelAndEntity = staticExecute(type, LevelAccessor.class, Entity.class);
			if (levelAndEntity != null)
				return context -> invoke(levelAndEntity, context.level(), context.player());

			Method entityOnly = staticExecute(type, Entity.class);
			if (entityOnly != null)
				return context -> invoke(entityOnly, context.player());

			return null;
		}

		private static Method staticExecute(Class<?> type, Class<?>... parameters) {
			try {
				Method method = type.getMethod("execute", parameters);
				return Modifier.isStatic(method.getModifiers()) ? method : null;
			} catch (NoSuchMethodException exception) {
				return null;
			}
		}

		private void invoke(Method method, Object... arguments) {
			try {
				method.invoke(null, arguments);
			} catch (ReflectiveOperationException | RuntimeException exception) {
				SololevelingMod.LOGGER.error("Ability {} failed inside procedure {}",
						ability.id(), executorClassName, exception);
			}
		}
	}
}
