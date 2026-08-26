package net.solocraft.client.renderer;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.RangerClientState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Renders the Spirit Bow's continuous drawstring and nocked arrows inside the
 * fully transformed item pose.
 *
 * <p>This is invoked from {@code ItemRenderer} after the active bow's vanilla,
 * resource-pack, and custom JSON transforms have been applied.</p>
 */
public final class RangerManaQuiverHandRenderer {
	private static final Vec3 GENERIC_NOCK = new Vec3(0.5D, 0.5D, 0.5D);
	private static final Vec3 SPIRIT_BOW_REST_NOCK =
			new Vec3(5.875D / 16.0D, 14.95D / 16.0D, 7.625D / 16.0D);
	private static final Vec3 SPIRIT_BOW_STRING_TOP =
			new Vec3(5.875D / 16.0D, 19.075D / 16.0D, 7.625D / 16.0D);
	private static final Vec3 SPIRIT_BOW_STRING_BOTTOM =
			new Vec3(5.875D / 16.0D, 10.825D / 16.0D, 7.625D / 16.0D);
	private static final double SPIRIT_BOW_MAX_STRING_PULL = 0.85D / 16.0D;

	private RangerManaQuiverHandRenderer() {
	}

	public static void renderNockedArrow(ItemStack stack, ItemDisplayContext context, boolean leftHand,
			PoseStack poseStack, MultiBufferSource buffers) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		boolean bow = stack.getItem() instanceof BowItem;
		boolean activeUse = bow && player != null && player.isUsingItem()
				&& player.getUseItem().getItem() instanceof BowItem
				&& ItemStack.isSameItemSameComponents(stack, player.getUseItem())
				&& isRenderedActiveHand(player, context);

		float draw = 0.0F;
		if (activeUse) {
			float useTicks = player.getTicksUsingItem()
					+ minecraft.getTimer().getGameTimeDeltaPartialTick(false);
			float rawDraw = Mth.clamp(useTicks / 20.0F, 0.0F, 1.0F);
			draw = Mth.clamp((rawDraw * rawDraw + rawDraw * 2.0F) / 3.0F, 0.0F, 1.0F);
		}

		boolean spiritBow = stack.is(SololevelingModItems.SPIRIT_BOW.get());
		double forwardZ = spiritBowForwardZ(context, leftHand);
		Vec3 spiritNock = SPIRIT_BOW_REST_NOCK;
		if (spiritBow) {
			/*
			 * The first- and third-person JSON transforms face opposite local-Z
			 * directions. Keep the nock centered between the authored limb anchors
			 * and move only in depth; changing local X/Y makes the junction float
			 * above the grip after the near-90-degree item rotations.
			 */
			double nockZ = SPIRIT_BOW_REST_NOCK.z
					- forwardZ * SPIRIT_BOW_MAX_STRING_PULL * draw;
			spiritNock = new Vec3(SPIRIT_BOW_REST_NOCK.x, SPIRIT_BOW_REST_NOCK.y, nockZ);
			ManaArrowVisual.renderBowString(poseStack, buffers,
					SPIRIT_BOW_STRING_TOP, spiritNock, 0.0019D);
			ManaArrowVisual.renderBowString(poseStack, buffers,
					spiritNock, SPIRIT_BOW_STRING_BOTTOM, 0.0019D);
		}

		if (!context.firstPerson() || !activeUse)
			return;

		float formation = 0.26F + draw * 0.74F;

		Vec3 nock = spiritBow ? spiritNock : GENERIC_NOCK;
		if (!spiritBow)
			forwardZ = leftHand ? -1.0D : 1.0D;
		Vec3 forward = new Vec3(0.0D, 0.0D, forwardZ);
		double visibleLength = spiritBow
				? 0.040D + 0.0225D * formation
				: 0.105D + 0.055D * formation;
		Vec3 tip = nock.add(forward.scale(visibleLength));

		boolean manaArrow = RangerClientState.quiverActive
				&& player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.map(variables -> Math.round(variables.Classes) == 6L).orElse(false);
		if (!manaArrow) {
			if (spiritBow) {
				ManaArrowVisual.renderPhysicalNocked(poseStack, buffers, nock, tip,
						0.0027D, 0.016D, 0.0065D);
			}
			return;
		}

		int stage = Math.max(1, Mth.clamp(RangerClientState.chargeStage, 0, 3));
		float lock = RangerClientState.locked
				? 1.0F : Mth.clamp(RangerClientState.lockProgress, 0.0F, 1.0F);
		float intensity = Mth.clamp(0.58F + draw * 0.30F + stage * 0.025F + lock * 0.045F,
				0.0F, 1.0F);

		if (spiritBow) {
			ManaArrowVisual.renderNocked(poseStack, buffers, nock, tip, stage,
					RangerClientState.locked, intensity, 0.0032D, 0.018D, 0.0075D);
		} else {
			ManaArrowVisual.renderNocked(poseStack, buffers, nock, tip, stage,
					RangerClientState.locked, intensity, 0.0055D, 0.050D, 0.020D);
		}
	}

	private static double spiritBowForwardZ(ItemDisplayContext context, boolean leftHand) {
		return switch (context) {
			case FIRST_PERSON_RIGHT_HAND -> -1.0D;
			case FIRST_PERSON_LEFT_HAND -> 1.0D;
			case THIRD_PERSON_RIGHT_HAND -> 1.0D;
			case THIRD_PERSON_LEFT_HAND -> -1.0D;
			default -> leftHand ? 1.0D : -1.0D;
		};
	}

	private static boolean isRenderedActiveHand(LocalPlayer player, ItemDisplayContext context) {
		HumanoidArm renderedArm;
		if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
			renderedArm = HumanoidArm.RIGHT;
		} else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
			renderedArm = HumanoidArm.LEFT;
		} else {
			return false;
		}
		HumanoidArm activeArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
				? player.getMainArm() : player.getMainArm().getOpposite();
		return renderedArm == activeArm;
	}
}
