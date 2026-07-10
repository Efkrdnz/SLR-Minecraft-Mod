package net.solocraft.procedures;

import net.solocraft.entity.CrossStrikeEntity;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.CooldownManager;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class CrossStrikeProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity))
			return;
		if (world instanceof Level level && level.isClientSide())
			return;
		livingEntity.swing(InteractionHand.MAIN_HAND, true);
		float attack = (float) livingEntity.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
		double strength = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).map(cap -> cap.Strength).orElse(0.0D);
		float damage = (float) (attack * 0.85F + 4.0D + strength / 40.0D);
		CooldownManager.set(entity, "Cross Strike", 200);
		CrossStrikeEntity.spawn(world, livingEntity, damage, 1.0F);
		playSlashSound(world, x, y, z);
	}

	private static void playSlashSound(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof Level level))
			return;
		float pitch = Mth.nextFloat(level.getRandom(), 1.22F, 1.45F);
		level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:basic_slash")), SoundSource.NEUTRAL, 0.75F, pitch);
	}
}
