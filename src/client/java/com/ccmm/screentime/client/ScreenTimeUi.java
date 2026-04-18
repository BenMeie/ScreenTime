package com.ccmm.screentime.client;

import com.ccmm.screentime.client.screen.ScreenTimeSettingsScreen;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenTimeUi {
	private ScreenTimeUi() {}

	public static void openSettings(Screen parent) {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new ScreenTimeSettingsScreen(parent));
	}
}
