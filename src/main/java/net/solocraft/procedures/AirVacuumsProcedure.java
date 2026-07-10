package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;

public class AirVacuumsProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double delay = 0;
		double dmgmultiplier = 0;
		if (!CooldownManager.isOnCooldown(entity, "Curse Sphere")) {
			CooldownManager.set(entity, "Curse Sphere", 100);
			entity.getPersistentData().putDouble("range", 100);
			entity.getPersistentData().putDouble("rotation", 0);
			entity.getPersistentData().putDouble("sx", (entity.getX()));
			entity.getPersistentData().putDouble("sy", (entity.getY() + 1));
			entity.getPersistentData().putDouble("sz", (entity.getZ()));
			entity.getPersistentData().putDouble("tx",
					(entity.level().clip(new ClipContext(entity.getEyePosition(1f), entity.getEyePosition(1f).add(entity.getViewVector(1f).scale(30)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity)).getBlockPos().getX()));
			entity.getPersistentData().putDouble("ty",
					(entity.level().clip(new ClipContext(entity.getEyePosition(1f), entity.getEyePosition(1f).add(entity.getViewVector(1f).scale(30)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity)).getBlockPos().getY()));
			entity.getPersistentData().putDouble("tz",
					(entity.level().clip(new ClipContext(entity.getEyePosition(1f), entity.getEyePosition(1f).add(entity.getViewVector(1f).scale(30)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity)).getBlockPos().getZ()));
			entity.getPersistentData().putDouble("range", Math.sqrt(Math.pow(entity.getPersistentData().getDouble("sx") - entity.getPersistentData().getDouble("tx"), 2)
					+ Math.pow(entity.getPersistentData().getDouble("sy") - entity.getPersistentData().getDouble("ty"), 2) + Math.pow(entity.getPersistentData().getDouble("sz") - entity.getPersistentData().getDouble("tz"), 2)));
			entity.getPersistentData().putDouble("x+", ((entity.getPersistentData().getDouble("sx") - entity.getPersistentData().getDouble("tx")) / entity.getPersistentData().getDouble("range")));
			entity.getPersistentData().putDouble("y+", ((entity.getPersistentData().getDouble("sy") - entity.getPersistentData().getDouble("ty")) / entity.getPersistentData().getDouble("range")));
			entity.getPersistentData().putDouble("z+", ((entity.getPersistentData().getDouble("sz") - entity.getPersistentData().getDouble("tz")) / entity.getPersistentData().getDouble("range")));
			entity.getPersistentData().putDouble("size", 0);
			for (int index0 = 0; index0 < (int) (entity.getPersistentData().getDouble("range") * 5); index0++) {
				delay = 5;
				SololevelingMod.queueServerWork((int) delay, () -> {
					entity.getPersistentData().putDouble("rotation", (entity.getPersistentData().getDouble("rotation") + 0.5));
					entity.getPersistentData().putDouble("sx", (entity.getPersistentData().getDouble("sx") + entity.getPersistentData().getDouble("x+") * (-0.2)));
					entity.getPersistentData().putDouble("sy", (entity.getPersistentData().getDouble("sy") + entity.getPersistentData().getDouble("y+") * (-0.2)));
					entity.getPersistentData().putDouble("sz", (entity.getPersistentData().getDouble("sz") + entity.getPersistentData().getDouble("z+") * (-0.2)));
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SMOKE, (entity.getPersistentData().getDouble("sx") + Math.sin(entity.getPersistentData().getDouble("rotation")) * (entity.getDirection()).getStepZ()),
								(entity.getPersistentData().getDouble("sy") + Math.cos(entity.getPersistentData().getDouble("rotation"))),
								(entity.getPersistentData().getDouble("sz") + Math.sin(entity.getPersistentData().getDouble("rotation")) * (entity.getDirection()).getStepX()), 15, 0.1, 0.1, 0.1, 0);
					{
						final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("sx")), (entity.getPersistentData().getDouble("sy")), (entity.getPersistentData().getDouble("sz")));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
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
								if (entity instanceof Player) {
									if ((entity instanceof LivingEntity _entity) ? _entity.isHolding(SololevelingModItems.STORM_GRIAMORE.get()) : false) {
										entityiterator.hurt(
												new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:mage"))), entity),
												(float) (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Intelligence / 20 + 4) * 1.5));
									} else {
										entityiterator.hurt(
												new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:mage"))), entity),
												(float) ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Intelligence / 20 + 4));
									}
								} else if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("hunters")))) {
									entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:mage"))), entity),
											(float) (entity.getPersistentData().getDouble("int") / 3 + 6));
								}
							}
						}
					}
				});
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("ability on cooldown!"), false);
		}
	}
}
