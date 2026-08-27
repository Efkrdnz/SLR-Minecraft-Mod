package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.HealerVfxEntity;
import net.solocraft.entity.StatueOfGodEntity;
import net.solocraft.entity.StatueaxeEntity;
import net.solocraft.entity.StatuehammerEntity;
import net.solocraft.entity.StatueswordEntity;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The return to the Cartenon Temple.
 *
 * <p>The player awakened here, in front of the Statue of God, and this is where
 * the run ends. The Architect -- the voice behind the System -- speaks through
 * System popups rather than as an entity, because a presence with no body is
 * both more unsettling and free of any model, animation or AI work.
 *
 * <p>The arc is a small state machine per player, held in persistent data so it
 * survives a relog mid-run, while the permanent "this happened" bits live in
 * {@link CartenonProgressSavedData}. Stages:
 *
 * <ol>
 *   <li>{@code DIALOGUE} - the Architect speaks; nothing is hostile yet.</li>
 *   <li>{@code GUARDIANS} - every posed statue in the instance wakes at once.</li>
 *   <li>{@code GOD} - the Statue of God, which killed the player in the intro.</li>
 *   <li>{@code CHOICE} - release the System, or walk away and keep it.</li>
 * </ol>
 */
@EventBusSubscriber
public final class CartenonFinaleManager {
	// Deliberately NOT under the "slr_cartenon_" prefix: PlayerResetKeyPolicy
	// preserves that prefix across a progress reset to protect awakening state,
	// which would strand a reset player permanently mid-arc. These belong to the
	// run, so they must be cleared with it.
	public static final String STAGE_TAG = "slr_temple_finale_stage";
	public static final String LINE_TAG = "slr_temple_finale_line";
	public static final String NEXT_LINE_AT_TAG = "slr_temple_finale_next";
	public static final String INSTANCE_TAG = "slr_temple_finale_instance";
	public static final String STARTED_AT_TAG = "slr_temple_finale_started";
	private static final String GUARDIAN_MARKER = "slr_temple_finale_guardian";
	private static final String GOD_MARKER = StatueOfGodEntity.FINALE_BOSS_TAG;
	private static final String OWNER_TAG = "slr_temple_finale_owner";

	/** Set by the generator on every posed statue it places. */
	private static final String TEMPLE_STATUE_TAG = "CartenonTempleStatue";
	/** The intro's marker. It disables the Statue of God's melee goal outright. */
	private static final String STORY_STATUE_TAG = "slr_story_intro_statue";

	public static final String STAGE_DIALOGUE = "dialogue";
	public static final String STAGE_GUARDIANS = "guardians";
	public static final String STAGE_GOD = "god";
	public static final String STAGE_CHOICE = "choice";

	/** Ticks between Architect lines. Slow enough to read, short enough to keep. */
	private static final int LINE_INTERVAL = 70;
	/**
	 * Grace before leaving the temple counts as abandoning the run, so a tick
	 * that lands mid-teleport cannot cancel the arc the instant it begins.
	 */
	private static final int ABANDON_GRACE_TICKS = 60;
	private static final String WITHDRAW_TICKS_TAG = "slr_temple_withdraw_ticks";
	/** Four seconds. Longer than the entry kneel: crouching mid-fight is normal. */
	private static final int WITHDRAW_REQUIRED_TICKS = 80;

	/** Woken-guardian combat stats. Their posed defaults are 0 damage, 0.1 speed. */
	private static final double GUARDIAN_DAMAGE = 9.0D;
	private static final double GUARDIAN_SPEED = 0.28D;
	/**
	 * The Statue of God's melee, down from a base of 85.
	 *
	 * <p>85 is most of a full health bar in one swing. The fight is meant to be
	 * survivable and readable, so the pressure moves to the telegraphed eye beam
	 * where the player is given a second to get out of the way.
	 */
	private static final double GOD_MELEE_DAMAGE = 18.0D;

