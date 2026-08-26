package net.solocraft.api;

/**
 * How a skill cast reached the game.
 *
 * <p>Presentation sometimes has to differ even when the mechanics do not. Arise
 * is the clearest case: cast from the keybind, the System speaks the word for the
 * player and the effect is held back briefly so the shadows rise on that shout.
 * Cast by voice, the player has already said it out loud -- replaying a recording
 * would talk over them, and the delay would open a gap between their word and the
 * result.
 *
 * <p>This is a presentation hint only. Every gameplay check -- class, resources,
 * cooldown, ownership -- is identical whatever the source, so nothing can be
 * gained by claiming a different one.
 */
public enum CastSource {
	/** A keybind, GUI button, or command. */
	MANUAL,

	/** The player spoke the incantation and an addon recognised it locally. */
	VOICE;

	/** True when the player already performed the incantation themselves. */
	public boolean isSpokenAloud() {
		return this == VOICE;
	}
}
