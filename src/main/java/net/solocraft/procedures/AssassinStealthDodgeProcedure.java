package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.CooldownManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class AssassinStealthDodgeProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity, event.getSource().getEntity());
		}
	}

	public static void execute(Entity entity, Entity sourceentity) {
		execute(null, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double scale_opp = 0;
		double scale_self = 0;
		if (CooldownManager.isOnCooldown(entity, "Stealth") && entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(MobEffects.INVISIBILITY)) {
			if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("hunters"))) || sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("dm")))) {
				scale_opp = sourceentity.getPersistentData().getDouble("Level");
			} else if (sourceentity instanceof Player) {
				scale_opp = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level;
			}
			if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("dm"))) || entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("hunters")))) {
				scale_self = entity.getPersistentData().getDouble("Level");
			} else if (entity instanceof Player) {
				scale_self = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level;
			}
			if (Math.random() < (scale_self) / ((float) scale_opp + 30)) {
				if (event != null && event.isCancelable()) {
					event.setCanceled(true);
				}
			}
		}
	}
}