	// --- Eye beam. Telegraphed, avoidable, and devastating if it lands.
	private static final String BEAM_NEXT_AT = "slr_temple_beam_next";
	private static final String BEAM_FIRE_AT = "slr_temple_beam_fire";
	private static final String BEAM_X = "slr_temple_beam_x";
	private static final String BEAM_Y = "slr_temple_beam_y";
	private static final String BEAM_Z = "slr_temple_beam_z";
	/** Warning time between the lock and the shot. */
	private static final int BEAM_TELEGRAPH_TICKS = 25;
	private static final int BEAM_COOLDOWN_TICKS = 110;
	private static final double BEAM_RANGE = 48.0D;
	private static final float BEAM_RADIUS = 3.5F;
	/** Far above the melee, because standing in a marked circle is a choice. */
	private static final float BEAM_DAMAGE = 45.0F;
	private static final int BEAM_WARN_COLOR = 0xFFFF5A3C;
	private static final int BEAM_COLOR = 0xFFFFE27A;

	/**
	 * The Architect's script.
	 *
	 * <p>The boolean is whether the line uses the negative System chime. That
	 * sound means "you failed" everywhere else in the mod, so it is spent only on
	 * the Architect's first intrusion and on its threats -- using it for every
	 * line would blunt what it means and grate across a long scene.
	 */
	private static final Line[] SCRIPT = {
			new Line("SYSTEM ERROR", "An unregistered process is accessing this channel.", true),
			new Line("???", "So. You climbed all of it.", false),
			new Line("ARCHITECT", "I built the ladder. I did not expect anyone to finish it.", false),
			new Line("ARCHITECT", "You were a vessel. A container for someone else's war.", false),
			new Line("ARCHITECT", "Every quest. Every level. Every reward. Mine.", false),
			new Line("ARCHITECT", "And now you stand where I first measured you.", false),
			new Line("WARNING", "The temple no longer recognizes you as a guest.", true),
	};

	private CartenonFinaleManager() {
	}

	/** Begins the arc for a player who has just entered the finale instance. */
	public static void begin(ServerPlayer player, int instanceId) {
		if (player == null)
			return;
		player.getPersistentData().putString(STAGE_TAG, STAGE_DIALOGUE);
		player.getPersistentData().putInt(LINE_TAG, 0);
		player.getPersistentData().putInt(INSTANCE_TAG, Math.max(1, instanceId));
		player.getPersistentData().putLong(STARTED_AT_TAG, player.serverLevel().getGameTime());
		player.getPersistentData().putLong(NEXT_LINE_AT_TAG,
				player.serverLevel().getGameTime() + 40L);
	}

	public static boolean isActive(ServerPlayer player) {
		return player != null && !stage(player).isEmpty();
	}

