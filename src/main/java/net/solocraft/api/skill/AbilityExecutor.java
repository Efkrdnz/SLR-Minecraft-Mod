package net.solocraft.api.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What an ability actually does.
 *
 * <p>An ability's description lives in JSON; its behaviour lives here. The JSON
 * names the implementing class in its {@code executor} field and the mod builds
 * one when the ability is first cast.
 *
 * <p>Implementations need a public no-argument constructor, and are built once
 * and reused, so they must hold no per-player state. Anything that varies per
 * cast arrives in the {@link AbilityContext}.
 *
 * <p>By the time this runs the mod has confirmed the hunter holds the right
 * class, learned the ability, and is off cooldown, and has checked they can
 * afford it. Produce the effect, report what it reached, and nothing else.
 */
@FunctionalInterface
public interface AbilityExecutor {
	void execute(AbilityContext context);

	/**
	 * Called when a toggle ability ends, whether the hunter turned it off or
	 * ran out of mana to hold it.
	 *
	 * <p>Undo whatever {@link #execute} put in place. The mod clears the form
	 * state itself; anything else -- an aura, a summoned thing, an attribute --
	 * is yours to remove, and leaving it behind is how a toggle turns into a
	 * permanent buff.
	 *
	 * <p>Ignored for instant abilities.
	 */
	default void deactivate(AbilityContext context) {
	}

	/**
	 * Everything an ability is given when it fires, and what it reports back.
	 *
	 * <p>Mana is settled <em>after</em> the effect, because an ability that hits
	 * four targets should not cost the same as one that hit nothing. Tell the
	 * context what the effect actually reached and the mod prices it on the same
	 * curve its own abilities use.
	 *
	 * <p>Leave everything alone and the ability simply costs its band's base --
	 * which is right for an ability that always does the same thing.
	 *
	 * <p>Server-side by construction, and one instance per cast, so the outcome
	 * fields are not shared between players.
	 */
	final class AbilityContext {
		private final ServerPlayer player;
		private final HunterAbility ability;

		private int acceptedTargets;
		private int stage = 1;
		private double executionModifier = 1.0D;

		public AbilityContext(ServerPlayer player, HunterAbility ability) {
			this.player = player;
			this.ability = ability;
		}

		public ServerPlayer player() {
			return player;
		}

		public HunterAbility ability() {
			return ability;
		}

		public ServerLevel level() {
			return player.serverLevel();
		}

		/**
		 * How many targets the effect actually reached.
		 *
		 * <p>Count what it landed on, not what it aimed at. Charging for targets
		 * an ability missed is the thing this exists to avoid.
		 */
		public AbilityContext acceptedTargets(int targets) {
			this.acceptedTargets = Math.max(0, targets);
			return this;
		}

		/** Which stage of a staged ability fired, 1 through 5. Later stages cost more. */
		public AbilityContext stage(int stage) {
			this.stage = Math.max(1, Math.min(5, stage));
			return this;
		}

		/**
		 * A final multiplier of your own, for anything the mod cannot see -- a
		 * timing window hit, a stack consumed, a charge level.
		 *
		 * <p>Never scale this by Intelligence. Intelligence already raises maximum
		 * mana and cost is a fraction of that maximum, so a second Intelligence
		 * term would scale the stat twice and make investment feel punishing.
		 */
		public AbilityContext executionModifier(double modifier) {
			this.executionModifier = Math.max(0.0D, modifier);
			return this;
		}

		/** Reports nothing happened, so the cast costs nothing. */
		public AbilityContext noEffect() {
			return executionModifier(0.0D);
		}

		public int acceptedTargetCount() {
			return acceptedTargets;
		}

		public int stageReached() {
			return stage;
		}

		public double executionModifierUsed() {
			return executionModifier;
		}
	}
}
