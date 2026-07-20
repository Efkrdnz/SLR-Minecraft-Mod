
package net.solocraft.item;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import java.util.Comparator;
import java.util.List;

public class DemonKingsLongSwordItem extends SwordItem {
	public DemonKingsLongSwordItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 10f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2.8f, new Item.Properties().fireResistant());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		stormOfTheFlames(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, sourceentity, itemstack);
		return retval;
	}

	private static void stormOfTheFlames(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity, ItemStack itemstack) {
		if (entity == null || sourceentity == null)
			return;
		double limit = 0;
		double max = 0;
		if (sourceentity instanceof LivingEntity living && living.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE.get()) && !(sourceentity instanceof Player player && player.getCooldowns().isOnCooldown(itemstack.getItem()))) {
			if (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).JOB == 4) {
				max = 6;
			} else {
				max = 3;
			}
			if (sourceentity instanceof Player player)
				player.getCooldowns().addCooldown(itemstack.getItem(), player.isCreative() ? 10 : 60);
			final Vec3 center = new Vec3(x, y, z);
			List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(15 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(found -> found.distanceToSqr(center))).toList();
			for (Entity nearby : entities) {
				if (isValidStormTarget(sourceentity, nearby)) {
					if (limit <= max) {
						limit = limit + 1;
						if (world instanceof ServerLevel level) {
							LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
							lightning.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(nearby.getX(), nearby.getY(), nearby.getZ())));
							lightning.setVisualOnly(true);
							level.addFreshEntity(lightning);
						}
						nearby.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), sourceentity),
								(float) (5 + sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).Intelligence / 15));
					}
				}
			}
		}
	}

	private static boolean isValidStormTarget(Entity sourceentity, Entity target) {
		if (sourceentity == target)
			return false;
		if (target instanceof TamableAnimal tamable && sourceentity instanceof LivingEntity living && tamable.isOwnedBy(living))
			return false;
		String sourceTeam = sourceentity instanceof LivingEntity living ? teamName(living) : "";
		String targetTeam = target instanceof LivingEntity living ? teamName(living) : "";
		return !sourceTeam.equals(targetTeam) || sourceTeam.equals("");
	}

	private static String teamName(LivingEntity entity) {
		if (entity.level().getScoreboard().getPlayersTeam(entity.getStringUUID()) == null)
			return "";
		return entity.level().getScoreboard().getPlayersTeam(entity instanceof Player player ? player.getGameProfile().getName() : entity.getStringUUID()).getName();
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A76LEVEL OF DIFFICULTY: S"));
		list.add(Component.literal("\u00A76ATTACK +350"));
		list.add(Component.literal("\u00A76TYPE: LONGSWORD"));
		list.add(Component.literal("\u00A76A LONGSWORD CONTANING THE POWERS OF BARAN, THE DEMON KING. THE EFFECT \"STORM OF THE FLAMES\" WILL ACTIVATE EVERY TIME THIS SWORD IS SWUNG."));
		list.add(Component.literal("\u00A76EFFECT \"STORM OF THE FLAMES\" : A VIOLENT THUNDERSTORM IS SUMMONED WITHIN A SPECIFIED AREA"));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE.get());
	}
}
