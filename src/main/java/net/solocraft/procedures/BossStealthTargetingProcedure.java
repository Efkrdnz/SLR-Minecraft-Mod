package net.solocraft.procedures;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

@Mod.EventBusSubscriber
public class BossStealthTargetingProcedure {
	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingTickEvent event) {
		if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob boss) || !StealthBossDetectionHelper.seesThroughStealth(boss))
			return;
		if (boss.tickCount % 10 != 0 || boss.isNoAi() || !boss.isAlive())
			return;
		LivingEntity currentTarget = boss.getTarget();
		if (isValidStealthedTarget(boss, currentTarget))
			return;
		Player target = boss.level().getNearestPlayer(boss.getX(), boss.getY(), boss.getZ(), followRange(boss), entity -> entity instanceof Player player && isValidStealthedTarget(boss, player));
		if (target != null)
			boss.setTarget(target);
	}

	private static boolean isValidStealthedTarget(Mob boss, LivingEntity target) {
		if (!(target instanceof Player player) || !player.isAlive() || player.isCreative() || player.isSpectator())
			return false;
		return player.hasEffect(MobEffects.INVISIBILITY) && boss.hasLineOfSight(player);
	}

	private static double followRange(Mob boss) {
		if (boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE) != null)
			return Math.max(16.0D, boss.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
		return 48.0D;
	}
}
