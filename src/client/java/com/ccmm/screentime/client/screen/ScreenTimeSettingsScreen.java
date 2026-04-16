package com.ccmm.screentime.client.screen;

import com.ccmm.screentime.client.ScreenTimeLimiter;
import com.ccmm.screentime.client.config.ScreenTimeConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class ScreenTimeSettingsScreen extends Screen {
	private final Screen parent;

	private EditBox limitMinutes;
	private EditBox password;
	private EditBox passwordConfirm;
	private Component status = Component.empty();

	public ScreenTimeSettingsScreen(Screen parent) {
		super(Component.translatable("com.ccmm.screentime.settings.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ScreenTimeConfig cfg = ScreenTimeLimiter.getConfig();

		int cx = this.width / 2;
		int y = this.height / 2 - 55;

		this.limitMinutes = new EditBox(minecraft.font, cx - 100, y, 200, 20, Component.translatable("com.ccmm.screentime.settings.daily_limit_box"));
		this.limitMinutes.setMaxLength(9);
		this.limitMinutes.setValue(Integer.toString(Math.max(cfg.limitMinutes, 0)));
		this.addWidget(this.limitMinutes);

		y += 35;

		if (cfg.hasPassword()) {
			this.password = new EditBox(minecraft.font, cx - 100, y, 200, 20, Component.translatable("com.ccmm.screentime.settings.password_box"));
			this.password.setMaxLength(256);
			this.addWidget(this.password);

			y += 25;
		} else {
			this.password = new EditBox(this.font, cx - 100, y, 200, 20, Component.translatable("com.ccmm.screentime.settings.set_password_box"));
			this.password.setMaxLength(256);
			this.addWidget(this.password);

			y += 35;

			this.passwordConfirm = new EditBox(this.font, cx - 100, y, 200, 20, Component.translatable("com.ccmm.screentime.settings.confirm_password_box"));
			this.passwordConfirm.setMaxLength(256);
			this.addWidget(this.passwordConfirm);

			y += 25;
		}

		this.addRenderableWidget(Button.builder(Component.translatable("com.ccmm.screentime.settings.save"), b -> onSave()).bounds(cx - 100, y, 200, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("com.ccmm.screentime.settings.back"), b -> this.minecraft.setScreen(parent)).bounds(cx - 100, y + 24, 200, 20).build());
	}

	private void onSave() {
		ScreenTimeConfig cfg = ScreenTimeLimiter.getConfig();

		int minutes;
		try {
			minutes = Integer.parseInt(limitMinutes.getValue().trim());
			if (minutes < 0) minutes = 0;
		} catch (NumberFormatException e) {
			status = Component.translatable("com.ccmm.screentime.settings.limit_error");
			return;
		}

		if (!cfg.hasPassword()) {
			String p1 = password.getValue();
			String p2 = passwordConfirm == null ? "" : passwordConfirm.getValue();
			if (p1 == null || p1.isBlank()) {
				status = Component.translatable("com.ccmm.screentime.settings.password_required");
				return;
			}
			if (!p1.equals(p2)) {
				status = Component.translatable("com.ccmm.screentime.settings.passwords_not_same");
				return;
			}

			cfg.setPassword(p1);
			cfg.limitMinutes = minutes;
			ScreenTimeLimiter.saveNow();
			this.minecraft.setScreen(parent);
			return;
		}

		if (!cfg.verifyPassword(password.getValue())) {
			status = Component.translatable("com.ccmm.screentime.settings.incorrect_password");
			return;
		}

		cfg.limitMinutes = minutes;
		ScreenTimeLimiter.saveNow();
		this.minecraft.setScreen(parent);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
//		this.extractBlurredBackground(context);
		context.centeredText(minecraft.font, this.title, this.width / 2, this.height / 2 - 90, ARGB.opaque(0xFFFFFF));
		ScreenTimeConfig cfg = ScreenTimeLimiter.getConfig();
		String used = formatDuration(cfg.usedMsToday);
		context.centeredText(minecraft.font, Component.translatable(cfg.limitMinutes <= 0 ? "com.ccmm.screentime.settings.limit_disabled" : "com.ccmm.screentime.settings.limit", used, cfg.limitMinutes), this.width / 2, this.height / 2 - 78, ARGB.opaque(0xAAAAAA));

		context.text(this.font, Component.translatable("com.ccmm.screentime.settings.daily_limit"), this.width / 2 - 100, this.height / 2 - 64, ARGB.opaque(0xEEEEEE));
		this.limitMinutes.extractWidgetRenderState(context, mouseX, mouseY, delta);

		int y = this.height / 2 - 13;
		if (cfg.hasPassword()) {
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.enter_password"), this.width / 2 - 100, y - 16, ARGB.opaque(0xEEEEEE));
			this.password.extractWidgetRenderState(context, mouseX, mouseY, delta);
		} else {
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.set_password"), this.width / 2 - 100, y - 16, ARGB.opaque(0xEEEEEE));
			this.password.extractWidgetRenderState(context, mouseX, mouseY, delta);
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.confirm_password"), this.width / 2 - 100, y + 19, ARGB.opaque(0xEEEEEE));
			this.passwordConfirm.extractWidgetRenderState(context, mouseX, mouseY, delta);
		}

		if (!status.getString().isEmpty()) {
			context.centeredText(this.font, status, this.width / 2, this.height - 30, ARGB.opaque(0xFF5555));
		}

		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	private static String formatDuration(long ms) {
		if (ms < 0) ms = 0;
		long totalSeconds = ms / 1000;
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		if (hours > 0) return hours + "h " + minutes + "m";
		if (minutes > 0) return minutes + "m " + seconds + "s";
		return seconds + "s";
	}
}

