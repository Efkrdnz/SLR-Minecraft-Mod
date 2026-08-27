package net.solocraft.procedures;

import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.util.JobChangeQuestManager;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@EventBusSubscriber
public class JobAdvPointGainProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof DKnight1Entity || event.getEntity() instanceof DKnight2Entity || event.getEntity() instanceof DKnight3Entity))
			return;
		Entity creditedSource = ShadowKillCreditHelper
				.creditedSourceForDeath(event.getEntity().level(),
						event.getEntity(), event.getSource().getEntity(),
						event.getSource().getDirectEntity());
		ServerPlayer player = ShadowKillCreditHelper.creditedServerPlayer(
				event.getEntity().level(), creditedSource);
		if (player != null)
			JobChangeQuestManager.grantAdvancementPoint(player, event.getEntity());
	}

	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null)
			return;
		ServerPlayer player = ShadowKillCreditHelper.creditedServerPlayer(entity.level(), sourceentity);
		if (player != null && (entity instanceof DKnight1Entity || entity instanceof DKnight2Entity || entity instanceof DKnight3Entity))
			JobChangeQuestManager.grantAdvancementPoint(player, entity);
	}
}
