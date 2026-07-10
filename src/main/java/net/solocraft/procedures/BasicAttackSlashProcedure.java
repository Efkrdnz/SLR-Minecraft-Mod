package net.solocraft.procedures;

import net.solocraft.entity.BasicAttackSlashEntity;
import net.solocraft.network.SololevelingModVariables;

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

public class BasicAttackSlashProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, int style, int swingIndex) {
		if (!(entity instanceof LivingEntity livingEntity))
			return;
		livingEntity.swing(InteractionHand.MAIN_HAND, true);
		float attack = (float) livingEntity.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
		double strength = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).map(cap -> cap.Strength).orElse(0.0D);
		float damage = switch (style) {
			case BasicAttackSlashEntity.STYLE_FIST -> (float) ((3.0D + strength / 18.0D) * (swingIndex == 1 ? 1.45D : 1.0D));
			case BasicAttackSlashEntity.STYLE_DAGGER -> attack * 0.72F;
			case BasicAttackSlashEntity.STYLE_DUAL_DAGGER -> attack * 0.58F;
			default -> attack;
		};
		float scale = switch (style) {
			case BasicAttackSlashEntity.STYLE_FIST -> swingIndex == 1 ? 1.15F : 1.0F;
			case BasicAttackSlashEntity.STYLE_DAGGER -> 0.9F + swingIndex * 0.06F;
			case BasicAttackSlashEntity.STYLE_DUAL_DAGGER -> 0.98F + swingIndex * 0.05F;
			default -> 1.05F;
		};
		BasicAttackSlashEntity.spawn(world, livingEntity, style, swingIndex, damage, scale);
		playSound(world, x, y, z, style);
	}

	private static void playSound(LevelAccessor world, double x, double y, double z, int style) {
		if (!(world instanceof Level level))
			return;
		boolean fist = style == BasicAttackSlashEntity.STYLE_FIST;
		ResourceLocation sound = fist ? new ResourceLocation("sololeveling:impact1") : new ResourceLocation("sololeveling:basic_slash");
		float volume = fist ? 0.55F : 0.5F;
		float pitch = fist ? 0.82F : Mth.nextFloat(level.getRandom(), 1.35F, 1.65F);
		if (!level.isClientSide()) {
			level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(sound), SoundSource.NEUTRAL, volume, pitch);
		} else {
			level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(sound), SoundSource.NEUTRAL, volume, pitch, false);
		}
	}
}
