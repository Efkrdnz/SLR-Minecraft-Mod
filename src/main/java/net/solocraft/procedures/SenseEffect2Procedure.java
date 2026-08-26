package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.EntityHighlightSystem;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.LevelAccessor;

import java.util.Comparator;
import java.util.List;

/** Periodically turns Perception into a private, short-lived entity sense. */
@EventBusSubscriber
public final class SenseEffect2Procedure {
	private static final String HIGHLIGHT_SOURCE = "perception:sense";
	private static final String NEXT_SENSE_TICK = "slr_next_perception_sense";
	private static final int HIGHLIGHT_DURATION_TICKS = 80;
	private static final int SENSE_COOLDOWN_TICKS = 240;
	private static final int MAX_SENSE_TARGETS = 24;
	private static final double MAX_PERCEPTION = 100.0D;

	private SenseEffect2Procedure() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true && event.getEntity() instanceof ServerPlayer player)
			trySense(player);
	}

	/** Retained for generated callers; perception sensing is authoritative on the server. */
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!world.isClientSide() && entity instanceof ServerPlayer player)
			trySense(player);
	}

	private static void trySense(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
		if (variables == null)
			return;
		double perception = Math.max(0.0D, Math.min(MAX_PERCEPTION, variables.perception));
		long gameTime = player.serverLevel().getGameTime();
		if (gameTime < player.getPersistentData().getLong(NEXT_SENSE_TICK))
			return;
		if (perception <= 0.0D || player.getRandom().nextDouble() > perception / 20_000.0D)
			return;
		player.getPersistentData().putLong(NEXT_SENSE_TICK, gameTime + SENSE_COOLDOWN_TICKS);

		double radius = perception / 2.0D;
		List<LivingEntity> targets = player.serverLevel()
				.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
						target -> eligible(player, target))
				.stream()
				.sorted(Comparator.comparingDouble(player::distanceToSqr))
				.limit(MAX_SENSE_TARGETS)
				.toList();
		for (LivingEntity target : targets) {
			EntityHighlightSystem.show(player, target, HIGHLIGHT_SOURCE,
					EntityHighlightSystem.perceptionColor(target), HIGHLIGHT_DURATION_TICKS,
					EntityHighlightSystem.perceptionPriority(target));
		}
		if (targets.isEmpty())
			return;

		player.playNotifySound(SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.45F, 1.6F);
	}

	private static boolean eligible(ServerPlayer viewer, LivingEntity target) {
		if (target == viewer || !EntityHighlightSystem.isPerceptionCandidate(target))
			return false;
		if (target instanceof TamableAnimal tamable && tamable.isOwnedBy(viewer))
			return false;
		return !viewer.isAlliedTo(target) && !target.isAlliedTo(viewer);
	}
}
