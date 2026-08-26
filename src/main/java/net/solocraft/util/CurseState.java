package net.solocraft.util;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Curses carried by a target.
 *
 * <p>Each curse is a real {@link MobEffect}, so vanilla owns presence, duration,
 * expiry, saving and the inventory icon, and only entities that actually carry
 * one are ticked. This class adds the single thing an effect cannot express:
 * <b>who applied it</b>. Damage, mana return, kill credit and cooldowns all
 * belong to that player even when an ally delivered the hit, so the owner is
 * recorded alongside the effect in the target's persistent data.
 *
 * <p>The effect is always the source of truth for "is this cursed". A stale
 * owner key is harmless because it is only ever read while the effect is
 * present, and applying a curse overwrites it.
 */
public final class CurseState {
	private static final String OWNER_PREFIX = "slr_curse_owner_";
	private static final String STAGE_PREFIX = "slr_curse_stage_";

	private CurseState() {
	}

	/** Applies or refreshes a curse, taking ownership of it at a given power. */
	public static void apply(Entity owner, LivingEntity target, CurseType curse,
			int durationTicks, int stage) {
		if (owner == null || target == null || curse == null || durationTicks <= 0)
			return;
		target.addEffect(new MobEffectInstance(effectFor(curse), durationTicks, 0,
				false, true, true));
		CompoundTag data = target.getPersistentData();
		data.putUUID(ownerKey(curse), owner.getUUID());
		// The stage is frozen at cast. A curse has to keep its own power even
		// after the caster dies, despawns or unloads -- which is the normal case
		// for a mob weaver -- or a purifier would have nothing to measure against.
		data.putInt(stageKey(curse), Math.max(1, stage));
	}

	/** The Intelligence stage the curse was cast at. Defaults to the weakest. */
	public static int stageOf(LivingEntity target, CurseType curse) {
		if (!has(target, curse))
			return 0;
		int stored = target.getPersistentData().getInt(stageKey(curse));
		return stored <= 0 ? 1 : stored;
	}

	public static boolean has(LivingEntity target, CurseType curse) {
		return target != null && curse != null && target.hasEffect(effectFor(curse));
	}

	/** Who applied this curse, or null when it is absent or unowned. */
	public static UUID ownerOf(LivingEntity target, CurseType curse) {
		if (!has(target, curse))
			return null;
		CompoundTag data = target.getPersistentData();
		return data.hasUUID(ownerKey(curse)) ? data.getUUID(ownerKey(curse)) : null;
	}

	public static boolean isOwnedBy(LivingEntity target, CurseType curse, Entity owner) {
		if (owner == null)
			return false;
		UUID recorded = ownerOf(target, curse);
		return recorded != null && recorded.equals(owner.getUUID());
	}

	/** Ticks left on the curse, or zero when it is not active. */
	public static int remaining(LivingEntity target, CurseType curse) {
		if (!has(target, curse))
			return 0;
		MobEffectInstance instance = target.getEffect(effectFor(curse));
		return instance == null ? 0 : instance.getDuration();
	}

	/** Every live curse on the target, in roster order. */
	public static List<CurseType> activeCurses(LivingEntity target) {
		List<CurseType> active = new ArrayList<>();
		if (target == null)
			return active;
		for (CurseType curse : CurseType.values()) {
			if (has(target, curse))
				active.add(curse);
		}
		return active;
	}

	/** Every live curse on the target applied by this owner. */
	public static List<CurseType> activeCursesFrom(LivingEntity target, Entity owner) {
		List<CurseType> active = new ArrayList<>();
		if (target == null || owner == null)
			return active;
		for (CurseType curse : CurseType.values()) {
			if (isOwnedBy(target, curse, owner))
				active.add(curse);
		}
		return active;
	}

	public static int count(LivingEntity target) {
		return activeCurses(target).size();
	}

	/**
	 * Removes a curse outright. This is a removal, not an expiry, so a curse that
	 * pays out when its timer completes deliberately does not pay out here.
	 */
	public static void clear(LivingEntity target, CurseType curse) {
		if (target == null || curse == null)
			return;
		target.removeEffect(effectFor(curse));
		clearOwner(target, curse);
	}

	public static void clearAll(LivingEntity target) {
		for (CurseType curse : CurseType.values())
			clear(target, curse);
	}

	/** Drops the ownership and power record without touching the effect itself. */
	public static void clearOwner(LivingEntity target, CurseType curse) {
		if (target == null || curse == null)
			return;
		target.getPersistentData().remove(ownerKey(curse));
		target.getPersistentData().remove(stageKey(curse));
	}

	public static Holder<MobEffect> effectFor(CurseType curse) {
		return switch (curse) {
			case WITHERING -> SololevelingModMobEffects.CURSE_WITHERING;
			case ENFEEBLEMENT -> SololevelingModMobEffects.CURSE_ENFEEBLEMENT;
			case LEADEN -> SololevelingModMobEffects.CURSE_LEADEN;
			case MANA_ROT -> SololevelingModMobEffects.CURSE_MANA_ROT;
			case BLIGHT -> SololevelingModMobEffects.CURSE_BLIGHT;
			case DOOM -> SololevelingModMobEffects.CURSE_DOOM;
		};
	}

	/** Reverse lookup used by the expiry dispatch. */
	public static CurseType curseFor(Holder<MobEffect> effect) {
		if (effect == null)
			return null;
		for (CurseType curse : CurseType.values()) {
			if (effect.is(effectFor(curse)))
				return curse;
		}
		return null;
	}

	private static String ownerKey(CurseType curse) {
		return OWNER_PREFIX + curse.key();
	}

	private static String stageKey(CurseType curse) {
		return STAGE_PREFIX + curse.key();
	}
}
