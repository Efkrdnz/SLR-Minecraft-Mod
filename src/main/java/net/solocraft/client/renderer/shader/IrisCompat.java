package net.solocraft.client.renderer.shader;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import java.lang.reflect.Method;

/** Optional Iris/Oculus access without a required runtime dependency. */
public final class IrisCompat {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static volatile boolean initialized;
	private static boolean available;
	private static Object irisApi;
	private static Method isShaderPackInUseMethod;
	private static Method isRenderingShadowPassMethod;

	private IrisCompat() {
	}

	/** True only while Iris/Oculus is actively rendering a shader pack. */
	public static boolean isShaderPackInUse() {
		initialize();
		return invokeBoolean(isShaderPackInUseMethod);
	}

	/** True while the optional shader mod is rendering its shadow camera. */
	public static boolean isRenderingShadowPass() {
		initialize();
		return invokeBoolean(isRenderingShadowPassMethod);
	}

	public static boolean isAvailable() {
		initialize();
		return available;
	}

	private static boolean invokeBoolean(Method method) {
		if (!available || method == null)
			return false;
		try {
			return Boolean.TRUE.equals(method.invoke(irisApi));
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return false;
		}
	}

	private static synchronized void initialize() {
		if (initialized)
			return;
		initialized = true;
		for (String className : new String[] {
				"net.irisshaders.iris.api.v0.IrisApi",
				"net.coderbot.iris.api.v0.IrisApi"
		}) {
			try {
				Class<?> apiClass = Class.forName(className);
				irisApi = apiClass.getMethod("getInstance").invoke(null);
				isShaderPackInUseMethod = apiClass.getMethod("isShaderPackInUse");
				isRenderingShadowPassMethod = apiClass.getMethod("isRenderingShadowPass");
				available = true;
				LOGGER.info("[SoloLeveling] Iris/Oculus detected ({}); world quad shaders will use deferred compatibility when a pack is active.", className);
				return;
			} catch (ReflectiveOperationException | LinkageError ignored) {
				irisApi = null;
				isShaderPackInUseMethod = null;
				isRenderingShadowPassMethod = null;
			}
		}
		available = false;
		LOGGER.info("[SoloLeveling] Iris/Oculus not detected; world quad shaders will use the normal render path.");
	}
}
