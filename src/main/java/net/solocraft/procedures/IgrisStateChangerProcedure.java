package net.solocraft.procedures;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class IgrisStateChangerProcedure {
    public static void execute(Entity entity) {
        if (entity == null)
            return;

        Mob mob = entity instanceof Mob m ? m : null;
        if (mob == null || mob.getTarget() == null)
            return;

        LivingEntity target    = mob.getTarget();
        boolean      enraged   = entity.getPersistentData().getBoolean("enraged");
        RandomSource rng       = entity.level().getRandom();
        double       dist      = entity.distanceTo(target);

        // Too far to commit to an attack — back off and retry in ~10 ticks
        if (dist > 5.5) {
            entity.getPersistentData().putString("state", "idle");
            entity.getPersistentData().putDouble("MF", 0);
            entity.getPersistentData().putDouble("nextAttackMF", 10);
            return;
        }

        // Lunge burst toward target when not already overlapping
        if (dist > 3.5 && !entity.level().isClientSide()) {
            Vec3 dir = target.position().subtract(entity.position()).normalize();
            entity.setDeltaMovement(dir.x * 0.8, 0.1, dir.z * 0.8);
        }

        // ── Phase-aware attack selection ──────────────────────────────────────
        // Normal:  spin 30 %, stab 35 %, slam 25 %, scream 10 %
        // Enraged: spin 28 %, stab 27 %, slam 20 %, scream 25 %
        int    roll          = rng.nextInt(100);
        String nextState;
        int    slowdownTicks;

        if (!enraged) {
            if      (roll < 30) { nextState = "spin";   slowdownTicks = 40; }
            else if (roll < 65) { nextState = "stab";   slowdownTicks = 40; }
            else if (roll < 90) { nextState = "slam";   slowdownTicks = 40; }
            else                { nextState = "scream"; slowdownTicks = 30; }
        } else {
            if      (roll < 28) { nextState = "spin";   slowdownTicks = 35; }
            else if (roll < 55) { nextState = "stab";   slowdownTicks = 35; }
            else if (roll < 75) { nextState = "slam";   slowdownTicks = 35; }
            else                { nextState = "scream"; slowdownTicks = 25; }
        }

        // Root Igris during the attack animation
        if (entity instanceof LivingEntity le && !le.level().isClientSide())
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowdownTicks, 10));

        // Cooldown before next attack:  normal 20–35 t,  enraged 10–20 t
        int nextAttackMF = enraged
                ? (10 + rng.nextInt(11))
                : (20 + rng.nextInt(16));

        entity.getPersistentData().putString("state", nextState);
        entity.getPersistentData().putDouble("MF", 0);
        entity.getPersistentData().putDouble("nextAttackMF", nextAttackMF);
    }
}
