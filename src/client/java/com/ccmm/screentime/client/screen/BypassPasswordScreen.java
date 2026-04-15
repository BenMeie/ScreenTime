package com.ccmm.screentime.client.screen;

import com.ccmm.screentime.client.ScreenTimeLimiter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class BypassPasswordScreen extends Screen {
	private final Screen parent;
	private EditBox password;
	private Component status = Component.empty();

	public BypassPasswordScreen(Screen parent) {
		super(Component.translatable("com.ccmm.screentime.bypass.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 2 - 10;

		this.password = new EditBox(this.font, cx - 100, y, 200, 20, Component.translatable("com.ccmm.screentime.bypass.password_box"));
		this.password.setMaxLength(256);
		this.setInitialFocus(this.password);
		this.addWidget(this.password);

		this.addRenderableWidget(Button.builder(
			Component.translatable("com.ccmm.screentime.bypass.bypass_today"),
			b -> tryBypass()
		).bounds(cx - 100, y + 28, 200, 20).build());

		this.addRenderableWidget(Button.builder(
			Component.translatable("com.ccmm.screentime.bypass.back"),
			b -> this.minecraft.setScreen(parent)
		).bounds(cx - 100, y + 52, 200, 20).build());
	}

	private void tryBypass() {
		var cfg = ScreenTimeLimiter.getConfig();
		if (!cfg.hasPassword()) {
			status = Component.translatable("com.ccmm.screentime.bypass.no_password_set");
			return;
		}
		if (!cfg.verifyPassword(password.getValue())) {
			status = Component.translatable("com.ccmm.screentime.bypass.incorrect_password");
			return;
		}

		ScreenTimeLimiter.setBypassedToday(true);
		status = Component.translatable("com.ccmm.screentime.bypass.enabled_today");
		this.minecraft.setScreen(new TitleScreen());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
		context.centeredText(this.font, Component.translatable("com.ccmm.screentime.bypass.instructions"), this.width / 2, this.height / 2 - 44, ARGB.opaque(0xAAAAAA));

		this.password.extractWidgetRenderState(context, mouseX, mouseY, delta);
		if (!status.getString().isEmpty()) {
			context.centeredText(this.font, status, this.width / 2, this.height / 2 + 80, ARGB.opaque(0xFF5555));
		}

		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}
}

