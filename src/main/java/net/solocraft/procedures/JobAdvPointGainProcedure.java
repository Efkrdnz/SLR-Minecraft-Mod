package net.solocraft.procedures;

import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@Mod.EventBusSubscriber
public class JobAdvPointGainProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof DKnight1Entity || event.getEntity() instanceof DKnight2Entity || event.getEntity() instanceof DKnight3Entity))
			return;
		ServerPlayer player = ShadowKillCreditHelper.creditedServerPlayer(event.getEntity().level(), event.getSource().getEntity());
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
