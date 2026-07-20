package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandFunction;

import java.util.Optional;
import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.OrbOfAvariceManager;

public class SummonBeastProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		int manaCost = OrbOfAvariceManager.adjustManaCost(entity, 2500);
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= manaCost) {
			if (!CooldownManager.isOnCooldown(entity, "Light Golem")) {
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - manaCost;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MP = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				CooldownManager.set(entity, "mana_refresh", 200);
				CooldownManager.set(entity, "Light Golem", 3600);
				if (world.getLevelData().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK) == true) {
					world.getLevelData().getGameRules().getRule(GameRules.RULE_SENDCOMMANDFEEDBACK).set(false, world.getServer());
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("mod:circle"));
							if (_fopt.isPresent())
								_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
						}
					}
					world.getLevelData().getGameRules().getRule(GameRules.RULE_SENDCOMMANDFEEDBACK).set(true, world.getServer());
				} else {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("mod:circle"));
							if (_fopt.isPresent())
								_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
						}
					}
				}
				if ((entity.getDirection()) == Direction.NORTH) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = SololevelingModEntities.ELDER_BEAST.get().spawn(_level, BlockPos.containing(x, y, z + 3), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setYRot(entity.getYRot());
							entityToSpawn.setYBodyRot(entity.getYRot());
							entityToSpawn.setYHeadRot(entity.getYRot());
							entityToSpawn.setDeltaMovement(0, 0, 0);
						}
					}
				} else if ((entity.getDirection()) == Direction.SOUTH) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = SololevelingModEntities.ELDER_BEAST.get().spawn(_level, BlockPos.containing(x, y, z - 3), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setYRot(entity.getYRot());
							entityToSpawn.setYBodyRot(entity.getYRot());
							entityToSpawn.setYHeadRot(entity.getYRot());
							entityToSpawn.setDeltaMovement(0, 0, 0);
						}
					}
				} else if ((entity.getDirection()) == Direction.EAST) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = SololevelingModEntities.ELDER_BEAST.get().spawn(_level, BlockPos.containing(x - 3, y, z), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setYRot(entity.getYRot());
							entityToSpawn.setYBodyRot(entity.getYRot());
							entityToSpawn.setYHeadRot(entity.getYRot());
							entityToSpawn.setDeltaMovement(0, 0, 0);
						}
					}
				} else if ((entity.getDirection()) == Direction.WEST) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = SololevelingModEntities.ELDER_BEAST.get().spawn(_level, BlockPos.containing(x + 3, y, z), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setYRot(entity.getYRot());
							entityToSpawn.setYBodyRot(entity.getYRot());
							entityToSpawn.setYHeadRot(entity.getYRot());
							entityToSpawn.setDeltaMovement(0, 0, 0);
						}
					}
				}
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if ((entityiterator.getDisplayName().getString()).equals("Elder Beast")) {
							if (entityiterator instanceof TamableAnimal _toTame && entity instanceof Player _owner)
								_toTame.tame(_owner);
						}
					}
				}
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:bellirng")), SoundSource.NEUTRAL, (float) 0.1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:bellirng")), SoundSource.NEUTRAL, (float) 0.1, 1, false);
					}
				}
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("Not enough MP!"), true);
		}
	}
}
