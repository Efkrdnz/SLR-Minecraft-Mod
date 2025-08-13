package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class CooldownRemainingOnTickProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Stealth")) {
			if (entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(SololevelingModMobEffects.STEALTH_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.STEALTH_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.STEALTH_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Murderious Intent")) {
			if (entity instanceof LivingEntity _livEnt3 && _livEnt3.hasEffect(SololevelingModMobEffects.BLOODLUST_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.BLOODLUST_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.BLOODLUST_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Quickslashes")) {
			if (entity instanceof LivingEntity _livEnt5 && _livEnt5.hasEffect(SololevelingModMobEffects.QUICK_SLASHES_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.QUICK_SLASHES_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.QUICK_SLASHES_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Shadowstep")) {
			if (entity instanceof LivingEntity _livEnt7 && _livEnt7.hasEffect(SololevelingModMobEffects.SHADOW_STEP_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.SHADOW_STEP_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.SHADOW_STEP_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Dualwield")) {
			if (entity instanceof LivingEntity _livEnt9 && _livEnt9.hasEffect(SololevelingModMobEffects.DUAL_WIELDING_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.DUAL_WIELDING_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.DUAL_WIELDING_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Backstab")) {
			if (entity instanceof LivingEntity _livEnt11 && _livEnt11.hasEffect(SololevelingModMobEffects.BACKSTAB_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.BACKSTAB_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.BACKSTAB_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Fireball")) {
			if (entity instanceof LivingEntity _livEnt14 && _livEnt14.hasEffect(SololevelingModMobEffects.FIREBALL_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.FIREBALL_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.FIREBALL_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Flame Tornado")) {
			if (entity instanceof LivingEntity _livEnt16 && _livEnt16.hasEffect(SololevelingModMobEffects.FIRE_TORNADO_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.FIRE_TORNADO_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.FIRE_TORNADO_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Heavy Flame")) {
			if (entity instanceof LivingEntity _livEnt18 && _livEnt18.hasEffect(SololevelingModMobEffects.HEAVY_FLAME_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.HEAVY_FLAME_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.HEAVY_FLAME_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Flame Vortex")) {
			if (entity instanceof LivingEntity _livEnt20 && _livEnt20.hasEffect(SololevelingModMobEffects.FLAME_VORTEX_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.FLAME_VORTEX_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.FLAME_VORTEX_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Fire Rain")) {
			if (entity instanceof LivingEntity _livEnt22 && _livEnt22.hasEffect(SololevelingModMobEffects.FIRE_RAIN_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.FIRE_RAIN_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.FIRE_RAIN_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Curse Smoke")) {
			if (entity instanceof LivingEntity _livEnt25 && _livEnt25.hasEffect(SololevelingModMobEffects.CURSE_SMOKE_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.CURSE_SMOKE_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.CURSE_SMOKE_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Curse Chains")) {
			if (entity instanceof LivingEntity _livEnt27 && _livEnt27.hasEffect(SololevelingModMobEffects.CURSED_CHAINS_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.CURSED_CHAINS_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.CURSED_CHAINS_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Curse Sphere")) {
			if (entity instanceof LivingEntity _livEnt29 && _livEnt29.hasEffect(SololevelingModMobEffects.AIR_VACUUM_COOLDWON.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.AIR_VACUUM_COOLDWON.get()) ? _livEnt.getEffect(SololevelingModMobEffects.AIR_VACUUM_COOLDWON.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Lightball")) {
			if (entity instanceof LivingEntity _livEnt32 && _livEnt32.hasEffect(SololevelingModMobEffects.LIGHTBALL_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.LIGHTBALL_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.LIGHTBALL_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Light Golem")) {
			if (entity instanceof LivingEntity _livEnt35 && _livEnt35.hasEffect(SololevelingModMobEffects.ELDER_BEAST_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.ELDER_BEAST_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.ELDER_BEAST_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Water Slash")) {
			if (entity instanceof LivingEntity _livEnt38 && _livEnt38.hasEffect(SololevelingModMobEffects.WATER_BULLET_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.WATER_BULLET_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.WATER_BULLET_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Detection")) {
			if (entity instanceof LivingEntity _livEnt41 && _livEnt41.hasEffect(SololevelingModMobEffects.DETECTION_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.DETECTION_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.DETECTION_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Slash Dash")) {
			if (entity instanceof LivingEntity _livEnt44 && _livEnt44.hasEffect(SololevelingModMobEffects.HEAVY_IMPACT_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.HEAVY_IMPACT_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.HEAVY_IMPACT_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Critical Strike")) {
			if (entity instanceof LivingEntity _livEnt46 && _livEnt46.hasEffect(SololevelingModMobEffects.CROSS_ATTACK_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.CROSS_ATTACK_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.CROSS_ATTACK_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Sword of Light")) {
			if (entity instanceof LivingEntity _livEnt48 && _livEnt48.hasEffect(SololevelingModMobEffects.SO_LCOOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.SO_LCOOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.SO_LCOOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Ground Slam")) {
			if (entity instanceof LivingEntity _livEnt50 && _livEnt50.hasEffect(SololevelingModMobEffects.UPFORCE_SLASH_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.UPFORCE_SLASH_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.UPFORCE_SLASH_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Sword Dance")) {
			if (entity instanceof LivingEntity _livEnt52 && _livEnt52.hasEffect(SololevelingModMobEffects.SWORD_DANCE_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.SWORD_DANCE_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.SWORD_DANCE_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Slash Fury")) {
			if (entity instanceof LivingEntity _livEnt54 && _livEnt54.hasEffect(SololevelingModMobEffects.IMPACT_RUSH_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.IMPACT_RUSH_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.IMPACT_RUSH_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Heal Beam")) {
			if (entity instanceof LivingEntity _livEnt57 && _livEnt57.hasEffect(SololevelingModMobEffects.HEALING_BEAM_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.HEALING_BEAM_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.HEALING_BEAM_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Blessing Mark")) {
			if (entity instanceof LivingEntity _livEnt59 && _livEnt59.hasEffect(SololevelingModMobEffects.BELL_OF_HEALING_COOLDOWN.get())) {
				return ""
						+ ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.BELL_OF_HEALING_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.BELL_OF_HEALING_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Purification")) {
			return "Ready!";
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Physical Buff")) {
			if (entity instanceof LivingEntity _livEnt61 && _livEnt61.hasEffect(SololevelingModMobEffects.PHYSICAL_BUFF_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.PHYSICAL_BUFF_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.PHYSICAL_BUFF_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Haste Buff")) {
			if (entity instanceof LivingEntity _livEnt63 && _livEnt63.hasEffect(SololevelingModMobEffects.HASTE_BUFF_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.HASTE_BUFF_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.HASTE_BUFF_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Overheal")) {
			if (entity instanceof LivingEntity _livEnt65 && _livEnt65.hasEffect(SololevelingModMobEffects.OVER_HEAL_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.OVER_HEAL_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.OVER_HEAL_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Tank Leap")) {
			if (entity instanceof LivingEntity _livEnt68 && _livEnt68.hasEffect(SololevelingModMobEffects.LEAP_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.LEAP_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.LEAP_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Reinforcement")) {
			if (entity instanceof LivingEntity _livEnt70 && _livEnt70.hasEffect(SololevelingModMobEffects.TANK_INV_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.TANK_INV_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.TANK_INV_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Protection Mark")) {
			if (entity instanceof LivingEntity _livEnt72 && _livEnt72.hasEffect(SololevelingModMobEffects.FRAG_OF_PROTECTION_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.FRAG_OF_PROTECTION_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.FRAG_OF_PROTECTION_COOLDOWN.get()).getDuration() : 0)
						/ 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Shield Bash")) {
			if (entity instanceof LivingEntity _livEnt74 && _livEnt74.hasEffect(SololevelingModMobEffects.SHIELD_BASH_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.SHIELD_BASH_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.SHIELD_BASH_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Willpower")) {
			if (entity instanceof LivingEntity _livEnt76 && _livEnt76.hasEffect(SololevelingModMobEffects.WILLPOWER_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.WILLPOWER_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.WILLPOWER_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Taunt")) {
			if (entity instanceof LivingEntity _livEnt78 && _livEnt78.hasEffect(SololevelingModMobEffects.TAUNT_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.TAUNT_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.TAUNT_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Sharpshooter")) {
			if (entity instanceof LivingEntity _livEnt81 && _livEnt81.hasEffect(SololevelingModMobEffects.HOMING_FLAME_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.HOMING_FLAME_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.HOMING_FLAME_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Proximity Trap")) {
			if (entity instanceof LivingEntity _livEnt83 && _livEnt83.hasEffect(SololevelingModMobEffects.TRAP_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.TRAP_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.TRAP_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Back Step")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).rangerleapnum < 3) {
				return "Charges: " + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).rangerleapnum + " / CD:"
						+ (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).rangerleaptimer / 20;
			} else {
				return "Charges: " + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).rangerleapnum;
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("High Value Target")) {
			if (entity instanceof LivingEntity _livEnt85 && _livEnt85.hasEffect(SololevelingModMobEffects.ARROW_RAIN_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.ARROW_RAIN_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.ARROW_RAIN_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Hawkeye")) {
			if (entity instanceof LivingEntity _livEnt87 && _livEnt87.hasEffect(SololevelingModMobEffects.HAWKEYE_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.HAWKEYE_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.HAWKEYE_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Hyper Focus")) {
			if (entity instanceof LivingEntity _livEnt89 && _livEnt89.hasEffect(SololevelingModMobEffects.FOCUS_COOLDOWN.get())) {
				return "" + ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.FOCUS_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.FOCUS_COOLDOWN.get()).getDuration() : 0) / 20);
			}
		}
		return "";
	}
}
