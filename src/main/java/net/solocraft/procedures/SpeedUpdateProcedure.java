package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.TemporaryStatBonusManager;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@EventBusSubscriber
public class SpeedUpdateProcedure {
	private static final ResourceLocation AGILITY_SWIM_SPEED_MODIFIER =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "agility_swim_speed");
	private static final double SPRINT_AGILITY_BASELINE = 0.13D;
	private static final double AGILITY_SPEED_PER_POINT = 0.0005D;
	private static final double MAX_SWIM_SPEED_MULTIPLIER = 4.0D;

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (!(entity instanceof LivingEntity living))
			return;
		double effectiveAgility = TemporaryStatBonusManager.effectiveAgility(entity);
		SololevelingModVariables.PlayerVariables variables = entity.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		living.getAttribute(Attributes.MOVEMENT_SPEED)
				.setBaseValue(variables.dash * 0.1);
		if (entity.isSprinting()) {
			living.getAttribute(Attributes.MOVEMENT_SPEED)
					.setBaseValue(variables.dash
							* (SPRINT_AGILITY_BASELINE
									+ AGILITY_SPEED_PER_POINT * effectiveAgility
											* (variables.speedpercent / 100)));
		}
		updateAgilitySwimSpeed(living, effectiveAgility,
				variables.speedpercent);
		if (effectiveAgility < 30) {
			living.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.9D);
		} else if (effectiveAgility < 50) {
			living.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.2D);
		} else if (effectiveAgility < 100) {
			living.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.8D);
		} else {
			living.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(2.2D);
		}
		if (!entity.onGround()) {
			if (effectiveAgility >= 70) {
				entity.fallDistance = 0;
			}
		}
	}

	private static void updateAgilitySwimSpeed(LivingEntity living,
			double effectiveAgility, double speedPercent) {
		AttributeInstance swimSpeed = living.getAttribute(
				NeoForgeMod.SWIM_SPEED);
		if (swimSpeed == null)
			return;
		double modifierAmount = agilitySwimSpeedMultiplier(
				effectiveAgility, speedPercent) - 1.0D;
		AttributeModifier current = swimSpeed.getModifier(
				AGILITY_SWIM_SPEED_MODIFIER);
		if (modifierAmount <= 0.0D) {
			if (current != null)
				swimSpeed.removeModifier(AGILITY_SWIM_SPEED_MODIFIER);
			return;
		}
		if (current != null
				&& Math.abs(current.amount() - modifierAmount) < 0.000001D)
			return;
		if (current != null)
			swimSpeed.removeModifier(AGILITY_SWIM_SPEED_MODIFIER);
		swimSpeed.addTransientModifier(new AttributeModifier(
				AGILITY_SWIM_SPEED_MODIFIER, modifierAmount,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	static double agilitySwimSpeedMultiplier(double effectiveAgility,
			double speedPercent) {
		double usedAgility = Math.max(0.0D, effectiveAgility);
		double usage = Math.max(0.0D, Math.min(100.0D, speedPercent))
				/ 100.0D;
		double sprintRelativeBonus = AGILITY_SPEED_PER_POINT * usedAgility
				* usage / SPRINT_AGILITY_BASELINE;
		return Math.min(MAX_SWIM_SPEED_MULTIPLIER,
				1.0D + sprintRelativeBonus);
	}
}
