package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class ARISEProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double rand = 0;
		if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1) {
			if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 500) {
				if (!(sourceentity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.ARISE_COOLDOWN.get()))) {
					if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage < (sourceentity
							.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorage) {
						world.addParticle((SimpleParticleType) (SololevelingModParticleTypes.SHADOW_REVIVE.get()), x, (y + 2), z, 0, 0, 0);
						if ((entity.getPersistentData().getString("soultype")).equals("soldier")) {
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ordshadowmax + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.ordshadowmax = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
							if (sourceentity instanceof Player _player && !_player.level().isClientSide())
								_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
							if (!entity.level().isClientSide())
								entity.discard();
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.shadowstorageusage = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).OrdShadow + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.OrdShadow = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.MP = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (world instanceof ServerLevel _level) {
								LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
								entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
								entityToSpawn.setVisualOnly(true);
								_level.addFreshEntity(entityToSpawn);
							}
							if (world instanceof ServerLevel _level) {
								Entity _entityToSpawn = SololevelingModEntities.SHADOW_SOLD_1.get().create(_level);
								_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
								if (_entityToSpawn instanceof Mob _mobToSpawn) {
									_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
								}
								if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
									_toTame.tame(_owner);
								_level.addFreshEntity(_entityToSpawn);
							}
						} else if ((entity.getPersistentData().getString("soultype")).equals("goblin")) {
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.shadowstorageusage = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).GobShadowMax + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.GobShadowMax = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (sourceentity instanceof Player _player && !_player.level().isClientSide())
								_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
							if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
							if (!entity.level().isClientSide())
								entity.discard();
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).GobShadow + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.GobShadow = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.MP = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (world instanceof ServerLevel _level) {
								LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
								entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
								entityToSpawn.setVisualOnly(true);
								_level.addFreshEntity(entityToSpawn);
							}
							if (world instanceof ServerLevel _level) {
								Entity _entityToSpawn = SololevelingModEntities.GOBLIN_CLUB_SHADOW.get().create(_level);
								_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
								if (_entityToSpawn instanceof Mob _mobToSpawn) {
									_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
								}
								if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
									_toTame.tame(_owner);
								_level.addFreshEntity(_entityToSpawn);
							}
						} else if ((entity.getPersistentData().getString("soultype")).equals("goblinarc")) {
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.shadowstorageusage = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ShadowGoblinArcherMax + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.ShadowGoblinArcherMax = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (sourceentity instanceof Player _player && !_player.level().isClientSide())
								_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
							if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
							if (!entity.level().isClientSide())
								entity.discard();
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).GobShadow + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.GobShadow = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).summonlimitusage + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.summonlimitusage = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.MP = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (world instanceof ServerLevel _level) {
								LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
								entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
								entityToSpawn.setVisualOnly(true);
								_level.addFreshEntity(entityToSpawn);
							}
							if (world instanceof ServerLevel _level) {
								Entity _entityToSpawn = SololevelingModEntities.GOBLIN_ARCHER_SHADOW.get().create(_level);
								_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
								if (_entityToSpawn instanceof Mob _mobToSpawn) {
									_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
								}
								if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
									_toTame.tame(_owner);
								_level.addFreshEntity(_entityToSpawn);
							}
						} else if ((entity.getPersistentData().getString("soultype")).equals("goblinmage")) {
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.shadowstorageusage = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ShadowGoblinMageMax + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.ShadowGoblinMageMax = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (sourceentity instanceof Player _player && !_player.level().isClientSide())
								_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
							if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
							if (!entity.level().isClientSide())
								entity.discard();
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ShadowGoblinMageAmount + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.ShadowGoblinMageAmount = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.MP = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (world instanceof ServerLevel _level) {
								LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
								entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
								entityToSpawn.setVisualOnly(true);
								_level.addFreshEntity(entityToSpawn);
							}
							if (world instanceof ServerLevel _level) {
								Entity _entityToSpawn = SololevelingModEntities.GOBLIN_MAGE_SHADOW.get().create(_level);
								_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
								if (_entityToSpawn instanceof Mob _mobToSpawn) {
									_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
								}
								if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
									_toTame.tame(_owner);
								_level.addFreshEntity(_entityToSpawn);
							}
						} else if ((entity.getPersistentData().getString("soultype")).equals("wolf")) {
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.shadowstorageusage = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).WolfShadowMax + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.WolfShadowMax = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (sourceentity instanceof Player _player && !_player.level().isClientSide())
								_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
							if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
							if (!entity.level().isClientSide())
								entity.discard();
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).WolfShadow + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.WolfShadow = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).summonlimitusage + 1;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.summonlimitusage = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							{
								double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
								sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.MP = _setval;
									capability.syncPlayerVariables(sourceentity);
								});
							}
							if (world instanceof ServerLevel _level) {
								LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
								entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
								entityToSpawn.setVisualOnly(true);
								_level.addFreshEntity(entityToSpawn);
							}
							if (world instanceof ServerLevel _level) {
								Entity _entityToSpawn = SololevelingModEntities.STEEL_FANG_WOLF_SHADOW.get().create(_level);
								_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
								if (_entityToSpawn instanceof Mob _mobToSpawn) {
									_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
								}
								if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
									_toTame.tame(_owner);
								_level.addFreshEntity(_entityToSpawn);
							}
						} else if ((entity.getPersistentData().getString("soultype")).equals("orc")) {
							if (Math.random() < ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level) / ((float) 40)) {
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.shadowstorageusage = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).orcmax + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.orcmax = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
								if (sourceentity instanceof Player _player && !_player.level().isClientSide())
									_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.MP = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (!entity.level().isClientSide())
									entity.discard();
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).orcspawned + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.orcspawned = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).summonlimitusage + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.summonlimitusage = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (world instanceof ServerLevel _level) {
									LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
									entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
									entityToSpawn.setVisualOnly(true);
									_level.addFreshEntity(entityToSpawn);
								}
								if (world instanceof ServerLevel _level) {
									Entity _entityToSpawn = SololevelingModEntities.SHADOW_GREEN_ORC.get().create(_level);
									_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
									if (_entityToSpawn instanceof Mob _mobToSpawn) {
										_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
									}
									if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
										_toTame.tame(_owner);
									_level.addFreshEntity(_entityToSpawn);
								}
							} else {
								{
									Entity _ent = sourceentity;
									if (!_ent.level().isClientSide() && _ent.getServer() != null) {
										_ent.getServer().getCommands().performPrefixedCommand(
												new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
														_ent.getDisplayName(), _ent.level().getServer(), _ent),
												"/title @p title {\"text\":\"Failed\",\"color\":\"red\",\"bold\":true,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
									}
								}
								if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
								entity.getPersistentData().putDouble("ariset", (entity.getPersistentData().getDouble("ariset") + 1));
							}
						} else if ((entity.getPersistentData().getString("soultype")).equals("bear")) {
							if (Math.random() < ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level) / ((float) 40)) {
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.shadowstorageusage = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).polarbearmax + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.polarbearmax = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
								if (sourceentity instanceof Player _player && !_player.level().isClientSide())
									_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
								if (!entity.level().isClientSide())
									entity.discard();
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.MP = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).polarbear + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.polarbear = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).summonlimitusage + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.summonlimitusage = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (world instanceof ServerLevel _level) {
									LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
									entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
									entityToSpawn.setVisualOnly(true);
									_level.addFreshEntity(entityToSpawn);
								}
								if (world instanceof ServerLevel _level) {
									Entity _entityToSpawn = SololevelingModEntities.SHADOW_POLAR_BEAR.get().create(_level);
									_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
									if (_entityToSpawn instanceof Mob _mobToSpawn) {
										_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
									}
									if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
										_toTame.tame(_owner);
									_level.addFreshEntity(_entityToSpawn);
								}
							} else {
								{
									Entity _ent = sourceentity;
									if (!_ent.level().isClientSide() && _ent.getServer() != null) {
										_ent.getServer().getCommands().performPrefixedCommand(
												new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
														_ent.getDisplayName(), _ent.level().getServer(), _ent),
												"/title @p title {\"text\":\"Failed\",\"color\":\"red\",\"bold\":true,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
									}
								}
								if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
								entity.getPersistentData().putDouble("ariset", (entity.getPersistentData().getDouble("ariset") + 1));
							}
						} else if ((entity.getPersistentData().getString("soultype")).equals("highorc")) {
							if (Math.random() < ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level) / ((float) 50)) {
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).shadowstorageusage + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.shadowstorageusage = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).highorcmax + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.highorcmax = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
								if (sourceentity instanceof Player _player && !_player.level().isClientSide())
									_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.MP = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (!entity.level().isClientSide())
									entity.discard();
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).highorcspawned + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.highorcspawned = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).summonlimitusage + 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.summonlimitusage = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (world instanceof ServerLevel _level) {
									LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
									entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
									entityToSpawn.setVisualOnly(true);
									_level.addFreshEntity(entityToSpawn);
								}
								if (world instanceof ServerLevel _level) {
									Entity _entityToSpawn = SololevelingModEntities.SHADOW_HIGH_ORC.get().create(_level);
									_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
									if (_entityToSpawn instanceof Mob _mobToSpawn) {
										_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
									}
									if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
										_toTame.tame(_owner);
									_level.addFreshEntity(_entityToSpawn);
								}
							} else {
								{
									Entity _ent = sourceentity;
									if (!_ent.level().isClientSide() && _ent.getServer() != null) {
										_ent.getServer().getCommands().performPrefixedCommand(
												new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
														_ent.getDisplayName(), _ent.level().getServer(), _ent),
												"/title @p title {\"text\":\"Failed\",\"color\":\"red\",\"bold\":true,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
									}
								}
								if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
								entity.getPersistentData().putDouble("ariset", (entity.getPersistentData().getDouble("ariset") + 1));
							}
						} else if ((entity.getPersistentData().getString("soultype")).equals("tusk")) {
							if (Math.random() < ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level) / ((float) 70)) {
								{
									double _setval = 1;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.tuskmax = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
								if (sourceentity instanceof Player _player && !_player.level().isClientSide())
									_player.displayClientMessage(Component.literal(("\u00A75" + "ARISE")), true);
								{
									double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
									sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.MP = _setval;
										capability.syncPlayerVariables(sourceentity);
									});
								}
								if (!entity.level().isClientSide())
									entity.discard();
								if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).tuskspawned == 0) {
									{
										double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).tuskspawned + 1;
										sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.tuskspawned = _setval;
											capability.syncPlayerVariables(sourceentity);
										});
									}
									if (world instanceof ServerLevel _level) {
										LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
										entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y - 1, z)));
										entityToSpawn.setVisualOnly(true);
										_level.addFreshEntity(entityToSpawn);
									}
									if (world instanceof ServerLevel _level) {
										Entity _entityToSpawn = SololevelingModEntities.TUSK_SHADOW.get().create(_level);
										_entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
										if (_entityToSpawn instanceof Mob _mobToSpawn) {
											_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
										}
										if ((_entityToSpawn) instanceof TamableAnimal _toTame && sourceentity instanceof Player _owner)
											_toTame.tame(_owner);
										_level.addFreshEntity(_entityToSpawn);
									}
								}
							} else {
								{
									Entity _ent = sourceentity;
									if (!_ent.level().isClientSide() && _ent.getServer() != null) {
										_ent.getServer().getCommands().performPrefixedCommand(
												new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
														_ent.getDisplayName(), _ent.level().getServer(), _ent),
												"/title @p title {\"text\":\"Failed\",\"color\":\"red\",\"bold\":true,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
									}
								}
								if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.ARISE_COOLDOWN.get(), 10, 1, false, false));
								entity.getPersistentData().putDouble("ariset", (entity.getPersistentData().getDouble("ariset") + 1));
							}
						}
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.wither.spawn")), SoundSource.NEUTRAL, 1, 1);
							} else {
								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.wither.spawn")), SoundSource.NEUTRAL, 1, 1, false);
							}
						}
					} else {
						if (sourceentity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("You need more shadow storage to store any more soldiers"), false);
					}
				}
			} else {
				if (sourceentity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("Not enough mana to revive this monster"), false);
			}
		}
	}
}
