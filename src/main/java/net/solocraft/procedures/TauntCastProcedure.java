package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;

public class TauntCastProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double chain = 0;
		double chainwait = 0;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 100) {
			if (!CooldownManager.isOnCooldown(entity, "Taunt")) {
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 100;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MP = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).progression_tanker + 1;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_tanker = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				CooldownManager.set(entity, "Taunt", 200);
				CooldownManager.set(entity, "mana_refresh", 40);
				chain = 10;
				for (int index0 = 0; index0 < (int) chain; index0++) {
					SololevelingMod.queueServerWork((int) chainwait, () -> {
						if (world instanceof ServerLevel _level)
							_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_RED.get()), x, y, z, 5, 3, 3, 3, 1);
						{
							final Vec3 _center = new Vec3(x, y, z);
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
							for (Entity entityiterator : _entfound) {
								if (entityiterator instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
									if (!((entity instanceof LivingEntity _teamEnt && _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt.getStringUUID()) != null
											? _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt instanceof Player _pl ? _pl.getGameProfile().getName() : _teamEnt.getStringUUID()).getName()
											: "")
											.equals((entityiterator instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _teamEnt
													&& _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt.getStringUUID()) != null
															? _teamEnt.level().getScoreboard().getPlayersTeam(_teamEnt instanceof Player _pl ? _pl.getGameProfile().getName() : _teamEnt.getStringUUID()).getName()
															: ""))) {
										if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent)
											_entity.setTarget(_ent);
									}
								} else {
									if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent)
										_entity.setTarget(_ent);
								}
							}
						}
					});
					chainwait = chainwait + 2;
				}
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("Not enough MP!"), true);
		}
	}
}
