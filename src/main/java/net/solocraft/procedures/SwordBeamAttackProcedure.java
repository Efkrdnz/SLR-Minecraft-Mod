package net.solocraft.procedures;

import net.solocraft.entity.SwordBeamProjectileEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.CooldownManager;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class SwordBeamAttackProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity))
			return;
		if (world instanceof Level level && level.isClientSide())
			return;
		if (CooldownManager.isOnCooldown(entity, "Sword Beam"))
			return;
		CooldownManager.set(entity, "Sword Beam", 50);
		livingEntity.swing(InteractionHand.MAIN_HAND, true);
		float damage = (float) Math.max(1.0D, livingEntity.getAttribute(Attributes.ATTACK_DAMAGE).getValue() * 0.34D);
		Level level = livingEntity.level();
		SwordBeamProjectileEntity projectile = new SwordBeamProjectileEntity(SololevelingModEntities.SWORD_BEAM_PROJECTILE.get(), livingEntity, level);
		Vec3 look = livingEntity.getLookAngle();
		Vec3 eye = livingEntity.getEyePosition();
		projectile.setOwner(livingEntity);
		projectile.setBaseDamage(damage);
		projectile.setKnockback(1);
		projectile.setSilent(true);
		projectile.setCritArrow(false);
		projectile.setOrigin(eye.x, eye.y, eye.z);
		projectile.setRoll(Mth.nextFloat(level.getRandom(), -18.0F, 18.0F));
		projectile.setPos(eye.x + look.x * 0.7D, eye.y - 0.12D + look.y * 0.7D, eye.z + look.z * 0.7D);
		projectile.shoot(look.x, look.y, look.z, 3.15F, 0.0F);
		level.addFreshEntity(projectile);
		playSound(level, x, y, z);
	}

	private static void playSound(Level level, double x, double y, double z) {
		float pitch = Mth.nextFloat(level.getRandom(), 0.74F, 0.92F);
		level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, 0.9F, pitch);
	}
}
