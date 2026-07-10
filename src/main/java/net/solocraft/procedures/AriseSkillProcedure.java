package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.ShadowSoulEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.ShadowMonarchManager;
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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AriseSkillProcedure {
	private static final double RANGE = 18.0D;
	private static final int MANA_PER_SOUL = 500;
	private static final int COOLDOWN_TICKS = 40;
	private static final int ARISE_DELAY_TICKS = 12;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof Player player) || !(world instanceof ServerLevel level))
			return;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (vars.JOB != 1)
			return;
		if (player.isShiftKeyDown()) {
			showExtractionScan(player, level, x, y, z, vars);
			return;
		}
		if (CooldownManager.isOnCooldown(player, "arise")) {
			negativePopup(player, "ARISE UNAVAILABLE", "Skill is on cooldown.");
			return;
		}
		int freeStorage = Math.max(0, (int) Math.floor(vars.shadowstorage - vars.shadowstorageusage));
		if (freeStorage <= 0) {
			negativePopup(player, "ARISE FAILED", "Shadow storage is full.");
			return;
		}
		List<ShadowSoulEntity> souls = findExtractableSouls(level, x, y, z);
		if (souls.isEmpty()) {
			negativePopup(player, "ARISE FAILED", "No extractable shadows nearby.");
			return;
		}
		souls.sort(Comparator.comparingInt(AriseSkillProcedure::soulPriority).reversed().thenComparingDouble(soul -> soul.distanceToSqr(player)));
		int attempts = Math.min(freeStorage, souls.size());
		int affordable = Math.min(attempts, (int) Math.floor(vars.MP / MANA_PER_SOUL));
		if (affordable <= 0) {
			negativePopup(player, "ARISE FAILED", "Not enough mana.");
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
				&& Math.max(0, (int) Math.floor(vars.shadowstorage - vars.shadowstorageusage)) > 0
				&& vars.MP >= MANA_PER_SOUL
				&& !findExtractableSouls(level, x, y, z).isEmpty();
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
		int freeStorage = Math.max(0, (int) Math.floor(vars.shadowstorage - vars.shadowstorageusage));
		if (freeStorage <= 0) {
			negativePopup(player, "ARISE FAILED", "Shadow storage is full.");
			return;
		}
		List<ShadowSoulEntity> souls = findExtractableSouls(level, x, y, z);
		if (souls.isEmpty()) {
			negativePopup(player, "ARISE FAILED", "No extractable shadows nearby.");
			return;
		}
		souls.sort(Comparator.comparingInt(AriseSkillProcedure::soulPriority).reversed().thenComparingDouble(soul -> soul.distanceToSqr(player)));
		int attempts = Math.min(freeStorage, souls.size());
		int affordable = Math.min(attempts, (int) Math.floor(vars.MP / MANA_PER_SOUL));
		if (affordable <= 0) {
			negativePopup(player, "ARISE FAILED", "Not enough mana.");
			return;
		}
		int revived = 0;
		for (ShadowSoulEntity soul : souls) {
			if (revived >= affordable)
				break;
			if (reviveSoul(level, soul, player))
				revived++;
		}
		if (revived <= 0) {
			negativePopup(player, "ARISE FAILED", "The shadows resisted extraction.");
			CooldownManager.set(player, "arise", 10);
			return;
		}
		int count = revived;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP = Math.max(0, capability.MP - MANA_PER_SOUL * count);
			capability.syncPlayerVariables(player);
		});
		player.displayClientMessage(Component.literal("\u00A75ARISE x" + revived), true);
	}

	private static boolean reviveSoul(ServerLevel level, ShadowSoulEntity soul, Player player) {
		String soulType = soul.getPersistentData().getString("soultype");
		if (soulType == null || soulType.isBlank())
			return false;
		double chance = successChance(player, soulType);
		if (level.random.nextDouble() >= chance) {
			soul.getPersistentData().putDouble("ariset", soul.getPersistentData().getDouble("ariset") + 1);
			return false;
		}
		String shadowType = shadowType(soulType);
		if (shadowType.isEmpty())
			return false;
		incrementOwnedAndUsage(player, shadowType);
		Vec3 pos = soul.position();
		level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.SHADOW_REVIVE.get(), pos.x, pos.y + 2.0D, pos.z, 1, 0, 0, 0, 0);
		spawnLightning(level, pos);
		Entity summoned = createSummonedShadow(level, shadowType, pos);
		if (summoned != null) {
			if (summoned instanceof TamableAnimal tame)
				tame.tame(player);
			level.addFreshEntity(summoned);
			ShadowMonarchManager.tagExistingSummon(player, summoned, shadowType);
		}
		soul.discard();
		return true;
	}

	private static List<ShadowSoulEntity> findExtractableSouls(ServerLevel level, double x, double y, double z) {
		return level.getEntitiesOfClass(ShadowSoulEntity.class, new AABB(x - RANGE, y - RANGE, z - RANGE, x + RANGE, y + RANGE, z + RANGE),
				soul -> soul.isAlive() && !soul.getPersistentData().getString("soultype").isBlank());
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
			case "tusk" -> SololevelingModEntities.TUSK_SHADOW.get().create(level);
			case "kaisel" -> SololevelingModEntities.SHADOW_KAISELIN.get().create(level);
			default -> null;
		};
		if (entity == null)
			return null;
		entity.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
		if (entity instanceof Mob mob)
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
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

	private static double successChance(Player player, String soulType) {
		double level = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).Level;
		return switch (normalizeSoulType(soulType)) {
			case "orc", "bear" -> Math.min(1.0D, level / 40.0D);
			case "highorc" -> Math.min(1.0D, level / 50.0D);
			case "tusk", "kaisel" -> Math.min(1.0D, level / 70.0D);
			default -> 1.0D;
		};
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
			case "tusk" -> "tusk";
			case "kaisel" -> "kaisel";
			default -> "";
		};
	}

	private static int soulPriority(ShadowSoulEntity soul) {
		return switch (normalizeSoulType(soul.getPersistentData().getString("soultype"))) {
			case "kaisel" -> 1000;
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
}
