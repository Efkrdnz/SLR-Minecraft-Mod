package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.api.CastSource;
import net.solocraft.entity.BeruDeadBodyEntity;
import net.solocraft.entity.IgrisDeadBodyEntity;
import net.solocraft.entity.ShadowSoulEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.AriseExtractionRules;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.ManaRules;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.TrueMonarchRules;
import net.solocraft.util.SystemNotifications;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AriseSkillProcedure {
	private static final double RANGE = 18.0D;
	private static final int COOLDOWN_TICKS = 40;
	private static final int ARISE_DELAY_TICKS = 12;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(world, x, y, z, entity, CastSource.MANUAL);
	}

	/** @param source presentation only; every gameplay check below is unchanged by it. */
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CastSource source) {
		CastSource castSource = source == null ? CastSource.MANUAL : source;
		if (!(entity instanceof Player player) || !(world instanceof ServerLevel level))
			return;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (vars.JOB != 1)
			return;
		// Sneak+key is a keybind modifier, so it means nothing for a spoken cast.
		// Crouching is an ordinary movement state, and letting it swallow a voice
		// Arise would silently scan instead of casting.
		if (!castSource.isSpokenAloud() && player.isShiftKeyDown()) {
			showExtractionScan(player, level, x, y, z, vars);
			return;
		}
		if (CooldownManager.isOnCooldown(player, "arise")) {
			negativePopup(player, "ARISE UNAVAILABLE", "Skill is on cooldown.");
			return;
		}
		int freeStorage = TrueMonarchRules.freeShadowStorage(vars.trueMonarchHeart,
				vars.shadowstorage, vars.shadowstorageusage);
		if (freeStorage <= 0) {
			negativePopup(player, "ARISE FAILED", "Shadow storage is full.");
			return;
		}
		List<Entity> targets = findExtractableTargets(level, x, y, z, player);
		if (targets.isEmpty()) {
			negativePopup(player, "ARISE FAILED", "No extractable shadows nearby.");
			return;
		}
		targets.sort(Comparator.comparingInt(AriseSkillProcedure::soulPriority).reversed()
				.thenComparingDouble(target -> target.distanceToSqr(player)));
		int attempts = Math.min(freeStorage, targets.size());
		int affordable = AriseExtractionRules.affordableSouls(vars.MP,
				ManaRules.costBasis(player), attempts);
		if (affordable <= 0) {
			negativePopup(player, "ARISE FAILED", "Not enough mana.");
			return;
		}
		if (castSource.isSpokenAloud()) {
			// The player already shouted the word. The recording would talk over them,
			// and the delay exists only so the effect lands on that recording.
			CooldownManager.set(player, "arise", COOLDOWN_TICKS);
			completeArise(player, x, y, z);
			return;
		}
		playAriseSound(level, player.blockPosition(), player.getX(), player.getY(), player.getZ());
		CooldownManager.set(player, "arise", COOLDOWN_TICKS + ARISE_DELAY_TICKS);
		SololevelingMod.queueServerWork(ARISE_DELAY_TICKS, () -> completeArise(player, x, y, z));
	}

	private static void showExtractionScan(Player player, ServerLevel level, double x, double y, double z, SololevelingModVariables.PlayerVariables vars) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		boolean possible = !CooldownManager.isOnCooldown(player, "arise")
				&& TrueMonarchRules.freeShadowStorage(vars.trueMonarchHeart,
						vars.shadowstorage, vars.shadowstorageusage) > 0
				&& vars.MP >= AriseExtractionRules.manaCostForSouls(
						ManaRules.costBasis(player), 1)
				&& findExtractableTargets(level, x, y, z, player).stream()
						.anyMatch(target -> !isTargetOverwhelming(player, target));
		Component title = Component.literal("\u00A76\u00A7lSystem");
		Component under = Component.literal(possible ? "\u00A75[Shadow Extraction]\n \u00A72is possible" : "\u00A75[Shadow Extraction]\n \u00A74is NOT possible");
		if (possible)
			SystemNotifications.showTitleUnder(serverPlayer, 0xFF9B5CFF, 80, title, under);
		else
			SystemNotifications.showNegativeTitleUnder(serverPlayer, 0xFFFF3D3D, 80, title, under);
	}

	private static void completeArise(Player player, double x, double y, double z) {
		if (player == null || !player.isAlive() || !(player.level() instanceof ServerLevel level))
			return;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (vars.JOB != 1)
			return;
		int freeStorage = TrueMonarchRules.freeShadowStorage(vars.trueMonarchHeart,
				vars.shadowstorage, vars.shadowstorageusage);
		if (freeStorage <= 0) {
			negativePopup(player, "ARISE FAILED", "Shadow storage is full.");
			return;
		}
		List<Entity> targets = findExtractableTargets(level, x, y, z, player);
		if (targets.isEmpty()) {
			negativePopup(player, "ARISE FAILED", "No extractable shadows nearby.");
			return;
		}
		targets.sort(Comparator.comparingInt(AriseSkillProcedure::soulPriority).reversed()
				.thenComparingDouble(target -> target.distanceToSqr(player)));
		int attempts = Math.min(freeStorage, targets.size());
		int affordable = AriseExtractionRules.affordableSouls(vars.MP,
				ManaRules.costBasis(player), attempts);
		if (affordable <= 0) {
			negativePopup(player, "ARISE FAILED", "Not enough mana.");
			return;
		}
		// A cast that cannot take every corpse has to say which limit stopped it.
		// Silently raising a subset is what made a mana-starved monarch read this
		// as "Arise only ever raises one".
		int leftBehind = targets.size() - affordable;
		String limit = attempts < targets.size() ? "shadow storage full" : "not enough mana";
		int attempted = 0;
		int revived = 0;
		int overwhelming = 0;
		for (Entity target : targets) {
			if (attempted >= affordable)
				break;
			ExtractionResult result = reviveTarget(level, target, player);
			if (result == ExtractionResult.INVALID)
				continue;
			attempted++;
			if (result == ExtractionResult.SUCCESS)
				revived++;
			else if (result == ExtractionResult.TOO_STRONG)
				overwhelming++;
		}
		if (revived <= 0) {
			negativePopup(player, "ARISE FAILED",
					attempted > 0 && overwhelming == attempted
							? "The target is too strong to extract."
							: "The shadows resisted extraction.");
			CooldownManager.set(player, "arise", 10);
			return;
		}
		double cost = AriseExtractionRules.manaCostForSouls(
				ManaRules.costBasis(player), revived);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP = Math.max(0, capability.MP - cost);
			capability.syncPlayerVariables(player);
		});
		player.displayClientMessage(Component.literal("\u00A75ARISE x" + revived
				+ (leftBehind > 0
						? " \u00A77(" + leftBehind + " left - " + limit + ")"
						: "")), true);
	}

	private static ExtractionResult reviveTarget(ServerLevel level, Entity target,
			Player player) {
		String soulType = soulType(target);
		if (soulType == null || soulType.isBlank())
			return ExtractionResult.INVALID;
		String shadowType = shadowType(soulType);
		if (shadowType.isEmpty() || alreadyOwnsUniqueBoss(player, shadowType))
			return ExtractionResult.INVALID;
		if (isTargetOverwhelming(player, target)) {
			recordFailedExtraction(target);
			return ExtractionResult.TOO_STRONG;
		}
		double chance = successChance(player, target, soulType);
		int previousFailures = failedExtractionCount(target);
		if (!AriseExtractionRules.isGuaranteedAttempt(previousFailures)
				&& level.random.nextDouble() >= chance) {
			recordFailedExtraction(target);
			return ExtractionResult.RESISTED;
		}
		Vec3 pos = target.position();
		Entity summoned = createSummonedShadow(level, shadowType, pos);
		if (summoned == null)
			return ExtractionResult.INVALID;
		incrementOwnedAndUsage(player, shadowType);
		level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.SHADOW_REVIVE.get(), pos.x, pos.y + 2.0D, pos.z, 1, 0, 0, 0, 0);
		spawnLightning(level, pos);
		if (summoned instanceof TamableAnimal tame)
			tame.tame(player);
		level.addFreshEntity(summoned);
		ShadowMonarchManager.tagExistingSummon(player, summoned, shadowType);
		target.discard();
		return ExtractionResult.SUCCESS;
	}

	private static List<Entity> findExtractableTargets(ServerLevel level, double x, double y, double z, Player player) {
		AABB area = new AABB(x - RANGE, y - RANGE, z - RANGE, x + RANGE, y + RANGE, z + RANGE);
		ArrayList<Entity> targets = new ArrayList<>();
		targets.addAll(level.getEntitiesOfClass(ShadowSoulEntity.class, area,
				target -> isExtractableTarget(player, target)));
		targets.addAll(level.getEntitiesOfClass(IgrisDeadBodyEntity.class, area,
				target -> isExtractableTarget(player, target)));
		targets.addAll(level.getEntitiesOfClass(BeruDeadBodyEntity.class, area,
				target -> isExtractableTarget(player, target)));
		return targets;
	}

	private static boolean isExtractableTarget(Player player, Entity target) {
		if (target == null || !target.isAlive())
			return false;
		if (!hasExtractionRights(player, target))
			return false;
		String owner = target.getPersistentData().getString("dkc_spawned_by");
		if (!owner.isBlank() && !owner.equals(player.getStringUUID()))
			return false;
		String type = shadowType(soulType(target));
		return !type.isEmpty() && !alreadyOwnsUniqueBoss(player, type)
				&& !AriseExtractionRules.failuresExhausted(
						failedExtractionCount(target));
	}

	private static boolean hasExtractionRights(Player player, Entity target) {
		if (player == null || target == null)
			return false;
		if (!target.getPersistentData().hasUUID(
				AriseExtractionRules.EXTRACTION_OWNER_TAG))
			return true; // Preserve extraction for corpses created before owner tracking.
		return player.getUUID().equals(target.getPersistentData().getUUID(
				AriseExtractionRules.EXTRACTION_OWNER_TAG));
	}

	private static String soulType(Entity target) {
		if (target instanceof IgrisDeadBodyEntity)
			return "igris";
		if (target instanceof BeruDeadBodyEntity)
			return "beru";
		return target instanceof ShadowSoulEntity
				? target.getPersistentData().getString("soultype")
				: "";
	}

	private static boolean alreadyOwnsUniqueBoss(Player player, String shadowType) {
		SololevelingModVariables.PlayerVariables vars = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		return switch (shadowType) {
			case "igris" -> vars.igris > 0;
			case "beru" -> vars.berumax > 0;
			case "tusk" -> vars.tuskmax > 0;
			case "kaisel" -> vars.Kaisel > 0;
			default -> false;
		};
	}

	private static void recordFailedExtraction(Entity target) {
		if (target instanceof IgrisDeadBodyEntity igris) {
			int failures = AriseExtractionRules.nextFailureCount(
					failedExtractionCount(igris));
			igris.getPersistentData().putInt(
					AriseExtractionRules.FAILURE_COUNT_TAG, failures);
			igris.getEntityData().set(IgrisDeadBodyEntity.DATA_arise, failures);
			if (AriseExtractionRules.failuresExhausted(failures))
				igris.discard();
			return;
		}
		if (target instanceof BeruDeadBodyEntity beru) {
			int remaining = Math.max(0,
					beru.getEntityData().get(BeruDeadBodyEntity.DATA_tries) - 1);
			beru.getEntityData().set(BeruDeadBodyEntity.DATA_tries, remaining);
			if (remaining <= 0)
				beru.discard();
			return;
		}
		int failures = AriseExtractionRules.nextFailureCount(
				failedExtractionCount(target));
		target.getPersistentData().putInt(
				AriseExtractionRules.FAILURE_COUNT_TAG, failures);
		target.getPersistentData().putDouble("ariset", failures);
		if (AriseExtractionRules.failuresExhausted(failures))
			target.discard();
	}

	private static int failedExtractionCount(Entity target) {
		if (target == null)
			return AriseExtractionRules.MAX_BOSS_EXTRACTION_FAILURES;
		if (target.getPersistentData().contains(
				AriseExtractionRules.FAILURE_COUNT_TAG))
			return Math.max(0, target.getPersistentData().getInt(
					AriseExtractionRules.FAILURE_COUNT_TAG));
		if (target instanceof IgrisDeadBodyEntity igris)
			// Legacy corpses started at one before any extraction was attempted.
			return Math.max(0,
					igris.getEntityData().get(IgrisDeadBodyEntity.DATA_arise)
							- 1);
		if (target instanceof BeruDeadBodyEntity beru)
			return Math.max(0,
					AriseExtractionRules.MAX_BOSS_EXTRACTION_FAILURES
							- beru.getEntityData().get(BeruDeadBodyEntity.DATA_tries));
		return Math.max(0,
				(int) Math.floor(target.getPersistentData().getDouble("ariset")));
	}

	private static Entity createSummonedShadow(ServerLevel level, String shadowType, Vec3 pos) {
		Entity entity = switch (shadowType) {
			case "knight" -> SololevelingModEntities.SHADOW_SOLD_1.get().create(level);
			case "goblin_club" -> SololevelingModEntities.GOBLIN_CLUB_SHADOW.get().create(level);
			case "goblin_archer" -> SololevelingModEntities.GOBLIN_ARCHER_SHADOW.get().create(level);
			case "goblin_mage" -> SololevelingModEntities.GOBLIN_MAGE_SHADOW.get().create(level);
			case "wolf" -> SololevelingModEntities.STEEL_FANG_WOLF_SHADOW.get().create(level);
			case "orc" -> SololevelingModEntities.SHADOW_GREEN_ORC.get().create(level);
			case "polar_bear" -> SololevelingModEntities.SHADOW_POLAR_BEAR.get().create(level);
			case "high_orc" -> SololevelingModEntities.SHADOW_HIGH_ORC.get().create(level);
			case "igris" -> SololevelingModEntities.IGRIS_SHADOW.get().create(level);
			case "beru" -> SololevelingModEntities.BERU_SHADOW.get().create(level);
			case "tusk" -> SololevelingModEntities.TUSK_SHADOW.get().create(level);
			case "kaisel" -> SololevelingModEntities.SHADOW_KAISELIN.get().create(level);
			default -> null;
		};
		if (entity == null)
			return null;
		entity.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
		if (entity instanceof Mob mob)
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
		return entity;
	}

	private static void incrementOwnedAndUsage(Player player, String shadowType) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.shadowstorageusage += 1;
			switch (shadowType) {
				case "knight" -> {
					capability.ordshadowmax += 1;
					capability.OrdShadow += 1;
				}
				case "goblin_club" -> {
					capability.GobShadowMax += 1;
					capability.GobShadow += 1;
				}
				case "goblin_archer" -> {
					capability.ShadowGoblinArcherMax += 1;
					capability.ShadowGoblinArcherAmount += 1;
				}
				case "goblin_mage" -> {
					capability.ShadowGoblinMageMax += 1;
					capability.ShadowGoblinMageAmount += 1;
				}
				case "wolf" -> {
					capability.WolfShadowMax += 1;
					capability.WolfShadow += 1;
				}
				case "orc" -> {
					capability.orcmax += 1;
					capability.orcspawned += 1;
					capability.summonlimitusage += 1;
				}
				case "polar_bear" -> {
					capability.polarbearmax += 1;
					capability.polarbear += 1;
					capability.summonlimitusage += 1;
				}
				case "high_orc" -> {
					capability.highorcmax += 1;
					capability.highorcspawned += 1;
					capability.summonlimitusage += 1;
				}
				case "igris" -> {
					capability.igris = Math.max(1, capability.igris);
					capability.IgrisSpawned = Math.max(1, capability.IgrisSpawned);
				}
				case "beru" -> {
					capability.berumax = Math.max(1, capability.berumax);
					capability.beru = Math.max(1, capability.beru);
				}
				case "tusk" -> {
					capability.tuskmax = Math.max(1, capability.tuskmax);
					capability.tuskspawned = Math.max(1, capability.tuskspawned);
				}
				case "kaisel" -> {
					capability.Kaisel = Math.max(1, capability.Kaisel);
					capability.KaiselSpawned = Math.max(1, capability.KaiselSpawned);
				}
				default -> {
				}
			}
			capability.syncPlayerVariables(player);
		});
	}

	private static double successChance(Player player, Entity target, String soulType) {
		double level = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).Level;
		return AriseExtractionRules.successChance(level,
				targetLevel(target, soulType),
				player.getAbilities().instabuild);
	}

	private static boolean isTargetOverwhelming(Player player, Entity target) {
		if (player == null || target == null)
			return true;
		double playerLevel = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).Level;
		return AriseExtractionRules.isOverwhelming(playerLevel,
				targetLevel(target, soulType(target)),
				player.getAbilities().instabuild);
	}

	private static double targetLevel(Entity target, String soulType) {
		double stored = target == null ? 0.0D
				: target.getPersistentData().getDouble(
						AriseExtractionRules.TARGET_LEVEL_TAG);
		return AriseExtractionRules.effectiveTargetLevel(soulType, stored);
	}

	private static String shadowType(String soulType) {
		return switch (normalizeSoulType(soulType)) {
			case "soldier" -> "knight";
			case "goblin" -> "goblin_club";
			case "goblinarc" -> "goblin_archer";
			case "goblinmage" -> "goblin_mage";
			case "wolf" -> "wolf";
			case "orc" -> "orc";
			case "bear" -> "polar_bear";
			case "highorc" -> "high_orc";
			case "igris" -> "igris";
			case "beru" -> "beru";
			case "tusk" -> "tusk";
			case "kaisel" -> "kaisel";
			default -> "";
		};
	}

	private static int soulPriority(Entity target) {
		return switch (normalizeSoulType(soulType(target))) {
			case "beru" -> 1200;
			case "kaisel" -> 1100;
			case "igris" -> 1000;
			case "tusk" -> 900;
			case "highorc" -> 700;
			case "bear" -> 550;
			case "orc" -> 500;
			case "soldier" -> 300;
			case "wolf" -> 250;
			case "goblinmage" -> 220;
			case "goblinarc" -> 210;
			case "goblin" -> 200;
			default -> 0;
		};
	}

	private static String normalizeSoulType(String soulType) {
		return soulType == null ? "" : soulType.trim().toLowerCase(Locale.ROOT).replace("_", "");
	}

	private static void spawnLightning(ServerLevel level, Vec3 pos) {
		LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
		if (lightning == null)
			return;
		lightning.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(pos.x, pos.y - 1, pos.z)));
		lightning.setVisualOnly(true);
		level.addFreshEntity(lightning);
	}

	private static void playAriseSound(Level level, BlockPos pos, double x, double y, double z) {
		if (!level.isClientSide())
			level.playSound(null, pos, SololevelingModSounds.ARISE.get(), SoundSource.NEUTRAL, 1.1F, 0.85F);
		else
			level.playLocalSound(x, y, z, SololevelingModSounds.ARISE.get(), SoundSource.NEUTRAL, 1.1F, 0.85F, false);
	}

	private static void negativePopup(Player player, String title, String undertext) {
		if (player instanceof ServerPlayer serverPlayer) {
			SystemNotifications.showNegativeTitleUnder(serverPlayer, 0xFFFF3D3D, 80,
					Component.literal("§4§l" + title),
					Component.literal("§c" + undertext));
		}
	}

	private enum ExtractionResult {
		SUCCESS,
		RESISTED,
		TOO_STRONG,
		INVALID
	}
}
