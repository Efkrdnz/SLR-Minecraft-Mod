package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.EntityHighlightSystem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Hyperfocus control effect with a caster-only target outline. */
public final class IntenseFocusProcedure {
	private static final String HIGHLIGHT_SOURCE = "skill:intense_focus";
	private static final int DURATION_TICKS = 200;

	private IntenseFocusProcedure() {
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof ServerPlayer player))
			return;
		Vec3 center = new Vec3(x, y, z);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(30.0D),
				target -> target != player && EntityHighlightSystem.isPerceptionCandidate(target))
				.stream().sorted(Comparator.comparingDouble(target -> target.distanceToSqr(center))).toList();
		String party = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).party;
		for (LivingEntity target : targets) {
			String targetParty = target.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.orElse(new SololevelingModVariables.PlayerVariables()).party;
			if (!party.isBlank() && party.equals(targetParty))
				continue;
			EntityHighlightSystem.show(player, target, HIGHLIGHT_SOURCE,
					EntityHighlightSystem.perceptionColor(target), DURATION_TICKS,
					EntityHighlightSystem.PRIORITY_PERCEPTION + 50);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS, 2,
					false, false));
		}
	}
}
