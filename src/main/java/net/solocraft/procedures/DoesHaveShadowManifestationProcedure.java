package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.VesselProgressionManager;

import net.minecraft.world.entity.Entity;

public class DoesHaveShadowManifestationProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (VesselProgressionManager.isShadowMonarch(entity)
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ShadowBody) {
			return true;
		}
		return false;
	}
}
