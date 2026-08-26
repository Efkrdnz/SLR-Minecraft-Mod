package net.solocraft.util;

import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight3Entity;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

/**
 * Encounter-only damage tuning for the knights created by Job Change portals.
 *
 * <p>The sword-bearing variants equip Igris's real longsword, whose item
 * modifier was being added on top of their level-scaled attack attribute.
 * Keeping the item preserves their appearance; this modifier brings the whole
 * encounter attack value back into the intended early-game range.</p>
 */
@EventBusSubscriber
public final class JobChangeKnightBalance {
	public static final String QUEST_KNIGHT_TAG =
			"slr_job_change_advancement_knight";
	private static final ResourceLocation DAMAGE_MODIFIER_ID =
			ResourceLocation.fromNamespaceAndPath("sololeveling",
					"attribute/job_change_knight_damage");
	private static final double DAMAGE_MULTIPLIER = -0.60D;

	private JobChangeKnightBalance() {
	}

	public static void markAndBalance(Entity entity) {
		if (!isKnight(entity))
			return;
		entity.getPersistentData().putBoolean(QUEST_KNIGHT_TAG, true);
		apply(entity);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		if (!event.getLevel().isClientSide()
				&& entity.getPersistentData().getBoolean(QUEST_KNIGHT_TAG))
			apply(entity);
	}

	private static void apply(Entity entity) {
		if (!isKnight(entity))
			return;
		AttributeInstance attack = ((net.minecraft.world.entity.LivingEntity) entity)
				.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack == null)
			return;
		if (attack.getModifier(DAMAGE_MODIFIER_ID) != null)
			attack.removeModifier(DAMAGE_MODIFIER_ID);
		attack.addPermanentModifier(new AttributeModifier(DAMAGE_MODIFIER_ID,
				DAMAGE_MULTIPLIER,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static boolean isKnight(Entity entity) {
		return entity instanceof DKnight1Entity
				|| entity instanceof DKnight2Entity
				|| entity instanceof DKnight3Entity;
	}
}
