package net.solocraft.procedures;

import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;
import java.util.Comparator;

public class AirTornadoProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double rep = 0;
		double delay = 0;
		entity.getPersistentData().putDouble("pX", (entity.getX() + entity.getLookAngle().x));
		entity.getPersistentData().putDouble("pY", (entity.getY() + 1.5));
		entity.getPersistentData().putDouble("pZ", (entity.getZ() + entity.getLookAngle().z));
		entity.getPersistentData().putDouble("vX", (entity.getLookAngle().x * 0.1));
		entity.getPersistentData().putDouble("vY", (entity.getLookAngle().y * 0.1));
		entity.getPersistentData().putDouble("vZ", (entity.getLookAngle().z * 0.1));
		entity.getPersistentData().putDouble("rep2", 0);
		for (int index0 = 0; index0 < 200; index0++) {
			rep = rep + 0.5;
			SololevelingMod.queueServerWork((int) rep, () -> {
				entity.getPersistentData().putDouble("pX", (entity.getPersistentData().getDouble("pX") + entity.getPersistentData().getDouble("vX")));
				entity.getPersistentData().putDouble("pY", (entity.getPersistentData().getDouble("pY") + entity.getPersistentData().getDouble("vY")));
				entity.getPersistentData().putDouble("pZ", (entity.getPersistentData().getDouble("pZ") + entity.getPersistentData().getDouble("vZ")));
				entity.getPersistentData().putDouble("rep2", (entity.getPersistentData().getDouble("rep2") + (2 * Math.PI) / 60));
				entity.getPersistentData().putDouble("rep", (entity.getPersistentData().getDouble("rep2")));
				entity.getPersistentData().putDouble("height", 0);
				entity.getPersistentData().putDouble("distance", 0);
				for (int index1 = 0; index1 < 60; index1++) {
					entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.05));
					entity.getPersistentData().putDouble("height", (entity.getPersistentData().getDouble("height") + 0.15));
					entity.getPersistentData().putDouble("rep", (entity.getPersistentData().getDouble("rep") + Math.PI / 60));
					for (int index2 = 0; index2 < 3; index2++) {
						entity.getPersistentData().putDouble("rep", (entity.getPersistentData().getDouble("rep") + (2 * Math.PI) / 3));
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.CLOUD, (entity.getPersistentData().getDouble("pX") + Math.sin(entity.getPersistentData().getDouble("rep")) * entity.getPersistentData().getDouble("distance")),
									(entity.getPersistentData().getDouble("pY") + entity.getPersistentData().getDouble("height")),
									(entity.getPersistentData().getDouble("pZ") + Math.cos(entity.getPersistentData().getDouble("rep")) * entity.getPersistentData().getDouble("distance")), 1, 0, 0, 0, 0);
						{
							final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("pX") + Math.sin(entity.getPersistentData().getDouble("rep")) * entity.getPersistentData().getDouble("distance")),
									(entity.getPersistentData().getDouble("pY") + entity.getPersistentData().getDouble("height")),
									(entity.getPersistentData().getDouble("pZ") + Math.cos(entity.getPersistentData().getDouble("rep")) * entity.getPersistentData().getDouble("distance")));
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
							for (Entity entityiterator : _entfound) {
								if ((!((entity instanceof LivingEntity _teamEnt && _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt.getStringUUID()) != null
										? _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt instanceof Player _pl ? _pl.getGameProfile().getName() : _teamEnt.getStringUUID()).getName()
										: "")
										.equals(entityiterator instanceof LivingEntity _teamEnt && _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt.getStringUUID()) != null
												? _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt instanceof Player _pl ? _pl.getGameProfile().getName() : _teamEnt.getStringUUID()).getName()
												: ""))
										|| (entity instanceof LivingEntity _teamEnt && _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt.getStringUUID()) != null
												? _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt instanceof Player _pl ? _pl.getGameProfile().getName() : _teamEnt.getStringUUID()).getName()
												: "").equals(""))
										&& !(entity == entityiterator || entityiterator instanceof ExperienceOrb || entityiterator instanceof ItemEntity)) {
									entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:mage"))), entity),
											4);
									if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
										_entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1, false, false));
								}
							}
						}
					}
				}
			});
		}
	}
}
