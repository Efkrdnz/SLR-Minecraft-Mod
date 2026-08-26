package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobSkillManager;
import net.solocraft.util.VesselProgressionManager;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class RunestoneShadowBodyManifestationProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z,
			Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if (world instanceof Level level && level.isClientSide())
			return;
		if (!VesselProgressionManager.isShadowMonarch(entity)) {
			if (entity instanceof Player player)
				player.displayClientMessage(net.minecraft.network.chat.Component.literal(
						"Only the Shadow Monarch can learn this skill."), true);
			return;
		}
		if (DoesHaveShadowManifestationProcedure.execute(entity)) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(
						net.minecraft.network.chat.Component.literal(
								"You already have \"Spiritual Body Manifestation\""), true);
			return;
		}

		if (entity instanceof ServerPlayer player) {
			AdvancementHolder advancement = player.server.getAdvancements().get(
					ResourceLocation.parse(
							"sololeveling:shadow_spiritual_body_manifestation"));
			if (advancement != null) {
				AdvancementProgress progress =
						player.getAdvancements().getOrStartProgress(advancement);
				for (String criteria : progress.getRemainingCriteria())
					player.getAdvancements().award(advancement, criteria);
			}
		}
		if (entity instanceof Player player) {
			ItemStack stackToRemove = itemstack;
			player.getInventory().clearOrCountMatchingItems(
					stack -> stackToRemove.getItem() == stack.getItem(), 1,
					player.inventoryMenu.getCraftSlots());
		}
		if (world instanceof Level level)
			level.playSound(null, BlockPos.containing(x, y, z),
					BuiltInRegistries.SOUND_EVENT.get(
							ResourceLocation.parse("block.enchantment_table.use")),
					SoundSource.NEUTRAL, 1, 1);
		JobSkillManager.markRunestoneSkill(
				entity, JobSkillManager.RUNESTONE_SHADOW_MANIFESTATION_TAG);
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.ShadowBody = true;
					capability.syncPlayerVariables(entity);
				});
		JobSkillManager.syncJobSkills(entity);
	}
}
