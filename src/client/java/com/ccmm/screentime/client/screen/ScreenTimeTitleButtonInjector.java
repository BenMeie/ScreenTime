package com.ccmm.screentime.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

public final class ScreenTimeTitleButtonInjector {
	private static final Component BUTTON_TEXT = Component.translatable("com.ccmm.screentime.title.button");

	private ScreenTimeTitleButtonInjector() {}

	public static void inject(Screen screen) {
		for (GuiEventListener child : screen.children()) {
			if (child instanceof Button bw) {
				if (BUTTON_TEXT.equals(bw.getMessage())) {
					return;
				}
			}
		}

		int x = screen.width / 2 - 100;
		int y = screen.height / 4 + 48 + 72 + 12; // roughly below vanilla buttons
		Button btn = Button.builder(BUTTON_TEXT, b -> {
			Minecraft client = Minecraft.getInstance();
			client.setScreen(new ScreenTimeSettingsScreen(screen));
		}).bounds(x, y, 200, 20).build();

		addWidget(screen, btn);
	}

	/**
	 * Screen#addRenderableWidget is protected in Mojmap; add via reflection.
	 */
	private static void addWidget(Screen screen, AbstractWidget widget) {
		try {
			Method m = Screen.class.getDeclaredMethod("addRenderableWidget", AbstractWidget.class);
			m.setAccessible(true);
			m.invoke(screen, widget);
			return;
		} catch (Throwable ignored) {
			// fall through
		}

		try {
			Method m = Screen.class.getDeclaredMethod("addWidget", GuiEventListener.class);
			m.setAccessible(true);
			m.invoke(screen, widget);
		} catch (Throwable ignored) {
			// If this fails too, the button just won't appear.
		}
	}
}

