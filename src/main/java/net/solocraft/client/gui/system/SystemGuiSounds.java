package net.solocraft.client.gui.system;

import net.solocraft.init.SololevelingModSounds;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/**
 * Soft System GUI enter/exit sounds shared by the reworked System screens.
 * Internal page swaps stay quiet; only entering from gameplay and leaving back
 * to gameplay should make noise.
 */
public final class SystemGuiSounds {
	private static final float OPEN_VOLUME = 0.5f;
	private static final float CLOSE_VOLUME = 0.5f;
	private static final float NOTIFICATION_VOLUME = 0.5f;
	private static final float NEGATIVE_VOLUME = 0.62f;
	private static final float PITCH = 1.0f;
	private static final long COOLDOWN_MS = 90L;

	private static boolean active;
	private static long lastSoundMs;

	private SystemGuiSounds() {
	}

	public static void enter() {
		if (active)
			return;
		active = true;
		play(SololevelingModSounds.PANELOPEN.get(), PITCH, OPEN_VOLUME);
	}

	public static void exit() {
		if (!active)
			return;
		active = false;
		play(SololevelingModSounds.PANELCLOSE.get(), PITCH, CLOSE_VOLUME);
	}

	public static void switchInsideSystem() {
		active = true;
	}

	public static void notification() {
		play(SololevelingModSounds.PANELOPEN.get(), PITCH, NOTIFICATION_VOLUME);
	}

	public static void negativeNotification() {
		play(SololevelingModSounds.SYSTEM_NEGATIVE.get(), PITCH, NEGATIVE_VOLUME);
	}

	private static void play(SoundEvent sound, float pitch, float volume) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.getSoundManager() == null)
			return;
		long now = Util.getMillis();
		if (now - lastSoundMs < COOLDOWN_MS)
			return;
		lastSoundMs = now;
		mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}
}
