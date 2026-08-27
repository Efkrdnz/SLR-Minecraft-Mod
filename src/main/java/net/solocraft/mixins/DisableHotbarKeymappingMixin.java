package net.solocraft.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;

import net.solocraft.init.SololevelingModKeyMappings;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

/**
 * Turns the vanilla hotbar keys into the skill-slot keys while in combat mode.
 *
 * <p>Combat mode already had to suppress hotbar slot switching, which meant the
 * key press was being detected and then thrown away. The mod used to register a
 * second set of bindings (AB_1..AB_8) defaulted to the same 1-8 keys purely to
 * hear the press it was already discarding here -- eight duplicate entries in
 * the controls screen for keys the player had bound once.
 *
 * <p>Routing the discarded press to the skill handler removes those bindings
 * entirely, and means a player who rebinds their hotbar to different keys gets
 * their skill slots moved with it for free.
 */
@Mixin(KeyMapping.class)
public abstract class DisableHotbarKeymappingMixin {
	@Shadow
	private int clickCount;

	@Unique
	private boolean solocraft$slotDown;

	/** 1-8 for the skill slots, or 0 when this is not a routed hotbar key. */
	@Unique
	private int solocraft$slot() {
		String keyName = ((KeyMapping) (Object) this).getName();
		if (!keyName.startsWith("key.hotbar."))
			return 0;
		try {
			int slot = Integer.parseInt(keyName.substring("key.hotbar.".length()));
			// Slot 9 stays a normal hotbar slot; the mod only has eight skills.
			return slot >= 1 && slot <= 8 ? slot : 0;
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	@Unique
	private boolean solocraft$combatMode() {
		Entity entity = Minecraft.getInstance().player;
		return entity != null && entity.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).combatmode;
	}

	@Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
	public void inject1(CallbackInfoReturnable<Boolean> cir) {
		String keyName = ((KeyMapping) (Object) this).getName();
		if (!keyName.startsWith("key.hotbar."))
			return;
		if (solocraft$combatMode()) {
			this.clickCount = 0;
			cir.setReturnValue(false);
		}
	}

	/**
	 * Edge-triggered so a held key fires once, matching what the old dedicated
	 * bindings did. Release is forwarded too: the radial selectors open on hold
	 * and commit on release, so swallowing it would leave a wheel stuck open.
	 */
	@Inject(method = "setDown", at = @At("TAIL"))
	public void solocraft$routeHotbarSkill(boolean isDown, CallbackInfo ci) {
		// Opening or closing a radial makes Minecraft flip every key's state as
		// bookkeeping -- releaseAll() on the way in, setAll() on the way out.
		// Those arrive here indistinguishable from real input, and acting on them
		// re-enters setScreen: begin -> release -> clear -> press -> begin, until
		// the stack runs out.
		if (net.solocraft.client.gui.RadialScreenTransition.isTransitioning())
			return;
		int slot = solocraft$slot();
		if (slot == 0 || solocraft$slotDown == isDown)
			return;
		solocraft$slotDown = isDown;
		// The release must always be delivered, even if combat mode was switched
		// off mid-hold, or a radial opened before the toggle never closes.
		if (!isDown) {
			SololevelingModKeyMappings.releaseHotbarSkill(slot);
			return;
		}
		if (solocraft$combatMode() && Minecraft.getInstance().screen == null)
			SololevelingModKeyMappings.pressHotbarSkill(slot);
	}
}
