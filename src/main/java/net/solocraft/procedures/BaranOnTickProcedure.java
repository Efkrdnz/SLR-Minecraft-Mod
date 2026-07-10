package net.solocraft.procedures;

import net.solocraft.entity.BaranEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Main tick dispatcher for Baran's boss state machine.
 * Called every tick from BaranEntity.baseTick() on the server side.
 *
 * States: idle → (state changer picks) → magic_blast / lightning_storm /
 *         summon / ground_slam / charge → back to idle
 *
 * MF (motion frame) counts up each tick a target is present.
 * Phase 2 triggers at 50% HP.
 */
public class BaranOnTickProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(entity instanceof BaranEntity baran))
			return;

		LivingEntity target = (entity instanceof Mob mob) ? mob.getTarget() : null;

		// ── No target: reset to idle ────────────────────────────────────────
		if (target == null) {
			baran.getPersistentData().putDouble("MF", 0);
			baran.setState("idle");
			return;
		}

		// ── Phase 2 check (50% HP, only once) ──────────────────────────────
		if (!baran.getPersistentData().getBoolean("baran_phase2")
				&& baran.getHealth() <= baran.getMaxHealth() * 0.5f) {
			baran.getPersistentData().putBoolean("baran_phase2", true);
			// Announce enrage
			if (world instanceof ServerLevel serverLevel) {
				for (net.minecraft.server.level.ServerPlayer sp : serverLevel.players()) {
					if (sp.distanceTo(baran) <= 200) {
						sp.sendSystemMessage(Component.literal("§4§l⚡ DEMON KING BARAN ENRAGES! §r§cHis power grows unbounded!"));
					}
				}
				// Spawn 6 lightning bolts around Baran as a dramatic effect
				for (int i = 0; i < 6; i++) {
					double angle = (i / 6.0) * Math.PI * 2;
					double lx = x + Math.cos(angle) * 4;
					double lz = z + Math.sin(angle) * 4;
					net.minecraft.world.entity.LightningBolt bolt =
							net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(serverLevel);
					if (bolt != null) {
						bolt.moveTo(lx, y, lz);
						bolt.setVisualOnly(true); // purely visual — Baran is immune already
						serverLevel.addFreshEntity(bolt);
					}
				}
			}
			// Speed boost for phase 2
			baran.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
					.setBaseValue(0.38);
		}

		// ── Advance MF ─────────────────────────────────────────────────────
		double MF = baran.getPersistentData().getDouble("MF") + 1;
		baran.getPersistentData().putDouble("MF", MF);

		// ── Sync state to IA for client (optional visual use) ──────────────
		String state = baran.getState();

		// ── Trigger next attack from idle ───────────────────────────────────
		// Phase 2 cuts the idle threshold in half (more aggressive)
		boolean phase2 = baran.getPersistentData().getBoolean("baran_phase2");
		int idleThreshold = phase2 ? 6 : 10;

		if (state.equals("idle") && MF >= idleThreshold) {
			BaranStateChangerProcedure.execute(world, x, y, z, entity);
			return;
		}

		// ── Dispatch to active attack procedure ─────────────────────────────
		switch (state) {
			case "magic_blast"     -> BaranMagicBlastProcedure.execute(world, x, y, z, entity);
			case "lightning_storm" -> BaranLightningStormProcedure.execute(world, x, y, z, entity);
			case "summon"          -> BaranSummonProcedure.execute(world, x, y, z, entity);
			case "ground_slam"     -> BaranGroundSlamProcedure.execute(world, x, y, z, entity);
			case "charge"          -> BaranChargeProcedure.execute(world, x, y, z, entity);
		}
	}
}
