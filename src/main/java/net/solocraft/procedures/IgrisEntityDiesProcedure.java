package net.solocraft.procedures;

import net.solocraft.entity.BloodRedComIgrisEntity;
import net.solocraft.entity.SpawnerPortalEntity;
import net.solocraft.init.SololevelingModBlocks;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.util.SystemNotifications;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.List;

@Mod.EventBusSubscriber
public class IgrisEntityDiesProcedure {
	private static final ResourceKey<Level> IGRIS_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			new ResourceLocation("sololeveling:dungeon_dimension_igris"));
	private static final int PORTAL_SEARCH_RADIUS = 50;
	private static final int MAX_ADVANCEMENT_PORTALS = 24;

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof BloodRedComIgrisEntity)
			execute(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
					event.getEntity(), event.getSource().getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (!(entity instanceof BloodRedComIgrisEntity) || !(world instanceof ServerLevel level))
			return;
		ServerPlayer killer = ShadowKillCreditHelper.creditedServerPlayer(world, sourceentity);
		if (killer == null)
			return;

		killer.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (capability.Player)
				capability.giftstatus = true;
			capability.syncPlayerVariables(killer);
		});

		if (!level.dimension().equals(IGRIS_DIMENSION)) {
			SystemNotifications.showTitleUnder(killer, 0xFFDF9607, 90,
					Component.literal("IGRIS DEFEATED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal("Leveled Up!").withStyle(ChatFormatting.YELLOW));
			return;
		}

		List<ServerPlayer> participants = JobChangeQuestManager.beginAdvancementPhase(killer);
		for (ServerPlayer participant : participants) {
			participant.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.giftstatus = true;
				capability.syncPlayerVariables(participant);
			});
		}
		spawnAdvancementPortals(level, BlockPos.containing(x, y, z));
	}

	private static void spawnAdvancementPortals(ServerLevel level, BlockPos center) {
		if (!level.getEntitiesOfClass(SpawnerPortalEntity.class, new AABB(center).inflate(120.0D)).isEmpty())
			return;

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int spawned = 0;
		for (int dx = -PORTAL_SEARCH_RADIUS; dx < PORTAL_SEARCH_RADIUS && spawned < MAX_ADVANCEMENT_PORTALS; dx++) {
			for (int dy = -PORTAL_SEARCH_RADIUS; dy < PORTAL_SEARCH_RADIUS && spawned < MAX_ADVANCEMENT_PORTALS; dy++) {
				for (int dz = -PORTAL_SEARCH_RADIUS; dz < PORTAL_SEARCH_RADIUS && spawned < MAX_ADVANCEMENT_PORTALS; dz++) {
					cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (!level.getBlockState(cursor).is(SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get()))
						continue;
					Entity portal = SololevelingModEntities.SPAWNER_PORTAL.get().spawn(level,
							BlockPos.containing(cursor.getX(), cursor.getY() + 1.2D, cursor.getZ()), MobSpawnType.MOB_SUMMONED);
					if (portal != null) {
						portal.setYRot(level.random.nextFloat() * 360.0F);
						portal.getPersistentData().putBoolean("slr_job_change_advancement_portal", true);
						spawned++;
					}
				}
			}
		}
	}
}
