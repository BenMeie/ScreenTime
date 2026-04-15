package com.ccmm.screentime.client;

import com.ccmm.screentime.ScreenTime;
import com.ccmm.screentime.client.screen.ScreenTimeSettingsScreen;
import com.ccmm.screentime.client.screen.ScreenTimeTitleButtonInjector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ScreenTimeClient implements ClientModInitializer {
	private static KeyMapping OPEN_SETTINGS;

	@Override
	public void onInitializeClient() {
		ScreenTimeLimiter.init();

		KeyMapping.Category CATEGORY = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(ScreenTime.MOD_ID, "screentime")
		);

		OPEN_SETTINGS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.screentime.open_settings",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_P,
			CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ScreenTimeLimiter.onClientTick(client);

			while (OPEN_SETTINGS.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new ScreenTimeSettingsScreen(null));
				} else {
					client.setScreen(new ScreenTimeSettingsScreen(client.screen));
				}
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof TitleScreen) {
				ScreenTimeLimiter.ensureNotExpiredScreen(client);
				ScreenTimeTitleButtonInjector.inject(screen);
			}
		});
	}
}