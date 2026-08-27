package net.solocraft.mixins;

import net.solocraft.entity.LegacyProjectileCompat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowKnockbackMixin {
	@Inject(method = "doKnockback", at = @At("TAIL"))
	private void sololeveling$applyLegacyKnockback(LivingEntity target, DamageSource source, CallbackInfo callback) {
		LegacyProjectileCompat.applyKnockback((AbstractArrow) (Object) this, target);
	}
}
