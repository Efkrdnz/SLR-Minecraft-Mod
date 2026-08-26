package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.TemporaryStatBonusManager;
import net.solocraft.init.SololevelingModParticleTypes;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.solocraft.util.MageCombatHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Comparator;

@EventBusSubscriber
public class RushAttackProcedureProcedure {
	/** Targets swept per dash tick, so a crowd cannot turn one dash into a nuke. */
	private static final int TARGET_CAP = 4;

	/**
	 * Server-side only, and only while a dash is actually running. The previous
	 * form was guarded by {@code if (true)} and performed a capability lookup
	 * for every player of every class, every tick, forever.
	 */
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player))
			return;
		if (!player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(vars -> vars.rushattack).orElse(false))
			return;
		execute(event, player.level(), player.getX(), player.getY(), player.getZ(), player);
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).rushattack == true) {
			entity.setDeltaMovement(new Vec3((entity.getLookAngle().x * 1.2), (-1), (entity.getLookAngle().z * 1.2)));
			{
				// Combat-filtered and capped. The previous query used
				// Entity.class with `e -> true`, so a dash hit party members,
				// tamed pets, item entities and neutral players alike.
				final Vec3 _center = new Vec3(x, y, z);
				DamageSource source = new DamageSource(world.registryAccess()
						.registryOrThrow(Registries.DAMAGE_TYPE)
						.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,
								ResourceLocation.parse("sololeveling:fighter"))), entity);
				List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class,
								new AABB(_center, _center).inflate(2.0D),
								candidate -> MageCombatHelper.isValidTarget(entity, candidate))
						.stream()
						.sorted(Comparator.comparingDouble(target -> target.distanceToSqr(_center)))
						.limit(TARGET_CAP)
						.toList();
				for (LivingEntity target : targets) {
					target.hurt(source,
							(float) (4 + TemporaryStatBonusManager.effectiveStrength(entity) / 20.0D));
					target.setDeltaMovement(new Vec3(entity.getLookAngle().x * 2, -1,
							entity.getLookAngle().z * 2));
					target.hurtMarked = true;
				}
			}
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 3, 1, 1, 1, 0);
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GOODSLASH_1.get()), x, y, z, 3, 1, 1, 1, 0);
		}
	}
}
