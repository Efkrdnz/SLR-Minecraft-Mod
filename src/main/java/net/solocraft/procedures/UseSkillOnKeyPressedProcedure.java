package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.ShadowStepEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.ClassPassiveManager;
import net.solocraft.util.JobSkillManager;
import net.solocraft.util.MageQTEHelper;
import net.solocraft.util.MageQTEState;
import net.solocraft.util.BarrierMageSpellManager;
import net.solocraft.util.ArcaneMageSpellManager;
import net.solocraft.util.FireMageSpellManager;
import net.solocraft.util.QTEResult;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.UrgentQuestManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.BlockPos;

public class UseSkillOnKeyPressedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		// ── Mage QTE: intercept and start ring animation ──────────────────────
		// Mage spells are NOT cast on key press. Instead the client shows the
		// rotating needle overlay; the spell fires when the key is released
		// (UseSkillOnKeyReleasedProcedure) with a mana discount based on timing.
		String _selectedPower = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower;
		if (MageQTEHelper.MAGE_SKILLS.contains(_selectedPower)
				&& CooldownManager.isOnCooldown(entity, _selectedPower)) {
			if (world instanceof Level level) {
				if (level.isClientSide()) {
					DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MageQTEState.INSTANCE.endQTE());
				} else {
					entity.getPersistentData().putBoolean("mage_casting", false);
					entity.getPersistentData().remove("mage_qte_zone_start");
					if (entity instanceof Player player)
						player.displayClientMessage(Component.literal("Ability on cooldown! "
								+ CooldownManager.getRemainingSeconds(entity, _selectedPower) + "s"), true);
				}
			}
			return;
		}
		UrgentQuestManager.onSkillUsed(entity, _selectedPower);
		if (ShadowMonarchManager.isFormationSkill(_selectedPower)) {
			ShadowFormationCastProcedure.execute(world, x, y, z, entity, _selectedPower);
			return;
		}
		if (JobSkillManager.cast(world, x, y, z, entity, _selectedPower)) {
			return;
		}
		if (FireMageSpellManager.FLAME_WEAVING.equals(_selectedPower)) {
			if (world instanceof Level level && !level.isClientSide())
				FireMageSpellManager.cast(entity, _selectedPower, QTEResult.MISS);
			return;
		}
		if (BarrierMageSpellManager.isInstantSkill(_selectedPower)) {
			if (world instanceof Level level && !level.isClientSide())
				BarrierMageSpellManager.cast(entity, _selectedPower, QTEResult.MISS);
			return;
		}
		if (ArcaneMageSpellManager.isInstantSkill(_selectedPower)) {
			if (world instanceof Level level && !level.isClientSide())
				ArcaneMageSpellManager.cast(entity, _selectedPower, QTEResult.MISS);
			return;
		}
		if (MageQTEHelper.MAGE_SKILLS.contains(_selectedPower)) {
			float qteZoneStart = MageQTEHelper.computeZoneStart(entity);
			if (world instanceof Level _lvl) {
				if (_lvl.isClientSide()) {
					DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
							MageQTEState.INSTANCE.startQTE(qteZoneStart));
				} else {
					entity.getPersistentData().putBoolean("mage_casting", true);
					entity.getPersistentData().putFloat("mage_qte_zone_start", qteZoneStart);
				}
			}
			return;
		}
		// ─────────────────────────────────────────────────────────────────────

		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Stealth")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 600) {
				if (!CooldownManager.isOnCooldown(entity, "Stealth")) {
					StealthProcedure.execute(world, x, y, z, entity);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 600;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A71Using Stealth"), true);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Murderious Intent")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 600) {
				if (!CooldownManager.isOnCooldown(entity, "Murderious Intent")) {
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 600;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					BloodLustProcedure.execute(world, x, y, z, entity);
					CooldownManager.set(entity, "mana_refresh", 60);
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A71Using Murderious Intent"), true);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Backstab")) {
			PhantomLeapAttackProcedure.execute(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Shadowstep")) {
			castShadowstep(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Dualwield")) {
			DualWieldProcedure.execute(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Quickslashes")) {
			if (!CooldownManager.isOnCooldown(entity, "Quickslashes")) {
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP > 1000) {
					QuickSlashesProcedure.execute(world, x, y, z, entity);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 1000;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A71Using Quick Slashes"), true);
				}
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Lightball")) {
			LightBallThrowProcedure.execute(entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Light Golem")) {
			SummonBeastProcedure.execute(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Detection")) {
			DetectEyeSpawnProcedure.execute(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Slash Dash")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 600) {
				if (!CooldownManager.isOnCooldown(entity, "Slash Dash")) {
					if (!(entity instanceof LivingEntity _livEnt46 && _livEnt46.hasEffect(SololevelingModMobEffects.SWORD_DANCE.get()))
							&& !(entity instanceof LivingEntity _livEnt47 && _livEnt47.hasEffect(SololevelingModMobEffects.SWORD_OF_LIGHT.get()))) {
						{
							double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 600;
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.MP = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						HeavyImpactProcedure.execute(world, entity);
						CooldownManager.set(entity, "mana_refresh", 60);
						if (entity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("\u00A7l\u00A77Using Slash Dash"), true);
					} else {
						if (entity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("\u00A76Can't Use skill while Sword Dance is active!"), true);
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, 1);
							} else {
								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, 1, false);
							}
						}
					}
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (_selectedPower.equals("Cross Strike") || _selectedPower.equals("Critical Strike")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 1000) {
				if (!CooldownManager.isOnCooldown(entity, "Cross Strike") && !CooldownManager.isOnCooldown(entity, "Critical Strike")) {
					if (!(entity instanceof LivingEntity _livEnt54 && _livEnt54.hasEffect(SololevelingModMobEffects.SWORD_DANCE.get()))
							&& !(entity instanceof LivingEntity _livEnt55 && _livEnt55.hasEffect(SololevelingModMobEffects.SWORD_OF_LIGHT.get()))) {
						{
							double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 1000;
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.MP = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						CrossStrikeProcedure.execute(world, x, y, z, entity);
						if (entity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("\u00A7l\u00A77Using Cross Strike"), true);
					} else {
						if (entity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("\u00A76Can't Use skill while Sword Dance is active!"), true);
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, 1);
							} else {
								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, 1, false);
							}
						}
					}
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Sword of Light")) {
			SwordOfLightGiveProcedure.execute(world, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Ground Slam")) {
			if (!(entity instanceof LivingEntity _livEnt60 && _livEnt60.hasEffect(SololevelingModMobEffects.SWORD_DANCE.get())) && !(entity instanceof LivingEntity _livEnt61 && _livEnt61.hasEffect(SololevelingModMobEffects.SWORD_OF_LIGHT.get()))) {
				UpforceSlashProcedure.execute(world, x, y, z, entity);
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("\u00A76Can't Use skill while Sword Dance is active!"), true);
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Sword Dance")) {
			SwordDanceGiveProcedure.execute(entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Slash Fury")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 300) {
				if (!CooldownManager.isOnCooldown(entity, "Slash Fury")) {
					if (!(entity instanceof LivingEntity _livEnt65 && _livEnt65.hasEffect(SololevelingModMobEffects.SWORD_DANCE.get()))
							&& !(entity instanceof LivingEntity _livEnt66 && _livEnt66.hasEffect(SololevelingModMobEffects.SWORD_OF_LIGHT.get()))) {
						SlashFurryTestRightclickedProcedure.execute(entity);
						{
							double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.MP = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						CooldownManager.set(entity, "mana_refresh", 60);
						if (entity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("\u00A7l\u00A77Using Slash Fury"), true);
					} else {
						if (entity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("\u00A76Can't Use skill while Sword Dance is active!"), true);
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, 1);
							} else {
								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, 1, false);
							}
						}
					}
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Heal Beam")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 600) {
				if (!CooldownManager.isOnCooldown(entity, "Heal Beam")) {
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 600;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					HealingBeamProcedure.execute(world, entity);
					if (world instanceof Level _lvl && !_lvl.isClientSide() && entity instanceof ServerPlayer _sp)
						ClassPassiveManager.onHealerCast(_sp);
					CooldownManager.set(entity, "mana_refresh", 60);
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A7a\u00A7lUsing Healing"), true);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Blessing Mark")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 1500) {
				if (!CooldownManager.isOnCooldown(entity, "Blessing Mark")) {
					BellOfHealingSummonProcedure.execute(world, x, y, z, entity);
					if (world instanceof Level _lvl && !_lvl.isClientSide() && entity instanceof ServerPlayer _sp)
						ClassPassiveManager.onHealerCast(_sp);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 1500;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					CooldownManager.set(entity, "mana_refresh", 60);
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A7a\u00A7lUsing Bell Of Healing"), true);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Purification")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 200) {
				PuricficationCastProcedure.execute(world, x, y, z, entity);
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 200;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MP = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("\u00A71Using Purification"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Physical Buff")) {
			PhysicalBuffCastProcedure.execute(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Haste Buff")) {
			HasteBuffCastProcedure.execute(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Overheal")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 600) {
				if (!CooldownManager.isOnCooldown(entity, "Overheal")) {
					OverhealProcedure.execute(world, x, y, z, entity);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 600;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					CooldownManager.set(entity, "mana_refresh", 60);
					CooldownManager.set(entity, "Overheal", 300);
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A7a\u00A7lUsing Over Effect"), true);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Tank Leap")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 300) {
				if (!CooldownManager.isOnCooldown(entity, "Tank Leap")) {
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 300;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					TankLeapProcedure.execute(world, x, y, z, entity);
					CooldownManager.set(entity, "mana_refresh", 60);
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A76Using Leap Strike"), true);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Reinforcement")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 500) {
				if (!CooldownManager.isOnCooldown(entity, "Reinforcement")) {
					TankInvincibilityAvtivateProcedure.execute(entity);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					CooldownManager.set(entity, "mana_refresh", 60);
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A76Using Reinforcement"), true);
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_RED.get()), x, y, z, 20, 0.5, 1, 0.5, 1);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Protection Mark")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 1500) {
				if (!CooldownManager.isOnCooldown(entity, "Protection Mark")) {
					FlagOfProtectionSummonProcedure.execute(world, x, y, z, entity);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 1500;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("\u00A76Using Protection Mark"), true);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Shield Bash")) {
			ShieldBashProcedure.execute(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Willpower")) {
			WillPowerGiveProcedure.execute(entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Taunt")) {
			TauntCastProcedure.execute(world, x, y, z, entity);
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Sharpshooter")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 600) {
				if (!CooldownManager.isOnCooldown(entity, "Sharpshooter")) {
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 600;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					HomingArrowProcedure.execute(entity);
					// Ranger Focus bonus: consume 100% charge for Strength II burst
					if (world instanceof Level _lvl && !_lvl.isClientSide() && entity instanceof ServerPlayer _sp)
						ClassPassiveManager.consumeRangerFocus(_sp);
					CooldownManager.set(entity, "mana_refresh", 20);
					CooldownManager.set(entity, "Sharpshooter", 120);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Proximity Trap")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 600) {
				if (!CooldownManager.isOnCooldown(entity, "Proximity Trap")) {
					DeployTrapProcedure.execute(world, x, y, z, entity);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 600;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					CooldownManager.set(entity, "mana_refresh", 40);
					CooldownManager.set(entity, "Proximity Trap", 400);
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Back Step")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 200) {
				if (BackStepProcedure.execute(entity)) {
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 200;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MP = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("High Value Target")) {
			if (!CooldownManager.isOnCooldown(entity, "High Value Target")) {
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 1000) {
					ArrowRainSprayProcedure.execute(world, x, y, z, entity);
					// Ranger Focus bonus: consume 100% charge for Strength II burst
					if (world instanceof Level _lvl && !_lvl.isClientSide() && entity instanceof ServerPlayer _sp)
						ClassPassiveManager.consumeRangerFocus(_sp);
					CooldownManager.set(entity, "High Value Target", 120);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 1000;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				} else {
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
				}
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Hawkeye")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 500) {
				HawkEyeProcedure.execute(entity);
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 500;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MP = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Hyper Focus")) {
			if (!CooldownManager.isOnCooldown(entity, "Hyper Focus")) {
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 600) {
					IntenseFocusProcedure.execute(world, x, y, z, entity);
					CooldownManager.set(entity, "Hyper Focus", 200);
					{
						double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 600;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.MP = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				} else {
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("You dont have enough MP"), true);
				}
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Critical Attack")) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).is(ItemTags.create(new ResourceLocation("dagger")))
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).is(ItemTags.create(new ResourceLocation("dagger")))) {
				entity.getPersistentData().putBoolean("Critical_Attack_Targetting", true);
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("This is a \"Dagger\" specific skill."), true);
				if (world instanceof Level _level) {
					if (_level.isClientSide()) {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, (float) 0.6, false);
					}
				}
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Mutilation")) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).is(ItemTags.create(new ResourceLocation("dagger")))
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).is(ItemTags.create(new ResourceLocation("dagger")))) {
				entity.getPersistentData().putBoolean("Mutilation_Targetting", true);
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("This is a \"Dagger\" specific skill."), true);
				if (world instanceof Level _level) {
					if (_level.isClientSide()) {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, (float) 0.6, false);
					}
				}
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Sword Beam")) {
			SwordBeamAttackProcedure.execute(world, x, y, z, entity);
		}
	}

	private static void castShadowstep(LevelAccessor world, double x, double y, double z, Entity entity) {
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (vars.MP < 100) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("Not enough MP!"), true);
			return;
		}
		if (CooldownManager.isOnCooldown(entity, "Shadowstep"))
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP -= 100;
			capability.progression_assassin += 1;
			capability.syncPlayerVariables(entity);
		});
		if (world instanceof ServerLevel level) {
			Entity afterImage = SololevelingModEntities.AFTER_IMAGE.get().spawn(level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
			if (afterImage != null)
				afterImage.setYRot(world.getRandom().nextFloat() * 360.0F);
		}
		if (world instanceof Level level) {
			if (!level.isClientSide()) {
				level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.wither.shoot")), SoundSource.NEUTRAL, 1, 1.5F);
			} else {
				level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.wither.shoot")), SoundSource.NEUTRAL, 1, 1.5F, false);
			}
		}
		if (entity instanceof LivingEntity living) {
			if (!living.level().isClientSide()) {
				living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 1, false, false));
				living.addEffect(new MobEffectInstance(SololevelingModMobEffects.NO_FALL_DAMAGE.get(), 9999, 1, false, false));
			}
			if (!living.level().isClientSide()) {
				ShadowStepEntity projectile = ShadowStepEntity.shoot(living.level(), living, living.getRandom(), 1.5F,
						1 + vars.Intelligence / 45, 0);
				projectile.setPos(living.getX(), living.getEyeY() - 0.1D, living.getZ());
			}
		}
		CooldownManager.set(entity, "mana_refresh", 40);
		CooldownManager.set(entity, "Shadowstep", 40);
	}
}
