package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModGameRules;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class BloodEffectProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity, event.getSource().getEntity(), event.getAmount());
		}
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity, double amount) {
		execute(null, world, entity, sourceentity, amount);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceentity, double amount) {
		if (entity == null || sourceentity == null)
			return;
		boolean can_initiate = false;
		if (!world.isClientSide()) {
			can_initiate = world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_BLOOD_EFFECTS);
			if (can_initiate) {
				if (!entity.isInvulnerable()) {
					if (!entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
						if (!(entity instanceof TamableAnimal _tamIsTamedBy && sourceentity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false)) {
							if (amount >= 2) {
								if (world instanceof ServerLevel _level)
									_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.BLOOD_PARTICLE.get()), (entity.getX()), (entity.getY() + (entity.getBbHeight() * 2) / 3), (entity.getZ()), 45, (entity.getBbWidth() / 2),
											(entity.getBbHeight() / 3), (entity.getBbWidth() / 2), 0.25);
							}
						}
					}
				}
			}
		}
	}
}
