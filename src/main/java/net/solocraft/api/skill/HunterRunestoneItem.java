package net.solocraft.api.skill;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A ready-made runestone that teaches one contributed ability.
 *
 * <p>Runestones are how the mod teaches a class ability, so a contributed one
 * that arrived any other way would feel like a different game. Register an
 * instance in your own item registry and supply a model and texture; the
 * behaviour -- the enchanted-looking stone, a tooltip in the mod's format,
 * teaching on right click, and being spent only when something was learned --
 * comes with it.
 *
 * <pre>
 * ITEMS.register("runestone_cinder_slash", () -&gt; new HunterRunestoneItem("Cinder Slash"));
 * </pre>
 *
 * <p>The stone names its ability rather than holding it. Items are registered
 * during mod construction while JSON definitions arrive with the datapack, so a
 * stone that demanded the ability up front could never teach a JSON-defined one.
 * The lookup happens when the stone is used or its tooltip drawn, by which time
 * the definition exists.
 *
 * <p>Everything it does is server-side, so a client cannot teach itself an ability.
 */
public class HunterRunestoneItem extends Item {
	private final String abilityName;

	public HunterRunestoneItem(String abilityName) {
		this(abilityName, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
	}

	public HunterRunestoneItem(String abilityName, Item.Properties properties) {
		super(properties);
		if (abilityName == null || abilityName.isBlank())
			throw new IllegalArgumentException("A runestone must name the ability it teaches");
		this.abilityName = abilityName.trim();
	}

	/** The ability this stone teaches, as the skill list stores it. */
	public String abilityName() {
		return abilityName;
	}

	/** Null until the definition has loaded, which is why nothing caches it. */
	protected HunterAbility ability() {
		return HunterAbilityRegistry.byName(abilityName).orElse(null);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context,
			List<Component> lines, TooltipFlag flag) {
		super.appendHoverText(stack, context, lines, flag);
		HunterAbility ability = ability();
		if (ability == null) {
			// The definition did not load, or has not reached this client yet. Say
			// so rather than drawing a stone that looks like it teaches nothing.
			lines.add(Component.literal("Teaches \"" + abilityName + "\" (definition not loaded)")
					.withStyle(ChatFormatting.RED));
			return;
		}

		lines.add(Component.literal("Right click this rune to obtain \""
				+ HunterAbilityRegistry.displayName(ability.name()) + "\"")
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.literal(ability.summary()).withStyle(ability.accent()));
		if (!ability.detail().isBlank())
			lines.add(Component.literal(ability.detail()).withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer))
			return result;

		HunterAbility ability = ability();
		if (ability == null) {
			serverPlayer.displayClientMessage(Component.literal(
					"This rune teaches \"" + abilityName + "\", which is not loaded.")
					.withStyle(ChatFormatting.RED), false);
			// Not the player's fault, so the stone survives to be used once the
			// definition is fixed.
			return result;
		}

		if (!HunterSkills.learn(serverPlayer, ability.name())) {
			serverPlayer.displayClientMessage(
					Component.literal("You already have this skill!").withStyle(ChatFormatting.GRAY),
					false);
			return result;
		}

		serverPlayer.displayClientMessage(Component.literal("Learned ")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(HunterAbilityRegistry.displayName(ability.name()))
						.withStyle(ability.accent(), ChatFormatting.BOLD)), false);

		if (!serverPlayer.getAbilities().instabuild)
			result.getObject().shrink(1);
		return result;
	}
}
