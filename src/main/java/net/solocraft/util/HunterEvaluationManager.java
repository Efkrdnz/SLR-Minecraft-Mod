package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.block.entity.HunterRankEvaluatorTileEntity;
import net.solocraft.init.SololevelingModBlocks;
import net.solocraft.item.HunterIDItem;
import net.solocraft.network.HunterEvaluationStateMessage;
import net.solocraft.api.hunter.HunterClassRegistry;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.HunterEvaluationRules.Action;
import net.solocraft.util.HunterEvaluationRules.ClassDraw;
import net.solocraft.util.HunterEvaluationRules.Mode;
import net.solocraft.util.HunterEvaluationRules.Phase;
import net.solocraft.util.ClassStyleRules.StyleDraw;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.joml.Vector3f;

import java.util.UUID;

/**
 * Server-authoritative Hunter Evaluation ceremony. The client can request only
 * contact, reroll, accept, or close actions; all results and timing live here.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class HunterEvaluationManager {
	private static final int SCHEMA = 3;
	private static final String ROOT_KEY = "slr_evaluation";
	private static final String SESSION_KEY = "Session";
	private static final String INITIAL_COMPLETE = "InitialComplete";
	private static final String LEGACY_MIGRATED = "LegacyMigrated";
	private static final double MAX_DISTANCE_SQUARED = 64.0D;

	private HunterEvaluationManager() {
	}

	public static void openEvaluator(ServerPlayer player, BlockPos evaluatorPos) {
		if (player == null || evaluatorPos == null
				|| !(player.level() instanceof ServerLevel level)
				|| !isEvaluator(level.getBlockState(evaluatorPos).getBlock()))
			return;

		CompoundTag data = evaluationData(player);
		migrateLegacy(player, data);
		Session session = readSession(data);
		long now = level.getGameTime();
		if (session == null) {
			session = createSession(player, data, now);
			writeSession(data, session);
		} else if (ensureSessionStyle(player, session)) {
			writeSession(data, session);
		}

		session.stationDimension = level.dimension().location().toString();
		session.stationPos = evaluatorPos.asLong();
		session.hasStation = true;
		if (session.deadline == 0L && session.pausedTicks > 0) {
			session.deadline = now + session.pausedTicks;
			session.pausedTicks = 0;
		}
		writeSession(data, session);
		activatePublicPulse(level, evaluatorPos, session, now, false);
		level.playSound(null, evaluatorPos, SoundEvents.AMETHYST_BLOCK_CHIME,
				SoundSource.BLOCKS, 0.75F, 1.05F);
		sendState(player, session, true);
	}

	public static void handleAction(ServerPlayer player, UUID sessionId,
			Action action) {
		if (player == null || sessionId == null || action == null
				|| !(player.level() instanceof ServerLevel level))
			return;
		CompoundTag data = evaluationData(player);
		Session session = readSession(data);
		if (session == null || !session.id.equals(sessionId)
				|| !isNearStation(player, session))
			return;

		long now = level.getGameTime();
		boolean changed = switch (action) {
			case BEGIN_CONTACT -> beginContact(session, now);
			case CANCEL_CONTACT -> cancelContact(session);
			case REROLL_CLASS -> reroll(player, session, now);
			case ACCEPT_RESULT -> accept(player, data, session, now);
			case ACKNOWLEDGE -> acknowledge(player, data, session);
			case REROLL_STYLE -> rerollStyle(player, session, now);
		};
		if (!changed)
			return;
		if (action == Action.ACKNOWLEDGE)
			return;

		writeSession(data, session);
		BlockPos pos = session.station();
		activatePublicPulse(level, pos, session, now, true);
		sendState(player, session, false);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false
				|| !(event.getEntity() instanceof ServerPlayer player))
			return;
		CompoundTag data = evaluationDataIfPresent(player);
		if (data == null)
			return;
		Session session = readSession(data);
		if (session == null)
			return;

		ServerLevel level = player.serverLevel();
		long now = level.getGameTime();
		if (!isNearStation(player, session)) {
			if (session.hasStation) {
				pauseAwayFromStation(session, now);
				writeSession(data, session);
				sendClosed(player);
			}
			return;
		}

		boolean advanced = false;
		for (int guard = 0; guard < 8
				&& session.deadline > 0L && now >= session.deadline; guard++) {
			advance(player, session, now);
			advanced = true;
		}
		if (advanced) {
			writeSession(data, session);
			activatePublicPulse(level, session.station(), session, now, true);
			sendState(player, session, false);
		}
		emitPublicParticles(level, session, now);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		CompoundTag data = evaluationDataIfPresent(player);
		if (data == null)
			return;
		Session session = readSession(data);
		if (session == null || !session.hasStation)
			return;
		pauseAwayFromStation(session, player.serverLevel().getGameTime());
		writeSession(data, session);
	}

	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		if (!(event.getEntity() instanceof ServerPlayer replacement))
			return;
		CompoundTag originalRoot = event.getOriginal().getPersistentData();
		if (!originalRoot.contains(Player.PERSISTED_NBT_TAG,
				Tag.TAG_COMPOUND))
			return;
		CompoundTag originalPersisted =
				originalRoot.getCompound(Player.PERSISTED_NBT_TAG);
		if (!originalPersisted.contains(ROOT_KEY, Tag.TAG_COMPOUND))
			return;
		persisted(replacement).put(ROOT_KEY,
				originalPersisted.getCompound(ROOT_KEY).copy());
	}

	private static Session createSession(ServerPlayer player, CompoundTag data,
			long now) {
		SololevelingModVariables.PlayerVariables variables = variables(player);
		boolean initialComplete = data.getBoolean(INITIAL_COMPLETE);
		int currentClass = boundedClass(variables.Classes);
		int earnedRank = boundedRank(variables.HunterRank);
		int certifiedRank = boundedRank(variables.prevRank);

		if (initialComplete && currentClass > 0 && earnedRank > 0) {
			int result = resolvedEvaluationRank(player, variables, earnedRank);
			int styleId = savedStyleId(variables, currentClass);
			// A re-evaluating hunter whose class gained styles after they were
			// evaluated has no saved style yet. Show the class's first style
			// rather than leaving the readout unresolved, and only write the
			// Mage compatibility alias for an actual Mage.
			if (ClassStyleRules.supportsStyles(currentClass)
					&& !ClassStyleRules.isValidStyle(currentClass, styleId)) {
				styleId = firstEnabledStyle(currentClass);
				if (currentClass == ClassStyleRules.MAGE_CLASS_ID)
					MageSpellProgression.setSpecialization(player,
							ClassStyleRules.styleKey(currentClass, styleId), false);
			}
			int styleMask = ClassStyleRules.allStylesMask(currentClass);
			if (ClassStyleRules.isValidStyle(currentClass, styleId))
				styleMask &= ~(1 << (styleId - 1));
			return Session.create(Mode.REEVALUATION, Phase.CONTACT,
					currentClass, styleId, result, certifiedRank,
					HunterEvaluationRules.drawableClassMask(), true, styleMask,
					now, 0);
		}

		boolean fixedClass = currentClass > 0;
		int candidateRank = earnedRank > 0 ? earnedRank
				: HunterRankOdds.roll(player.serverLevel(),
						player.getRandom().nextInt(100) + 1);
		int resultRank = resolvedEvaluationRank(player, variables,
				candidateRank);
		// The pool starts as everything drawable, so a contributed class can be
		// the very first thing the Evaluator shows.
		int drawable = HunterEvaluationRules.drawableClassMask();
		int remainingMask = drawable;
		if (currentClass == 0) {
			ClassDraw draw = HunterEvaluationRules.drawClass(remainingMask, 0,
					player.getRandom().nextInt(), drawable);
			currentClass = draw.classId();
			remainingMask = draw.remainingMask();
		} else {
			remainingMask &= ~(1 << (currentClass - 1));
		}
		int styleId = fixedClass ? savedStyleId(variables, currentClass) : 0;
		int styleMask = ClassStyleRules.allStylesMask(currentClass);
		if (ClassStyleRules.supportsStyles(currentClass)) {
			if (ClassStyleRules.isValidStyle(currentClass, styleId)) {
				styleMask &= ~(1 << (styleId - 1));
			} else {
				StyleDraw style = ClassStyleRules.drawStyle(currentClass,
						styleMask, 0, player.getRandom().nextInt());
				styleId = style.styleId();
				styleMask = style.remainingMask();
			}
		}
		return Session.create(Mode.INITIAL, Phase.BOOT, currentClass,
				styleId, resultRank, certifiedRank, remainingMask, fixedClass,
				styleMask, now,
				HunterEvaluationRules.INITIAL_BOOT_TICKS);
	}

	/** Lowest enabled style id for a class, or 0 when the roster is gated. */
	private static int firstEnabledStyle(int classId) {
		int mask = ClassStyleRules.allStylesMask(classId);
		return mask == 0 ? 0 : Integer.numberOfTrailingZeros(mask) + 1;
	}

	private static int savedStyleId(
			SololevelingModVariables.PlayerVariables variables, int classId) {
		int generic = ClassStyleRules.styleId(classId, variables.classStyle);
		if (generic > 0)
			return generic;
		return ClassStyleRules.styleId(classId,
				variables.mageSpecialization);
	}

	/** Initializes style fields on a pre-style pending session. */
	private static boolean ensureSessionStyle(ServerPlayer player,
			Session session) {
		if (session.styleInitialized)
			return false;
		session.styleInitialized = true;
		if (!ClassStyleRules.supportsStyles(session.classId)) {
			session.styleId = 0;
			return true;
		}

		session.remainingStyleMask =
				ClassStyleRules.allStylesMask(session.classId);
		if (session.mode == Mode.REEVALUATION) {
			session.styleId = savedStyleId(variables(player), session.classId);
			if (!ClassStyleRules.isValidStyle(session.classId,
					session.styleId))
				session.styleId = firstEnabledStyle(session.classId);
			session.remainingStyleMask &= ~(1 << (session.styleId - 1));
		} else {
			drawStyleForCurrentClass(player, session);
		}
		return true;
	}

	private static void drawStyleForCurrentClass(ServerPlayer player,
			Session session) {
		if (!ClassStyleRules.supportsStyles(session.classId)) {
			session.styleId = 0;
			session.styleInitialized = true;
			return;
		}
		StyleDraw draw = ClassStyleRules.drawStyle(session.classId,
				session.remainingStyleMask, session.styleId,
				player.getRandom().nextInt());
		session.styleId = draw.styleId();
		session.remainingStyleMask = draw.remainingMask();
		session.styleInitialized = true;
	}

	private static boolean beginContact(Session session, long now) {
		if (session.phase != Phase.CONTACT || session.deadline > 0L)
			return false;
		session.phaseStarted = now;
		session.phaseDuration = HunterEvaluationRules.CONTACT_TICKS;
		session.deadline = now + HunterEvaluationRules.CONTACT_TICKS;
		session.pausedTicks = 0;
		return true;
	}

	private static boolean cancelContact(Session session) {
		if (session.phase != Phase.CONTACT || session.deadline <= 0L)
			return false;
		session.phaseStarted = 0L;
		session.phaseDuration = 0;
		session.deadline = 0L;
		session.pausedTicks = 0;
		return true;
	}

	private static boolean reroll(ServerPlayer player, Session session,
			long now) {
		if (session.mode != Mode.INITIAL || session.phase != Phase.DECISION
				|| session.fixedClass)
			return false;
		ClassDraw draw = HunterEvaluationRules.drawClass(
				session.remainingClassMask, session.classId,
				player.getRandom().nextInt(),
				HunterEvaluationRules.drawableClassMask());
		session.classId = draw.classId();
		session.remainingClassMask = draw.remainingMask();
		drawStyleForCurrentClass(player, session);
		startPhase(session, Phase.REROLL, now);
		return true;
	}

	private static boolean rerollStyle(ServerPlayer player, Session session,
			long now) {
		if (session.mode != Mode.INITIAL || session.phase != Phase.DECISION
				|| !ClassStyleRules.supportsStyles(session.classId))
			return false;
		drawStyleForCurrentClass(player, session);
		startPhase(session, Phase.REROLL_STYLE, now);
		return true;
	}

	private static boolean accept(ServerPlayer player, CompoundTag data,
			Session session, long now) {
		if (session.phase != Phase.DECISION)
			return false;
		if (session.mode == Mode.INITIAL)
			commitInitial(player, data, session);
		else
			commitReevaluation(player, session);
		startPhase(session, Phase.COMPLETE, now);
		return true;
	}

	private static boolean acknowledge(ServerPlayer player, CompoundTag data,
			Session session) {
		if (session.phase != Phase.COMPLETE)
			return false;
		data.remove(SESSION_KEY);
		sendClosed(player);
		return true;
	}

	private static void commitInitial(ServerPlayer player, CompoundTag data,
			Session session) {
		int selectedClass = boundedClass(session.classId);
		int resultRank = resolvedEvaluationRank(player, variables(player),
				session.rank);
		session.rank = resultRank;
		if (selectedClass == 0 || resultRank == 0)
			return;
		String selectedStyle = ClassStyleRules.styleKey(selectedClass,
				session.styleId);
		if (ClassStyleRules.supportsStyles(selectedClass)
				&& selectedStyle.isBlank())
			return;

		// A contributed class carries no number of its own, so its identity goes
		// through the registry and the numeric field takes the reserved mirror.
		boolean builtIn = HunterEvaluationRules.contributedClass(selectedClass).isEmpty();
		HunterEvaluationRules.contributedClass(selectedClass)
			.ifPresent(contributed -> HunterClassRegistry.assign(player, contributed));

		player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					if (builtIn)
						capability.Classes = selectedClass;
					capability.classStyle = selectedStyle;
					if (selectedClass == ClassStyleRules.MAGE_CLASS_ID)
						capability.mageSpecialization = selectedStyle;
					capability.HunterRank = resultRank;
					capability.prevRank = resultRank;
					capability.prevLevel = capability.Level;
					capability.ranking =
							HunterEvaluationRules.rankName(resultRank);
					capability.statshown = 0;
					capability.sl_EVA = 0;
					capability.syncPlayerVariables(player);
				});
		data.putBoolean(INITIAL_COMPLETE, true);
		HunterEvaluationRewardService.applyInitialRewards(player,
				selectedClass, resultRank, data);
		HunterIDItem.refreshAll(player);
		// The association registers a class, not a style. A Healer is announced as
		// a Healer; which style they took is their own business.
		String publicStyle = ClassStyleRules.publicStyleName(selectedClass, session.styleId);
		String resultType = selectedStyle.isBlank() || publicStyle.isEmpty()
				? HunterEvaluationRules.className(selectedClass)
				: publicStyle;
		player.displayClientMessage(Component.literal(
				"Hunter Association registration complete: "
						+ HunterEvaluationRules.rankName(resultRank) + "-Rank "
						+ resultType + "."),
				false);
	}

	private static void commitReevaluation(ServerPlayer player,
			Session session) {
		player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					int earned = boundedRank(capability.HunterRank);
					int result = resolvedEvaluationRank(player, capability,
							earned);
					capability.HunterRank = result;
					capability.prevRank = result;
					capability.prevLevel = capability.Level;
					capability.ranking =
							HunterEvaluationRules.rankName(result);
					capability.syncPlayerVariables(player);
					session.rank = result;
				});
		HunterIDItem.refreshAll(player);
		String message = session.rank > session.previousRank
				? "Reevaluation complete. Your Hunter ID is now certified "
						+ HunterEvaluationRules.rankName(session.rank) + "-Rank."
				: "Reevaluation complete. Your certified rank is unchanged.";
		player.displayClientMessage(Component.literal(message), false);
	}

	private static void advance(ServerPlayer player, Session session,
			long now) {
		switch (session.phase) {
			case BOOT -> startPhase(session, Phase.CONTACT, now);
			case CONTACT -> startPhase(session, Phase.SCAN, now);
			case SCAN -> {
				if (session.mode == Mode.INITIAL) {
					startPhase(session, Phase.CLASS_REVEAL, now);
				} else {
					refreshReevaluationResult(player, session);
					startPhase(session, Phase.RANK_REVEAL, now);
				}
			}
			case CLASS_REVEAL -> startPhase(session,
					ClassStyleRules.supportsStyles(session.classId)
							? Phase.STYLE_REVEAL : Phase.RANK_REVEAL,
					now);
			case STYLE_REVEAL ->
					startPhase(session, Phase.RANK_REVEAL, now);
			case RANK_REVEAL -> startPhase(session,
					session.mode == Mode.INITIAL
							? Phase.SETTLE : Phase.DECISION, now);
			case SETTLE, REROLL_STYLE ->
					startPhase(session, Phase.DECISION, now);
			case REROLL -> startPhase(session,
					ClassStyleRules.supportsStyles(session.classId)
							? Phase.REROLL_STYLE : Phase.DECISION,
					now);
			default -> {
				session.deadline = 0L;
				session.phaseDuration = 0;
			}
		}
	}

	private static void refreshReevaluationResult(ServerPlayer player,
			Session session) {
		SololevelingModVariables.PlayerVariables capability = variables(player);
		session.rank = resolvedEvaluationRank(player, capability,
				boundedRank(capability.HunterRank));
	}

	private static int resolvedEvaluationRank(ServerPlayer player,
			SololevelingModVariables.PlayerVariables capability,
			int candidateRank) {
		return HunterEvaluationRules.resolvedEvaluationRank(candidateRank,
				boundedRank(capability.prevRank),
				Math.max(0, (int) Math.floor(capability.Level)),
				VesselManager.currentDefinition(player) != null);
	}

	private static void startPhase(Session session, Phase phase, long now) {
		session.phase = phase;
		session.phaseStarted = now;
		// Contact is the one player-driven phase. It must enter unarmed: only
		// beginContact may start its countdown, or the ceremony would advance on
		// a timer whether or not the player is actually holding the gem.
		// An S-Rank reveal instead runs long, covering the error report.
		session.phaseDuration = phase == Phase.CONTACT ? 0
				: phase == Phase.RANK_REVEAL
						? HunterEvaluationRules.rankRevealDuration(session.rank)
						: HunterEvaluationRules.phaseDuration(session.mode, phase);
		session.deadline = session.phaseDuration > 0
				? now + session.phaseDuration : 0L;
		session.pausedTicks = 0;
	}

	private static void pauseAwayFromStation(Session session, long now) {
		if (session.phase == Phase.CONTACT) {
			session.deadline = 0L;
			session.phaseDuration = 0;
			session.pausedTicks = 0;
		} else if (session.deadline > now) {
			session.pausedTicks = (int) Math.min(Integer.MAX_VALUE,
					session.deadline - now);
			session.deadline = 0L;
		}
		session.hasStation = false;
		session.stationDimension = "";
		session.stationPos = 0L;
	}

	private static void activatePublicPulse(ServerLevel level, BlockPos pos,
			Session session, long now, boolean playTransitionSound) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		long until = session.deadline > now ? session.deadline : now + 40L;
		if (blockEntity instanceof HunterRankEvaluatorTileEntity evaluator)
			evaluator.setPublicPulse(session.id,
					pulseColor(session), pulseIntensity(session),
					session.phase.ordinal(), until);

		if (playTransitionSound) {
			SoundEvent sound = switch (session.phase) {
				case CONTACT -> SoundEvents.AMETHYST_BLOCK_CHIME;
				case SCAN -> SoundEvents.BEACON_ACTIVATE;
				case CLASS_REVEAL, REROLL, STYLE_REVEAL, REROLL_STYLE ->
						SoundEvents.ENCHANTMENT_TABLE_USE;
				case RANK_REVEAL -> session.rank == 6
						? SoundEvents.WITHER_SPAWN
						: SoundEvents.PLAYER_LEVELUP;
				case COMPLETE -> SoundEvents.TOTEM_USE;
				default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
			};
			level.playSound(null, pos, sound, SoundSource.BLOCKS,
					session.rank == 6 && session.phase == Phase.RANK_REVEAL
							? 0.8F : 0.65F,
					phasePitch(session));
			spawnPulseBurst(level, pos, session);
		}
	}

	private static void emitPublicParticles(ServerLevel level, Session session,
			long now) {
		if (now % 3L != 0L)
			return;
		BlockEntity blockEntity = level.getBlockEntity(session.station());
		if (!(blockEntity instanceof HunterRankEvaluatorTileEntity evaluator)
				|| !evaluator.isPublicPulseOwner(session.id, now))
			return;
		DustParticleOptions dust = dust(session, 1.0F);
		BlockPos pos = session.station();
		level.sendParticles(dust, pos.getX() + 0.5D,
				pos.getY() + 1.55D, pos.getZ() + 0.5D, 2,
				0.30D, 0.34D, 0.30D, 0.008D);
		if (session.rank == 6
				&& HunterEvaluationRules.revealsRank(session.phase)
				&& now % 6L == 0L)
			level.sendParticles(ParticleTypes.END_ROD,
					pos.getX() + 0.5D, pos.getY() + 1.55D,
					pos.getZ() + 0.5D, 1,
					0.16D, 0.22D, 0.16D, 0.004D);
	}

	private static void spawnPulseBurst(ServerLevel level, BlockPos pos,
			Session session) {
		level.sendParticles(dust(session, 1.35F), pos.getX() + 0.5D,
				pos.getY() + 1.55D, pos.getZ() + 0.5D, 28,
				0.46D, 0.50D, 0.46D, 0.025D);
		if (session.rank == 6
				&& HunterEvaluationRules.revealsRank(session.phase))
			level.sendParticles(ParticleTypes.END_ROD,
					pos.getX() + 0.5D, pos.getY() + 1.55D,
					pos.getZ() + 0.5D, 16,
					0.34D, 0.42D, 0.34D, 0.02D);
	}

	private static DustParticleOptions dust(Session session, float scale) {
		int rgb = pulseColor(session);
		float intensity = pulseIntensity(session);
		float red = ((rgb >> 16) & 0xFF) / 255.0F;
		float green = ((rgb >> 8) & 0xFF) / 255.0F;
		float blue = (rgb & 0xFF) / 255.0F;
		if (session.rank == 6
				&& HunterEvaluationRules.revealsRank(session.phase)) {
			red = Math.min(1.0F, red * 0.70F + 0.55F);
			green = Math.min(1.0F, green * 0.70F + 0.55F);
			blue = Math.min(1.0F, blue * 0.70F + 0.55F);
		} else {
			red = Math.max(0.05F, red * intensity);
			green = Math.max(0.05F, green * intensity);
			blue = Math.max(0.05F, blue * intensity);
		}
		return new DustParticleOptions(new Vector3f(red, green, blue), scale);
	}

	private static int pulseColor(Session session) {
		if (HunterEvaluationRules.revealsStyle(session.mode,
				session.classId, session.phase))
			return ClassStyleRules.styleColor(session.classId,
					session.styleId) & 0xFFFFFF;
		return HunterEvaluationRules.revealsClass(session.mode, session.phase)
				? HunterEvaluationRules.classColor(session.classId)
				: HunterEvaluationRules.classColor(0);
	}

	private static float pulseIntensity(Session session) {
		return HunterEvaluationRules.revealsRank(session.phase)
				? HunterEvaluationRules.rankIntensity(session.rank) : 0.48F;
	}

	private static float phasePitch(Session session) {
		if (session.phase == Phase.RANK_REVEAL)
			return 0.72F + session.rank * 0.08F;
		return session.phase == Phase.COMPLETE ? 1.18F : 1.0F;
	}

	private static void sendState(ServerPlayer player, Session session,
			boolean forceOpen) {
		long now = player.serverLevel().getGameTime();
		int remaining = session.deadline > now
				? (int) Math.min(Integer.MAX_VALUE, session.deadline - now)
				: session.pausedTicks;
		boolean revealClass = HunterEvaluationRules.revealsClass(
				session.mode, session.phase);
		boolean revealRank =
				HunterEvaluationRules.revealsRank(session.phase);
		boolean revealStyle = HunterEvaluationRules.revealsStyle(
				session.mode, session.classId, session.phase);
		boolean canReroll = session.mode == Mode.INITIAL
				&& session.phase == Phase.DECISION && !session.fixedClass;
		boolean canRerollStyle = session.mode == Mode.INITIAL
				&& session.phase == Phase.DECISION
				&& ClassStyleRules.supportsStyles(session.classId);
		SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new HunterEvaluationStateMessage(true, forceOpen, session.id,
						session.mode.ordinal(), session.phase.ordinal(),
						revealClass ? session.classId : 0,
						revealRank ? session.rank : 0,
						session.previousRank, session.phaseDuration, remaining,
						canReroll, session.fixedClass,
						revealStyle ? session.styleId : 0,
						canRerollStyle));
	}

	private static void sendClosed(ServerPlayer player) {
		SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new HunterEvaluationStateMessage(false, false,
						new UUID(0L, 0L), 0, 0, 0, 0, 0,
						0, 0, false, false, 0, false));
	}

	private static boolean isNearStation(ServerPlayer player,
			Session session) {
		if (!session.hasStation
				|| !player.level().dimension().location().toString()
						.equals(session.stationDimension))
			return false;
		BlockPos pos = session.station();
		if (!isEvaluator(player.level().getBlockState(pos).getBlock()))
			return false;
		double dx = player.getX() - (pos.getX() + 0.5D);
		double dy = player.getY() - (pos.getY() + 0.5D);
		double dz = player.getZ() - (pos.getZ() + 0.5D);
		return dx * dx + dy * dy + dz * dz <= MAX_DISTANCE_SQUARED;
	}

	private static boolean isEvaluator(Block block) {
		return block == SololevelingModBlocks.HUNTER_RANK_EVALUATOR.get()
				|| block == SololevelingModBlocks.EVALUATOR_TEST.get();
	}

	private static void migrateLegacy(ServerPlayer player, CompoundTag data) {
		if (data.getInt("Schema") >= SCHEMA
				&& data.getBoolean(LEGACY_MIGRATED))
			return;
		SololevelingModVariables.PlayerVariables capability = variables(player);
		int classId = boundedClass(capability.Classes);
		int rank = boundedRank(capability.HunterRank);
		boolean hasClass = classId > 0;
		boolean hasRank = rank > 0;
		if (classId == ClassStyleRules.MAGE_CLASS_ID) {
			int styleId = savedStyleId(capability, classId);
			if (!ClassStyleRules.isValidStyle(classId, styleId))
				styleId = ClassStyleRules.FIRE_MAGE;
			String styleKey = ClassStyleRules.styleKey(classId, styleId);
			capability.classStyle = styleKey;
			capability.mageSpecialization = styleKey;
		}
		boolean legacyMidCeremony = capability.sl_EVA >= 10.0D
				&& capability.sl_EVA < 130.0D && hasClass && hasRank;

		if (legacyMidCeremony) {
			int mask = HunterEvaluationRules.ALL_CLASSES_MASK
					& ~(1 << (classId - 1));
			int styleId = savedStyleId(capability, classId);
			int styleMask = ClassStyleRules.allStylesMask(classId);
			if (ClassStyleRules.isValidStyle(classId, styleId))
				styleMask &= ~(1 << (styleId - 1));
			Session session = Session.create(Mode.INITIAL, Phase.DECISION,
					classId, styleId, rank, boundedRank(capability.prevRank),
					mask, false, styleMask,
					player.serverLevel().getGameTime(), 0);
			writeSession(data, session);
			data.putBoolean(INITIAL_COMPLETE, false);
		} else if (hasClass && hasRank) {
			data.putBoolean(INITIAL_COMPLETE, true);
			HunterEvaluationRewardService.markLegacyRewardsApplied(data);
			if (capability.prevRank <= 0.0D)
				capability.prevRank = rank;
		}

		capability.sl_EVA = 0.0D;
		capability.statshown = 0.0D;
		capability.syncPlayerVariables(player);
		data.putInt("Schema", SCHEMA);
		data.putBoolean(LEGACY_MIGRATED, true);
	}

	private static CompoundTag evaluationData(ServerPlayer player) {
		CompoundTag persisted = persisted(player);
		if (!persisted.contains(ROOT_KEY, Tag.TAG_COMPOUND))
			persisted.put(ROOT_KEY, new CompoundTag());
		return persisted.getCompound(ROOT_KEY);
	}

	private static CompoundTag evaluationDataIfPresent(ServerPlayer player) {
		CompoundTag root = player.getPersistentData();
		if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			return null;
		CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
		return persisted.contains(ROOT_KEY, Tag.TAG_COMPOUND)
				? persisted.getCompound(ROOT_KEY) : null;
	}

	private static CompoundTag persisted(ServerPlayer player) {
		CompoundTag root = player.getPersistentData();
		if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
		return root.getCompound(Player.PERSISTED_NBT_TAG);
	}

	private static Session readSession(CompoundTag data) {
		return data.contains(SESSION_KEY, Tag.TAG_COMPOUND)
				? Session.load(data.getCompound(SESSION_KEY)) : null;
	}

	private static void writeSession(CompoundTag data, Session session) {
		data.put(SESSION_KEY, session.save());
		data.putInt("Schema", SCHEMA);
		data.putBoolean(LEGACY_MIGRATED, true);
	}

	private static SololevelingModVariables.PlayerVariables variables(
			ServerPlayer player) {
		return player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	/**
	 * Bounds the legacy {@code Classes} field, which only ever holds a built-in
	 * number. A contributed class stores the reserved mirror there, which is not
	 * a drawable id and correctly reads as zero.
	 */
	private static int boundedClass(double value) {
		int classId = (int) Math.round(value);
		return classId >= 1 && classId <= HunterEvaluationRules.CLASS_COUNT
				? classId : 0;
	}

	/**
	 * Bounds a class id held by a live session, which may be contributed.
	 *
	 * <p>Separate from {@link #boundedClass} because the two answer different
	 * questions. Running a session's id through the legacy bound turned every
	 * contributed draw into zero the first time the session was written to NBT
	 * and read back, which stranded the ceremony mid-reroll: the phase machine
	 * had nothing to advance and the screen sat on a full progress bar forever.
	 */
	private static int boundedSessionClass(int value) {
		return value >= 1 && value <= HunterEvaluationRules.MAX_CLASS_ID
				? value : 0;
	}

	private static int boundedRank(double value) {
		int rank = (int) Math.round(value);
		return rank >= 1 && rank <= 6 ? rank : 0;
	}

	private static final class Session {
		private UUID id;
		private Mode mode;
		private Phase phase;
		private int classId;
		private int styleId;
		private int rank;
		private int previousRank;
		private int remainingClassMask;
		private boolean fixedClass;
		private int remainingStyleMask;
		private boolean styleInitialized;
		private long phaseStarted;
		private long deadline;
		private int phaseDuration;
		private int pausedTicks;
		private boolean hasStation;
		private String stationDimension = "";
		private long stationPos;

		private static Session create(Mode mode, Phase phase, int classId,
				int styleId, int rank, int previousRank,
				int remainingClassMask, boolean fixedClass,
				int remainingStyleMask, long now, int duration) {
			Session session = new Session();
			session.id = UUID.randomUUID();
			session.mode = mode;
			session.phase = phase;
			session.classId = classId;
			session.styleId = ClassStyleRules.isValidStyle(classId, styleId)
					? styleId : 0;
			session.rank = rank;
			session.previousRank = previousRank;
			session.remainingClassMask = remainingClassMask;
			session.fixedClass = fixedClass;
			session.remainingStyleMask = remainingStyleMask
					& ClassStyleRules.allStylesMask(classId);
			session.styleInitialized = true;
			session.phaseStarted = now;
			session.phaseDuration = Math.max(0, duration);
			session.deadline = duration > 0 ? now + duration : 0L;
			return session;
		}

		private static Session load(CompoundTag tag) {
			if (!tag.hasUUID("Id"))
				return null;
			Session session = new Session();
			session.id = tag.getUUID("Id");
			session.mode = Mode.fromId(tag.getInt("Mode"));
			session.phase = Phase.fromId(tag.getInt("Phase"));
			session.classId = boundedSessionClass(tag.getInt("Class"));
			session.styleId = tag.getInt("Style");
			session.rank = boundedRank(tag.getInt("Rank"));
			session.previousRank = boundedRank(tag.getInt("PreviousRank"));
			// Masking with the built-in mask stripped the contributed bits out of
			// the bag on every save, so a contributed class could only appear on a
			// refill rather than taking its turn in the shuffle.
			session.remainingClassMask = tag.getInt("RemainingClassMask")
					& HunterEvaluationRules.drawableClassMask();
			session.fixedClass = tag.getBoolean("FixedClass");
			session.remainingStyleMask = tag.getInt("RemainingStyleMask")
					& ClassStyleRules.allStylesMask(session.classId);
			session.styleInitialized = tag.contains("Style")
					&& (!ClassStyleRules.supportsStyles(session.classId)
							|| ClassStyleRules.isValidStyle(session.classId,
									session.styleId));
			session.phaseStarted = tag.getLong("PhaseStarted");
			session.deadline = tag.getLong("Deadline");
			session.phaseDuration = Math.max(0, tag.getInt("PhaseDuration"));
			session.pausedTicks = Math.max(0, tag.getInt("PausedTicks"));
			session.hasStation = tag.getBoolean("HasStation");
			session.stationDimension = tag.getString("StationDimension");
			session.stationPos = tag.getLong("StationPos");
			if (session.classId == 0 || session.rank == 0)
				return null;
			return session;
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("Id", id);
			tag.putInt("Mode", mode.ordinal());
			tag.putInt("Phase", phase.ordinal());
			tag.putInt("Class", classId);
			tag.putInt("Style", styleId);
			tag.putInt("Rank", rank);
			tag.putInt("PreviousRank", previousRank);
			tag.putInt("RemainingClassMask", remainingClassMask);
			tag.putBoolean("FixedClass", fixedClass);
			tag.putInt("RemainingStyleMask", remainingStyleMask);
			tag.putLong("PhaseStarted", phaseStarted);
			tag.putLong("Deadline", deadline);
			tag.putInt("PhaseDuration", phaseDuration);
			tag.putInt("PausedTicks", pausedTicks);
			tag.putBoolean("HasStation", hasStation);
			tag.putString("StationDimension",
					stationDimension == null ? "" : stationDimension);
			tag.putLong("StationPos", stationPos);
			return tag;
		}

		private BlockPos station() {
			return BlockPos.of(stationPos);
		}
	}
}
