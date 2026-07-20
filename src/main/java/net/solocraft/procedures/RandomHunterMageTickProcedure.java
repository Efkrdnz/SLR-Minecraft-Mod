package net.solocraft.procedures;

import net.solocraft.entity.HunterEntity;
import net.solocraft.util.ArcaneMageSpellManager;
import net.solocraft.util.BarrierMageSpellManager;
import net.solocraft.util.FireMageSpellManager;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class RandomHunterMageTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double Rank = 0;
		double rand = 0;
		double dmg_modifier = 0;
		if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("S")) {
			dmg_modifier = 20;
		} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("A")) {
			dmg_modifier = 14;
		} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("B")) {
			dmg_modifier = 10;
		} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("C")) {
			dmg_modifier = 6;
		} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("D")) {
			dmg_modifier = 5;
		}
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			HunterAIHelper.casterBacklineTick(entity);
			entity.lookAt(EntityAnchorArgument.Anchor.EYES,
					new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getBbHeight()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
			if ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) <= 45) {
				if (entity instanceof HunterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(HunterEntity.DATA_IA, (int) ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) + 1));
				if ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) == 30) {
					if (dmg_modifier >= 10) {
						if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof LivingEntity _livEnt38 && _livEnt38.hasEffect(MobEffects.INVISIBILITY)) {
							DetectEyeSpawnProcedure.execute(world, x, y, z, entity);
						}
					}
				}
				if ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) == 40) {
					int stage = FireMageSpellManager.outputStage(entity);
					if (stage > 0) {
						Entity combatTarget = entity instanceof Mob mob ? mob.getTarget() : null;
						if (combatTarget == null)
							return;
						entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(combatTarget.getX(),
								combatTarget.getY() + combatTarget.getBbHeight() * 0.5D, combatTarget.getZ()));
						rand = Mth.nextInt(RandomSource.create(), 1, 100);
						String specialization = entity.getPersistentData().getString("sl_hunter_mage_specialization");
						if (specialization.isBlank()) {
							specialization = switch (entity.level().getRandom().nextInt(3)) {
								case 1 -> "barrier";
								case 2 -> "arcane";
								default -> "fire";
							};
							entity.getPersistentData().putString("sl_hunter_mage_specialization", specialization);
						}
						if ("arcane".equals(specialization)) {
							String arcaneSpell;
							if (stage == 1)
								arcaneSpell = ArcaneMageSpellManager.AETHER_BOLT;
							else if (stage == 2)
								arcaneSpell = rand <= 48 ? ArcaneMageSpellManager.AETHER_BOLT
										: rand <= 75 ? ArcaneMageSpellManager.VECTOR_STEP
										: ArcaneMageSpellManager.POLARITY_SPHERE;
							else if (stage == 3)
								arcaneSpell = rand <= 34 ? ArcaneMageSpellManager.AETHER_BOLT
										: rand <= 58 ? ArcaneMageSpellManager.POLARITY_SPHERE
										: rand <= 78 ? ArcaneMageSpellManager.RUNIC_RELAY
										: ArcaneMageSpellManager.VECTOR_STEP;
							else if (stage == 4)
								arcaneSpell = rand <= 24 ? ArcaneMageSpellManager.ASTRAL_ARSENAL
										: rand <= 45 ? ArcaneMageSpellManager.DIMENSIONAL_REND
										: rand <= 67 ? ArcaneMageSpellManager.POLARITY_SPHERE
										: rand <= 83 ? ArcaneMageSpellManager.VECTOR_STEP
										: ArcaneMageSpellManager.AETHER_BOLT;
							else
								arcaneSpell = rand <= 8 ? ArcaneMageSpellManager.CONVERGENCE
										: rand <= 29 ? ArcaneMageSpellManager.DIMENSIONAL_REND
										: rand <= 50 ? ArcaneMageSpellManager.ASTRAL_ARSENAL
										: rand <= 70 ? ArcaneMageSpellManager.POLARITY_SPHERE
										: rand <= 86 ? ArcaneMageSpellManager.VECTOR_STEP
										: ArcaneMageSpellManager.AETHER_BOLT;
							if (!ArcaneMageSpellManager.castNpc(entity, arcaneSpell))
								ArcaneMageSpellManager.castNpc(entity, ArcaneMageSpellManager.AETHER_BOLT);
							return;
						}
						if ("barrier".equals(specialization)) {
							String barrierSpell;
							if (stage == 1)
								barrierSpell = BarrierMageSpellManager.FRACTURE_BOLT;
							else if (stage == 2)
								barrierSpell = rand <= 50 ? BarrierMageSpellManager.FRACTURE_BOLT
										: rand <= 78 ? BarrierMageSpellManager.REPULSION_FRAME
										: BarrierMageSpellManager.PRISM_RAMPART;
							else if (stage == 3)
								barrierSpell = rand <= 34 ? BarrierMageSpellManager.FRACTURE_BOLT
										: rand <= 59 ? BarrierMageSpellManager.REPULSION_FRAME
										: rand <= 82 ? BarrierMageSpellManager.SEALING_PRISM
										: BarrierMageSpellManager.PRISM_RAMPART;
							else if (stage == 4)
								barrierSpell = rand <= 23 ? BarrierMageSpellManager.MIRROR_WARD
										: rand <= 45 ? BarrierMageSpellManager.RESONANT_COLLAPSE
										: rand <= 69 ? BarrierMageSpellManager.SEALING_PRISM
										: rand <= 86 ? BarrierMageSpellManager.REPULSION_FRAME
										: BarrierMageSpellManager.FRACTURE_BOLT;
							else
								barrierSpell = rand <= 10 ? BarrierMageSpellManager.ABSOLUTE_BASTION
										: rand <= 30 ? BarrierMageSpellManager.RESONANT_COLLAPSE
										: rand <= 49 ? BarrierMageSpellManager.MIRROR_WARD
										: rand <= 69 ? BarrierMageSpellManager.SEALING_PRISM
										: rand <= 86 ? BarrierMageSpellManager.REPULSION_FRAME
										: BarrierMageSpellManager.FRACTURE_BOLT;
							if (!BarrierMageSpellManager.castNpc(entity, barrierSpell))
								BarrierMageSpellManager.castNpc(entity, BarrierMageSpellManager.FRACTURE_BOLT);
							return;
						}
						String spell;
						if (stage == 1)
							spell = rand <= 42 ? FireMageSpellManager.FLAME_WEAVING : FireMageSpellManager.IGNITION_ORB;
						else if (stage == 2)
							spell = rand <= 34 ? FireMageSpellManager.IGNITION_ORB
									: rand <= 76 ? FireMageSpellManager.INFERNO_LANCE : FireMageSpellManager.FLASHFIRE;
						else if (stage == 3)
							spell = rand <= 34 ? FireMageSpellManager.INFERNO_LANCE
									: rand <= 62 ? FireMageSpellManager.FLASHFIRE
									: rand <= 80 ? FireMageSpellManager.CREMATION : FireMageSpellManager.IGNITION_ORB;
						else if (stage == 4)
							spell = rand <= 17 ? FireMageSpellManager.FURNACE_DOMINION
									: rand <= 39 ? FireMageSpellManager.CREMATION
									: rand <= 72 ? FireMageSpellManager.INFERNO_LANCE : FireMageSpellManager.FLASHFIRE;
						else
							spell = rand <= 9 ? FireMageSpellManager.HEAVENFALL
									: rand <= 28 ? FireMageSpellManager.FURNACE_DOMINION
									: rand <= 52 ? FireMageSpellManager.CREMATION
									: rand <= 80 ? FireMageSpellManager.INFERNO_LANCE : FireMageSpellManager.FLASHFIRE;
						if (!FireMageSpellManager.castNpc(entity, spell) && FireMageSpellManager.CREMATION.equals(spell))
							FireMageSpellManager.castNpc(entity, FireMageSpellManager.INFERNO_LANCE);
					}
				}
			} else {
				if (entity instanceof HunterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(HunterEntity.DATA_IA, 0);
			}
		}
	}
}