	public static String stage(ServerPlayer player) {
		return player == null ? "" : player.getPersistentData().getString(STAGE_TAG);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| !(player.level() instanceof ServerLevel level))
			return;
		String stage = stage(player);
		if (stage.isEmpty())
			return;
		// Leaving the temple abandons the run. The offer stays open, so the player
		// can come back; nothing is lost by walking out.
		if (player.serverLevel().dimension() != CartenonTempleManager.CARTENON_DIMENSION) {
			if (level.getGameTime() - player.getPersistentData().getLong(STARTED_AT_TAG)
					> ABANDON_GRACE_TICKS)
				abandon(player);
			return;
		}
		tickWithdraw(player, level);
		if (STAGE_DIALOGUE.equals(stage)) {
			tickDialogue(player, level);
			return;
		}
		// The Statue of God has no target-selector goals of its own -- the intro
		// drove it entirely by script. Without this it forgets the player the
		// moment the target clears and stands there for the rest of the fight.
		if (STAGE_GOD.equals(stage)) {
			if (player.tickCount % 20 == 0)
				retargetGod(player, level);
			tickEyeBeam(player, level);
		}
	}

	/**
	 * The Statue of God's eye beam.
	 *
	 * <p>Two phases on purpose. A thin marker locks a spot on the floor and holds
	 * for {@link #BEAM_TELEGRAPH_TICKS} with a rising tone; only then does the
	 * real beam land there, hard. The damage is high enough to respect precisely
	 * because it is avoidable -- the player is told exactly where and given a
	 * second to not be standing in it. An unavoidable hit that big would just be
	 * a damage check, and a thin beam that chips at you is not a Monarch.
	 */
	private static void tickEyeBeam(ServerPlayer player, ServerLevel level) {
		StatueOfGodEntity god = markedGod(player, level);
		if (god == null)
			return;
		long now = level.getGameTime();
		long fireAt = god.getPersistentData().getLong(BEAM_FIRE_AT);

		if (fireAt <= 0L) {
			if (now < god.getPersistentData().getLong(BEAM_NEXT_AT))
				return;
			if (!god.hasLineOfSight(player) || god.distanceTo(player) > BEAM_RANGE)
				return;
			lockBeam(god, player, level, now);
			return;
		}
		Vec3 aim = new Vec3(god.getPersistentData().getDouble(BEAM_X),
				god.getPersistentData().getDouble(BEAM_Y),
				god.getPersistentData().getDouble(BEAM_Z));
		if (now < fireAt) {
			// Held marker: redrawn every few ticks so it reads as a lock, and it
			// does NOT follow the player -- that is what makes it dodgeable.
			if (now % 4L == 0L) {
				HealerVfxEntity.beam(level, god.getEyePosition(), aim, 1, BEAM_WARN_COLOR, 6);
				HealerVfxEntity.sigil(level, aim, 1, BEAM_RADIUS, BEAM_WARN_COLOR, 6);
			}
			return;
		}
		fireBeam(god, level, aim);
		god.getPersistentData().putLong(BEAM_FIRE_AT, 0L);
		god.getPersistentData().putLong(BEAM_NEXT_AT, now + BEAM_COOLDOWN_TICKS);
	}

	private static void lockBeam(StatueOfGodEntity god, ServerPlayer player,
			ServerLevel level, long now) {
		Vec3 aim = player.position();
		god.getPersistentData().putDouble(BEAM_X, aim.x);
		god.getPersistentData().putDouble(BEAM_Y, aim.y);
		god.getPersistentData().putDouble(BEAM_Z, aim.z);
		god.getPersistentData().putLong(BEAM_FIRE_AT, now + BEAM_TELEGRAPH_TICKS);
		level.playSound(null, god.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
				SoundSource.HOSTILE, 2.0F, 0.5F);
	}

	private static void fireBeam(StatueOfGodEntity god, ServerLevel level, Vec3 aim) {
		HealerVfxEntity.beam(level, god.getEyePosition(), aim, 5, BEAM_COLOR, 14);
		HealerVfxEntity.wave(level, aim, 5, BEAM_RADIUS, BEAM_COLOR, 18);
		level.playSound(null, BlockPos.containing(aim), SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.HOSTILE, 2.4F, 0.6F);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
				aim.x, aim.y + 1.0D, aim.z, 8, BEAM_RADIUS * 0.4D, 0.6D, BEAM_RADIUS * 0.4D, 0.02D);
		for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
				new AABB(aim.subtract(BEAM_RADIUS, 3.0D, BEAM_RADIUS),
						aim.add(BEAM_RADIUS, 4.0D, BEAM_RADIUS)),
				candidate -> candidate.isAlive() && !(candidate instanceof StatueOfGodEntity)))
			caught.hurt(level.damageSources().indirectMagic(god, god), BEAM_DAMAGE);
	}

	private static StatueOfGodEntity markedGod(ServerPlayer player, ServerLevel level) {
		for (StatueOfGodEntity god : level.getEntitiesOfClass(StatueOfGodEntity.class,
				instanceBounds(player), Entity::isAlive)) {
			if (god.getPersistentData().getBoolean(GOD_MARKER))
				return god;
		}
		return null;
	}

	/**
	 * Kneeling again withdraws from the run.
	 *
	 * <p>The same gesture that opened the way closes it, so entering and leaving
	 * are one idea rather than two. Held longer than the entry kneel because a
	 * mid-fight crouch is common and this must never fire by accident. Leaving
	 * cancels the run and re-opens the summons, so nothing is lost.
	 */
	private static void tickWithdraw(ServerPlayer player, ServerLevel level) {
		if (STAGE_CHOICE.equals(stage(player)))
			return;
		if (!player.isShiftKeyDown() || !player.onGround()) {
			player.getPersistentData().remove(WITHDRAW_TICKS_TAG);
			return;
		}
		int held = player.getPersistentData().getInt(WITHDRAW_TICKS_TAG) + 1;
		if (held >= WITHDRAW_REQUIRED_TICKS) {
			player.getPersistentData().remove(WITHDRAW_TICKS_TAG);
			abandon(player);
			CartenonTempleManager.returnToOverworld(player);
			SystemNotifications.showTitleUnder(player, 0xFF3FC6FF, 110,
					Component.literal("WITHDRAWN").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
					Component.literal("The way back remains open to you.")
							.withStyle(ChatFormatting.GRAY));
			return;
		}
		player.getPersistentData().putInt(WITHDRAW_TICKS_TAG, held);
		if (held == 20)
			SystemNotifications.showUnder(player, 0xFF3FC6FF, 60,
					Component.literal("Keep kneeling to withdraw from the temple.")
							.withStyle(ChatFormatting.GRAY));
	}

	private static void retargetGod(ServerPlayer player, ServerLevel level) {
		for (StatueOfGodEntity god : level.getEntitiesOfClass(StatueOfGodEntity.class,
				instanceBounds(player), Entity::isAlive)) {
			if (!god.getPersistentData().getBoolean(GOD_MARKER))
				continue;
			if (god.getTarget() == null || !god.getTarget().isAlive())
				god.setTarget(player);
		}
	}

	/**
	 * Gives a scenery statue the stats of an actual opponent.
	 *
	 * <p>The guardians ship with {@code ATTACK_DAMAGE 0} and
	 * {@code MOVEMENT_SPEED 0.1} because until now they only ever stood in a
	 * corridor: they dealt no damage at all and moved at well under half a
	 * zombie's pace. Set on the instance rather than in {@code createAttributes}
	 * so the ones still posed as decoration stay harmless.
	 */
	private static void wakeCombatStats(Mob statue) {
		if (statue instanceof StatueOfGodEntity) {
			// The god hits like a truck by default. See MELEE_DAMAGE.
			setBase(statue, Attributes.ATTACK_DAMAGE, GOD_MELEE_DAMAGE);
			setBase(statue, Attributes.MOVEMENT_SPEED, 0.32D);
			setBase(statue, Attributes.FOLLOW_RANGE, 128.0D);
			return;
		}
		setBase(statue, Attributes.ATTACK_DAMAGE, GUARDIAN_DAMAGE);
		setBase(statue, Attributes.MOVEMENT_SPEED, GUARDIAN_SPEED);
		setBase(statue, Attributes.FOLLOW_RANGE, 96.0D);
		setBase(statue, Attributes.KNOCKBACK_RESISTANCE, 0.6D);
	}

	private static void setBase(Mob statue, Holder<Attribute> attribute, double value) {
		AttributeInstance instance = statue.getAttribute(attribute);
		if (instance != null)
			instance.setBaseValue(value);
	}

	/**
	 * Keeps the whole temple loaded for the duration of the arc.
	 *
	 * <p>The hall is 154 blocks deep. Without a ticket the guardians at the far
	 * end and the Statue of God on its dais sit in unloaded chunks: the entity
	 * search finds nothing, so the arc spawns a duplicate god and the distant
	 * guardians never wake or path.
	 */
	private static void holdTempleLoaded(ServerLevel level, int instanceId) {
		AABB bounds = CartenonTempleManager.instanceBounds(instanceId);
		ChunkPos min = new ChunkPos(BlockPos.containing(bounds.minX, 0, bounds.minZ));
		ChunkPos max = new ChunkPos(BlockPos.containing(bounds.maxX, 0, bounds.maxZ));
		for (int x = min.x; x <= max.x; x++) {
			for (int z = min.z; z <= max.z; z++)
				level.getChunk(x, z);
		}
	}

	private static AABB instanceBounds(ServerPlayer player) {
		return CartenonTempleManager.instanceBounds(
				Math.max(1, player.getPersistentData().getInt(INSTANCE_TAG)));
	}

	/**
	 * Brings a posed statue to life.
	 *
	 * <p>The generator places every temple statue with {@code setNoAi(true)} so it
	 * stands as scenery. Without clearing that, waking the temple set a target on
	 * a frozen mob and the whole finale was a room of statues being hit until
	 * they fell over.
	 */
	private static void animate(Mob statue, ServerPlayer owner, String marker) {
		statue.setNoAi(false);
		wakeCombatStats(statue);
		statue.getPersistentData().putBoolean(marker, true);
		statue.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
		// The intro's marker hard-disables the Statue of God's melee goal.
		statue.getPersistentData().remove(STORY_STATUE_TAG);
		statue.setPersistenceRequired();
		statue.setTarget(owner);
		// The hall is 154 blocks deep with side rooms. A quest to destroy every
		// guardian is unfinishable if the last one cannot be found, so they light
		// up as they wake.
		statue.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE,
				0, false, false, false));
	}

	private static void tickDialogue(ServerPlayer player, ServerLevel level) {
		long now = level.getGameTime();
		if (now < player.getPersistentData().getLong(NEXT_LINE_AT_TAG))
			return;
		int index = player.getPersistentData().getInt(LINE_TAG);
		if (index >= SCRIPT.length) {
			wakeGuardians(player, level);
			return;
		}
		Line line = SCRIPT[index];
		if (line.negative)
			SystemNotifications.showNegativeTitleUnder(player, 0xFFFF3D3D, LINE_INTERVAL,
					Component.literal(line.title).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
					Component.literal(line.body).withStyle(ChatFormatting.GRAY));
		else
			SystemNotifications.showTitleUnder(player, 0xFF9B5CFF, LINE_INTERVAL,
					Component.literal(line.title).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
					Component.literal(line.body).withStyle(ChatFormatting.WHITE));
		player.getPersistentData().putInt(LINE_TAG, index + 1);
		player.getPersistentData().putLong(NEXT_LINE_AT_TAG, now + LINE_INTERVAL);
	}

	/**
	 * Every posed statue in the instance comes alive at once.
	 *
	 * <p>Found by bounds search the way the intro finds the god statue
	 * ({@code StoryModeIntroManager#findAndMarkGodStatue}), with a spawn fallback,
	 * so this works whether or not the generated temple has them placed.
	 */
	private static void wakeGuardians(ServerPlayer player, ServerLevel level) {
		holdTempleLoaded(level, player.getPersistentData().getInt(INSTANCE_TAG));
		List<Mob> guardians = findGuardians(level, player);
		if (guardians.isEmpty())
			guardians = spawnGuardians(level, player);
		for (Mob guardian : guardians)
			animate(guardian, player, GUARDIAN_MARKER);
		player.getPersistentData().putString(STAGE_TAG, STAGE_GUARDIANS);
		level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
				SoundSource.HOSTILE, 1.0F, 0.6F);
		UrgentQuestManager.startCartenonFinaleQuest(player, false, guardians.size());
	}

	/**
	 * Every posed guardian in the instance.
	 *
	 * <p>Searched across the whole temple rather than a radius around the player:
	 * the generator places guardians from the entrance all the way back to the
	 * dais, and the player arrives eight blocks inside the door.
	 */
	private static List<Mob> findGuardians(ServerLevel level, ServerPlayer player) {
		AABB bounds = instanceBounds(player);
		List<Mob> found = new ArrayList<>();
		found.addAll(level.getEntitiesOfClass(StatueswordEntity.class, bounds, Entity::isAlive));
		found.addAll(level.getEntitiesOfClass(StatueaxeEntity.class, bounds, Entity::isAlive));
		found.addAll(level.getEntitiesOfClass(StatuehammerEntity.class, bounds, Entity::isAlive));
		return found;
	}

	/** Fallback when the instance has no posed statues to wake. */
	private static List<Mob> spawnGuardians(ServerLevel level, ServerPlayer player) {
		List<Mob> spawned = new ArrayList<>();
		BlockPos origin = player.blockPosition();
		for (int index = 0; index < 6; index++) {
			double angle = index * (Math.PI * 2.0D / 6.0D);
			BlockPos at = origin.offset((int) Math.round(Math.cos(angle) * 7.0D), 0,
					(int) Math.round(Math.sin(angle) * 7.0D));
			Mob guardian = switch (index % 3) {
				case 0 -> net.solocraft.init.SololevelingModEntities.STATUESWORD.get().create(level);
				case 1 -> net.solocraft.init.SololevelingModEntities.STATUEAXE.get().create(level);
				default -> net.solocraft.init.SololevelingModEntities.STATUEHAMMER.get().create(level);
			};
			if (guardian == null)
				continue;
			guardian.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D,
					level.random.nextFloat() * 360.0F, 0.0F);
			guardian.getPersistentData().putBoolean(TEMPLE_STATUE_TAG, true);
			if (level.addFreshEntity(guardian))
				spawned.add(guardian);
		}
		return spawned;
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		LivingEntity dead = event.getEntity();
		if (dead == null || !(dead.level() instanceof ServerLevel level))
			return;
		if (!dead.getPersistentData().hasUUID(OWNER_TAG))
			return;
		ServerPlayer owner = level.getServer().getPlayerList()
				.getPlayer(dead.getPersistentData().getUUID(OWNER_TAG));
		if (owner == null)
			return;
		if (dead.getPersistentData().getBoolean(GUARDIAN_MARKER)) {
			onGuardianDown(owner, level);
			return;
		}
		if (dead.getPersistentData().getBoolean(GOD_MARKER))
			onGodDown(owner, level);
	}

	private static void onGuardianDown(ServerPlayer owner, ServerLevel level) {
		if (!STAGE_GUARDIANS.equals(stage(owner)))
			return;
		// Counted by what is actually still standing rather than by a stored
		// tally: a guardian lost to a void, a despawn or a second player's blade
		// would otherwise leave the arc permanently stuck one kill short.
		List<Mob> remaining = findGuardians(level, owner).stream()
				.filter(guardian -> guardian.getPersistentData().getBoolean(GUARDIAN_MARKER))
				.toList();
		UrgentQuestManager.updateCartenonFinaleProgress(owner, remaining.size());
		if (!remaining.isEmpty())
			return;
		awakenGod(owner, level);
	}

	private static void awakenGod(ServerPlayer owner, ServerLevel level) {
		// Load first, then look. The dais sits 145 blocks from the entrance, so an
		// unloaded chunk here reads as "no statue" and spawns a second one into a
		// room that already has one.
		holdTempleLoaded(level, owner.getPersistentData().getInt(INSTANCE_TAG));
		StatueOfGodEntity god = level.getEntitiesOfClass(StatueOfGodEntity.class,
				instanceBounds(owner), Entity::isAlive).stream()
				.findFirst().orElse(null);
		if (god == null) {
			god = net.solocraft.init.SololevelingModEntities.STATUE_OF_GOD.get().create(level);
			if (god != null) {
				god.moveTo(owner.getX(), owner.getY(), owner.getZ() + 8.0D, 0.0F, 0.0F);
				if (!level.addFreshEntity(god))
					god = null;
			}
		}
		if (god == null) {
			// Without the statue there is no finale to fight. Fail open to the
			// choice rather than stranding the player in a cleared temple.
			beginChoice(owner);
			return;
		}
		// The authority for this entity is the PERSISTENT "state" string, not the
		// synced DATA_state. StatueOfGodOnEntityTickUpdateProcedure forces
		// setNoAi(true) back on every tick unless that string reads "aggresive",
		// so setting entity data alone left it frozen, seated and unable to fight.
		//
		// "waking" is the entity's own stand-up sequence: it rises in place over
		// 72 ticks and then goes aggressive by itself. No teleport -- it gets up
		// where it has always stood and the player comes to it.
		god.getPersistentData().putBoolean(GOD_MARKER, true);
		god.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
		god.getPersistentData().remove(STORY_STATUE_TAG);
		god.getPersistentData().putString("state", "waking");
		god.getPersistentData().putInt("IA", 0);
		god.getEntityData().set(StatueOfGodEntity.DATA_state, "waking");
		god.getEntityData().set(StatueOfGodEntity.DATA_story_upright, false);
		god.setPersistenceRequired();
		god.setNoAi(true);
		god.setTarget(owner);
		god.getNavigation().stop();
		wakeCombatStats(god);
		owner.getPersistentData().putString(STAGE_TAG, STAGE_GOD);
		SystemNotifications.showNegativeTitleUnder(owner, 0xFFFF3D3D, 100,
				Component.literal("ARCHITECT").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.literal("Then I will measure you myself.").withStyle(ChatFormatting.GRAY));
		level.playSound(null, god.blockPosition(), SoundEvents.WARDEN_ROAR,
				SoundSource.HOSTILE, 1.2F, 0.5F);
		UrgentQuestManager.startCartenonFinaleQuest(owner, true, 1);
	}

	private static void onGodDown(ServerPlayer owner, ServerLevel level) {
		if (!STAGE_GOD.equals(stage(owner)))
			return;
		UrgentQuestManager.updateCartenonFinaleProgress(owner, 0);
		level.playSound(null, owner.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
				SoundSource.HOSTILE, 1.3F, 0.5F);
		beginChoice(owner);
	}

	/**
	 * The ending. There is no prompt and no way back.
	 *
	 * <p>An accept/decline here would have made the climax a dialog box, and a
	 * player who declined would be left standing in a temple they had already
	 * emptied with nothing to do. Killing the Statue of God <em>is</em> the
	 * decision; the Architect concedes, and a few seconds later the System is
	 * the player's.
	 */
	private static void beginChoice(ServerPlayer owner) {
		owner.getPersistentData().putString(STAGE_TAG, STAGE_CHOICE);
		SystemNotifications.showTitleUnder(owner, 0xFF9B5CFF, 120,
				Component.literal("ARCHITECT").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
				Component.literal("Then take it. I have nothing left to administer.")
						.withStyle(ChatFormatting.WHITE));
		SololevelingMod.queueServerWork(70, () -> completeRelease(owner));
	}

	/**
	 * Applies the ending. The System keeps running -- nobody is at the controls
	 * any more -- and the player takes what the Architect was holding.
	 */
	public static void completeRelease(ServerPlayer player) {
		if (player == null)
			return;
		SystemAuthorityManager.release(player);
		int job = (int) player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.JOB).orElse(0.0D).doubleValue();
		String heart = TrueMonarchRules.rewardForJob(job);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.trueMonarchHeart = heart;
					capability.syncPlayerVariables(player);
				});
		CartenonProgressSavedData.get(player.serverLevel()).resolveFinale(player.getUUID());
		clearState(player);
		grantAdvancement(player, TrueMonarchRules.hasBlackHeart(heart)
				? "system/black_heart" : "system/true_vessel");
		SystemNotifications.showTitleUnder(player, 0xFF9B5CFF, 160,
				Component.literal(TrueMonarchRules.displayName(heart).toUpperCase(java.util.Locale.ROOT))
						.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
				Component.literal(TrueMonarchRules.hasBlackHeart(heart)
						? "The shadows have no ceiling now."
						: "Your vessel is whole.").withStyle(ChatFormatting.WHITE));
		// The temple has no exit of its own. Without this the run ends with the
		// player sealed inside the instance they just cleared, holding a reward
		// they cannot take anywhere.
		SololevelingMod.queueServerWork(80, () -> {
			CartenonTempleManager.returnToOverworld(player);
			SystemNotifications.showUnder(player, 0xFF9B5CFF, 100,
					Component.literal("The temple is closed. It has nothing left to measure.")
							.withStyle(ChatFormatting.GRAY));
		});
	}

	/** Marks the ending on the advancement tree. */
	private static void grantAdvancement(ServerPlayer player, String path) {
		AdvancementHolder advancement = player.server.getAdvancements()
				.get(ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, path));
		if (advancement == null)
			return;
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		if (progress.isDone())
			return;
		for (String criterion : progress.getRemainingCriteria())
			player.getAdvancements().award(advancement, criterion);
	}

	private static void abandon(ServerPlayer player) {
		clearState(player);
		CartenonProgressSavedData.get(player.serverLevel()).cancelFinaleOffer(player.getUUID());
	}

	private static void clearState(ServerPlayer player) {
		player.getPersistentData().remove(STAGE_TAG);
		player.getPersistentData().remove(LINE_TAG);
		player.getPersistentData().remove(NEXT_LINE_AT_TAG);
		player.getPersistentData().remove(INSTANCE_TAG);
	}

	private record Line(String title, String body, boolean negative) {
	}
}
