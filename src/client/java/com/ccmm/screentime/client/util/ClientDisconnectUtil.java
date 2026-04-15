package com.ccmm.screentime.client.util;

import com.ccmm.screentime.ScreenTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ClientDisconnectUtil {
	private ClientDisconnectUtil() {}

	/**
	 * Best-effort world disconnect that should trigger integrated-server save/stop on singleplayer.
	 * Uses reflection to tolerate minor signature differences across Minecraft versions/mappings.
	 */
	public static boolean disconnectToScreen(Minecraft client, Screen nextScreen) {
		if (client == null) return false;
		try {
			var m = Minecraft.class.getMethod("disconnect", Screen.class);
			m.invoke(client, nextScreen);
			return true;
		} catch (NoSuchMethodException ignored) {
			// fall through
		} catch (Throwable t) {
			ScreenTime.LOGGER.warn("disconnect(Screen) invocation failed", t);
			return false;
		}

		try {
			var m = Minecraft.class.getMethod("disconnect");
			m.invoke(client);
			client.setScreen(nextScreen);
			return true;
		} catch (NoSuchMethodException ignored) {
			// fall through
		} catch (Throwable t) {
			ScreenTime.LOGGER.warn("disconnect() invocation failed", t);
			return false;
		}

		// Last resort: just show the lock screen.
		client.setScreen(nextScreen);
		return true;
	}
}

