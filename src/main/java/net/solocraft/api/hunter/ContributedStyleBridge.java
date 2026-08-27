package net.solocraft.api.hunter;

import java.util.ArrayList;
import java.util.List;

import net.solocraft.util.ClassStyleRules;
import net.solocraft.util.HunterEvaluationRules;

/**
 * Lets a contributed class offer styles the way a built-in one does.
 *
 * <p>The evaluator asks {@link ClassStyleRules} about styles using a numeric
 * class id, because that is how class identity worked before any of this. A
 * contributed class has no number of its own -- it borrows a positional id above
 * the built-in range -- so this translates one to the other and hands back
 * {@code Style} records the evaluator already knows how to draw, name, colour,
 * and commit.
 *
 * <p>Installed rather than called directly so {@code ClassStyleRules} keeps
 * class-loading without Minecraft, which is what lets the style economy be
 * tested without a game.
 *
 * <p>Built-in classes are answered from the shipped table before this is ever
 * consulted, so nothing here can restyle a shipped class.
 */
public final class ContributedStyleBridge {
	private ContributedStyleBridge() {
	}

	/** Called once during mod construction. */
	public static void install() {
		ClassStyleRules.installContributedSource(ContributedStyleBridge::stylesFor);
	}

	/**
	 * The styles a contributed class declares, numbered for the evaluator.
	 *
	 * <p>Style ids are positional, assigned from the registry's own stable
	 * ordering, exactly as class ids are. What persists is the style's key, so
	 * the number never reaches save data and a later registration cannot
	 * silently change which style a player already took.
	 */
	private static List<ClassStyleRules.Style> stylesFor(int classId) {
		HunterClass contributed = HunterEvaluationRules.contributedClass(classId).orElse(null);
		if (contributed == null)
			return List.of();

		List<HunterStyle> declared = HunterStyleRegistry.forClass(contributed.id());
		if (declared.isEmpty())
			return List.of();

		List<ClassStyleRules.Style> styles = new ArrayList<>();
		for (int i = 0; i < declared.size() && i < MAX_STYLES; i++) {
			HunterStyle style = declared.get(i);
			styles.add(new ClassStyleRules.Style(classId, i + 1,
					HunterStyle.normalise(style.storedKey()),
					style.displayName(), style.description(), style.accentColor(), true));
		}
		return styles;
	}

	/**
	 * The evaluator tracks remaining styles in an int bitmask, so a class cannot
	 * offer more than the mask has bits. Anything past this is dropped rather
	 * than wrapping around onto a style already in the bag.
	 */
	private static final int MAX_STYLES = 32;
}
