package net.solocraft.network.compat;

import java.util.function.Supplier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Small replacement for the removed legacy DistExecutor helper. */
public final class DistExecutor {
	private DistExecutor() {
	}

	public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> action) {
		if (FMLEnvironment.dist == dist) {
			action.get().run();
		}
	}
}
