package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class BackStepChargeProcedure {
	private static final int MAX_CHARGES = 3;
	private static final int RECHARGE_TICKS = 180;
	private static final String INITIALIZED = "slr_back_step_charges_initialized";

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player);
		}
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity.level().isClientSide())
			return;
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (!(vars.Classes == 6 || vars.Plist.contains("Back Step")))
			return;
		if (!entity.getPersistentData().getBoolean(INITIALIZED)) {
			entity.getPersistentData().putBoolean(INITIALIZED, true);
			setCharges(entity, MAX_CHARGES, 0);
			return;
		}
		double charges = Math.max(0, Math.min(MAX_CHARGES, vars.rangerleapnum));
		double timer = Math.max(0, vars.rangerleaptimer);
		if (charges >= MAX_CHARGES) {
			if (vars.rangerleapnum != MAX_CHARGES || vars.rangerleaptimer != 0)
				setCharges(entity, MAX_CHARGES, 0);
			return;
		}
		if (timer <= 0) {
			timer = RECHARGE_TICKS;
		} else {
			timer--;
		}
		if (timer <= 0) {
			charges = Math.min(MAX_CHARGES, charges + 1);
			timer = charges >= MAX_CHARGES ? 0 : RECHARGE_TICKS;
		}
		setCharges(entity, charges, timer);
	}

	private static void setCharges(Entity entity, double charges, double timer) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.rangerleapnum = charges;
			capability.rangerleaptimer = timer;
			capability.syncPlayerVariables(entity);
		});
	}
}
