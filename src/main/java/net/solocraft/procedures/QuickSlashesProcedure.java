package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.entity.QuickSlashesEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;

public class QuickSlashesProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double delay = 0;
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
		CooldownManager.set(entity, "mana_refresh", 150);
		CooldownManager.set(entity, "Quickslashes", 300);
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(12 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (!(entity == entityiterator) && !(entityiterator instanceof TamableAnimal _tamIsTamedBy && entity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false) && entityiterator instanceof LivingEntity
						&& (!((entity instanceof LivingEntity _teamEnt && _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt.getStringUUID()) != null
								? _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt instanceof Player _pl ? _pl.getGameProfile().getName() : _teamEnt.getStringUUID()).getName()
								: "")
								.equals(entityiterator instanceof LivingEntity _teamEnt && _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt.getStringUUID()) != null
										? _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt instanceof Player _pl ? _pl.getGameProfile().getName() : _teamEnt.getStringUUID()).getName()
										: ""))
								|| (entity instanceof LivingEntity _teamEnt && _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt.getStringUUID()) != null
										? _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt instanceof Player _pl ? _pl.getGameProfile().getName() : _teamEnt.getStringUUID()).getName()
										: "").equals(""))) {
					if (entity instanceof LivingEntity livingEntity)
						QuickSlashesEntity.spawn(world, livingEntity, entityiterator);
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), 6, 0.5, 1, 0.5, 0);
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.CLEAVE.get()), (entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), 6, 0.5, 1, 0.5, 0);
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.DISMANTLE.get()), (entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), 6, 0.5, 1, 0.5, 0);
					entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:assassin"))), entity),
							(float) (6 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength / 20));
					if (entity instanceof LivingEntity _entity)
						_entity.swing(InteractionHand.MAIN_HAND, true);
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.3, 1);
						} else {
							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.3, 1, false);
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL, (float) 0.5, 1);
						} else {
							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL, (float) 0.5, 1, false);
						}
					}
				}
			}
		}
	}
}
