package com.ccmm.screentime.client.screen;

import com.ccmm.screentime.client.ScreenTimeLimiter;
import com.ccmm.screentime.client.ScreenTimeSaveLogic;
import com.ccmm.screentime.client.config.ScreenTimeConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.util.Optional;

public final class ScreenTimeSettingsScreen extends Screen {
	private final Screen parent;

	private boolean separateLimitsUi;

	private EditBox limitUnified;
	private EditBox limitSingleplayer;
	private EditBox limitMultiplayer;
	private EditBox password;
	private EditBox passwordConfirm;

	private Button separateToggleButton;
	private Button saveButton;
	private Button backButton;

	private Component status = Component.empty();

	private static final int TITLE_TO_USAGE_GAP = 14;
	private static final int USAGE_LINE_GAP = 12;
	private static final int SECTION_GAP = 10;
	private static final int LABEL_TO_FIELD = 12;
	private static final int FIELD_HEIGHT = 20;
	private static final int BTN_VERTICAL_GAP = 6;
	private static final int LINE_TEXT = 11;

	private static final int LAYOUT_MIN_TOP = 6;
	private static final int LAYOUT_BOTTOM_MARGIN = 12;

	private int renderTitleY;
	private int renderUsageY;
	private int renderHintY;
	private int hintTextLeft;
	private int renderUnifiedLimitLabelY;
	private int renderSpLimitLabelY;
	private int renderMpLimitLabelY;
	private int renderPwdPromptY;
	private int renderPwdConfirmPromptY;

	public ScreenTimeSettingsScreen(Screen parent) {
		super(Component.translatable("com.ccmm.screentime.settings.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ScreenTimeConfig cfg = ScreenTimeLimiter.getConfig();
		this.separateLimitsUi = cfg.separateLimitsForSpMp;

		int cx = this.width / 2;

		this.separateToggleButton = Button.builder(separateToggleLabel(), b -> {
			separateLimitsUi = !separateLimitsUi;
			b.setMessage(separateToggleLabel());
			updateFieldVisibility();
		}).bounds(cx - 100, 0, 200, FIELD_HEIGHT).build();
		this.addRenderableWidget(separateToggleButton);

		this.limitUnified = new EditBox(this.minecraft.font, cx - 100, 0, 200, FIELD_HEIGHT, Component.translatable("com.ccmm.screentime.settings.daily_limit_box"));
		this.limitUnified.setMaxLength(9);
		this.limitUnified.setValue(Integer.toString(Math.max(cfg.limitMinutes, 0)));
		this.addWidget(this.limitUnified);

		this.limitSingleplayer = new EditBox(this.minecraft.font, cx - 100, 0, 200, FIELD_HEIGHT, Component.translatable("com.ccmm.screentime.settings.limit_sp_box"));
		this.limitSingleplayer.setMaxLength(9);
		this.limitSingleplayer.setValue(Integer.toString(Math.max(cfg.limitMinutesSingleplayer, 0)));
		this.addWidget(this.limitSingleplayer);

		this.limitMultiplayer = new EditBox(this.minecraft.font, cx - 100, 0, 200, FIELD_HEIGHT, Component.translatable("com.ccmm.screentime.settings.limit_mp_box"));
		this.limitMultiplayer.setMaxLength(9);
		this.limitMultiplayer.setValue(Integer.toString(Math.max(cfg.limitMinutesMultiplayer, 0)));
		this.addWidget(this.limitMultiplayer);

		if (cfg.hasPassword()) {
			this.password = new EditBox(this.minecraft.font, cx - 100, 0, 200, FIELD_HEIGHT, Component.translatable("com.ccmm.screentime.settings.password_box"));
			this.password.setMaxLength(256);
			this.addWidget(this.password);

			this.passwordConfirm = null;
		} else {
			this.password = new EditBox(this.font, cx - 100, 0, 200, FIELD_HEIGHT, Component.translatable("com.ccmm.screentime.settings.set_password_box"));
			this.password.setMaxLength(256);
			this.addWidget(this.password);

			this.passwordConfirm = new EditBox(this.font, cx - 100, 0, 200, FIELD_HEIGHT, Component.translatable("com.ccmm.screentime.settings.confirm_password_box"));
			this.passwordConfirm.setMaxLength(256);
			this.addWidget(this.passwordConfirm);
		}

		this.saveButton = Button.builder(Component.translatable("com.ccmm.screentime.settings.save"), b -> onSave()).bounds(cx - 100, 0, 200, FIELD_HEIGHT).build();
		this.addRenderableWidget(this.saveButton);

		this.backButton = Button.builder(Component.translatable("com.ccmm.screentime.settings.back"), b -> this.minecraft.setScreen(parent)).bounds(cx - 100, 0, 200, FIELD_HEIGHT).build();
		this.addRenderableWidget(this.backButton);

		updateFieldVisibility();
		layoutWidgets();
	}

	private int computeContentHeight(boolean sep, boolean hasPwd, int sectionGap, int titleToUsageGap, int usageLineGap) {
		int h = titleToUsageGap + usageLineGap + sectionGap;
		h += LINE_TEXT + LABEL_TO_FIELD + FIELD_HEIGHT + sectionGap;
		if (sep) {
			h += LABEL_TO_FIELD + FIELD_HEIGHT + sectionGap + LABEL_TO_FIELD + FIELD_HEIGHT + sectionGap;
		} else {
			h += LABEL_TO_FIELD + FIELD_HEIGHT + sectionGap;
		}
		h += LABEL_TO_FIELD + FIELD_HEIGHT + sectionGap;
		if (!hasPwd) {
			h += LABEL_TO_FIELD + FIELD_HEIGHT + sectionGap;
		}
		h += FIELD_HEIGHT + BTN_VERTICAL_GAP + FIELD_HEIGHT;
		return h;
	}

	private record VerticalTuning(int sectionGap, int titleToUsageGap, int usageLineGap) {}

	private VerticalTuning pickVerticalTuning(boolean sep, boolean hasPwd) {
		int maxTotal = Math.max(0, this.height - LAYOUT_MIN_TOP - LAYOUT_BOTTOM_MARGIN);
		int sec = SECTION_GAP;
		int tit = TITLE_TO_USAGE_GAP;
		int use = USAGE_LINE_GAP;
		for (int i = 0; i < 64; i++) {
			if (computeContentHeight(sep, hasPwd, sec, tit, use) <= maxTotal) {
				return new VerticalTuning(sec, tit, use);
			}
			if (sec > 2) {
				sec--;
			} else if (tit > 10) {
				tit--;
			} else if (use > 8) {
				use--;
			} else {
				break;
			}
		}
		return new VerticalTuning(sec, tit, use);
	}

	private int clampedStackTop(int totalHeight) {
		int pinBottomTop = this.height - LAYOUT_BOTTOM_MARGIN - totalHeight;
		int idealTop = (this.height - totalHeight) / 2;
		if (pinBottomTop >= LAYOUT_MIN_TOP) {
			return Mth.clamp(idealTop, LAYOUT_MIN_TOP, pinBottomTop);
		}
		return pinBottomTop;
	}

	private void layoutWidgets() {
		ScreenTimeConfig cfg = ScreenTimeLimiter.getConfig();
		boolean sep = separateLimitsUi;
		boolean hasPwd = cfg.hasPassword();

		final int cx = this.width / 2;
		final int boxW = 200;
		final int boxLeft = cx - boxW / 2;
		final int toggleW = Math.min(280, Math.max(boxW, this.width - 48));
		this.hintTextLeft = cx - toggleW / 2;

		VerticalTuning tune = pickVerticalTuning(sep, hasPwd);
		int sectionGap = tune.sectionGap();
		int titleToUsageGap = tune.titleToUsageGap();
		int usageLineGap = tune.usageLineGap();

		int totalHeight = computeContentHeight(sep, hasPwd, sectionGap, titleToUsageGap, usageLineGap);
		int top = clampedStackTop(totalHeight);

		int y = top;

		this.renderTitleY = y;
		y += titleToUsageGap;
		this.renderUsageY = y;
		y += usageLineGap + sectionGap;

		this.renderHintY = y;
		int toggleTop = y + LINE_TEXT + LABEL_TO_FIELD;
		this.separateToggleButton.setRectangle(toggleW, FIELD_HEIGHT, cx - toggleW / 2, toggleTop);
		y = toggleTop + FIELD_HEIGHT + sectionGap;

		if (!sep) {
			this.renderUnifiedLimitLabelY = y;
			int unifiedTop = y + LABEL_TO_FIELD;
			this.limitUnified.setRectangle(boxW, FIELD_HEIGHT, boxLeft, unifiedTop);
			y = unifiedTop + FIELD_HEIGHT + sectionGap;
		} else {
			this.renderSpLimitLabelY = y;
			int spTop = y + LABEL_TO_FIELD;
			this.limitSingleplayer.setRectangle(boxW, FIELD_HEIGHT, boxLeft, spTop);
			y = spTop + FIELD_HEIGHT + sectionGap;

			this.renderMpLimitLabelY = y;
			int mpTop = y + LABEL_TO_FIELD;
			this.limitMultiplayer.setRectangle(boxW, FIELD_HEIGHT, boxLeft, mpTop);
			y = mpTop + FIELD_HEIGHT + sectionGap;
		}

		this.renderPwdPromptY = y;
		int pwdTop = y + LABEL_TO_FIELD;
		this.password.setRectangle(boxW, FIELD_HEIGHT, boxLeft, pwdTop);
		y = pwdTop + FIELD_HEIGHT + sectionGap;

		if (!hasPwd) {
			this.renderPwdConfirmPromptY = y;
			int confirmTop = y + LABEL_TO_FIELD;
			this.passwordConfirm.setRectangle(boxW, FIELD_HEIGHT, boxLeft, confirmTop);
			y = confirmTop + FIELD_HEIGHT + sectionGap;
		}

		this.saveButton.setRectangle(boxW, FIELD_HEIGHT, boxLeft, y);
		y += FIELD_HEIGHT + BTN_VERTICAL_GAP;
		this.backButton.setRectangle(boxW, FIELD_HEIGHT, boxLeft, y);
	}

	private Component separateToggleLabel() {
		return separateLimitsUi
			? Component.translatable("com.ccmm.screentime.settings.separate_toggle_on")
			: Component.translatable("com.ccmm.screentime.settings.separate_toggle_off");
	}

	private void updateFieldVisibility() {
		boolean sep = separateLimitsUi;
		this.limitUnified.setVisible(!sep);
		this.limitSingleplayer.setVisible(sep);
		this.limitMultiplayer.setVisible(sep);
		this.separateToggleButton.setMessage(separateToggleLabel());
		layoutWidgets();
	}

	private void onSave() {
		ScreenTimeConfig cfg = ScreenTimeLimiter.getConfig();

		int unified;
		int sp;
		int mp;
		try {
			unified = Integer.parseInt(limitUnified.getValue().trim());
			sp = Integer.parseInt(limitSingleplayer.getValue().trim());
			mp = Integer.parseInt(limitMultiplayer.getValue().trim());
			unified = Math.max(0, unified);
			sp = Math.max(0, sp);
			mp = Math.max(0, mp);
		} catch (NumberFormatException e) {
			status = Component.translatable("com.ccmm.screentime.settings.limit_error");
			return;
		}

		String passwordCurrent = cfg.hasPassword() ? password.getValue() : "";
		String passwordNew = cfg.hasPassword() ? "" : password.getValue();
		String passwordConfirmStr = cfg.hasPassword() || passwordConfirm == null ? "" : passwordConfirm.getValue();

		Optional<Component> err = ScreenTimeSaveLogic.apply(
			cfg,
			separateLimitsUi,
			unified,
			sp,
			mp,
			passwordCurrent,
			passwordNew,
			passwordConfirmStr
		);

		if (err.isPresent()) {
			status = err.get();
			return;
		}

		this.minecraft.setScreen(parent);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.centeredText(minecraft.font, this.title, this.width / 2, this.renderTitleY, ARGB.opaque(0xFFFFFF));
		ScreenTimeConfig cfg = ScreenTimeLimiter.getConfig();
		String used = formatDuration(cfg.usedMsToday);

		if (separateLimitsUi) {
			context.centeredText(minecraft.font, Component.translatable("com.ccmm.screentime.settings.usage_dual",
				used,
				Math.max(0, cfg.limitMinutesSingleplayer),
				Math.max(0, cfg.limitMinutesMultiplayer)
			), this.width / 2, this.renderUsageY, ARGB.opaque(0xAAAAAA));
		} else {
			int lim = Math.max(0, cfg.limitMinutes);
			context.centeredText(minecraft.font, Component.translatable(lim <= 0 ? "com.ccmm.screentime.settings.limit_disabled" : "com.ccmm.screentime.settings.limit", used, lim), this.width / 2, this.renderUsageY, ARGB.opaque(0xAAAAAA));
		}

		int cx = this.width / 2;

		context.text(this.font, Component.translatable("com.ccmm.screentime.settings.separate_hint"), this.hintTextLeft, this.renderHintY, ARGB.opaque(0xCCCCCC));

		if (limitUnified.visible) {
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.daily_limit"), cx - 100, this.renderUnifiedLimitLabelY, ARGB.opaque(0xEEEEEE));
			this.limitUnified.extractWidgetRenderState(context, mouseX, mouseY, delta);
		}
		if (limitSingleplayer.visible) {
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.limit_sp_label"), cx - 100, this.renderSpLimitLabelY, ARGB.opaque(0xEEEEEE));
			this.limitSingleplayer.extractWidgetRenderState(context, mouseX, mouseY, delta);
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.limit_mp_label"), cx - 100, this.renderMpLimitLabelY, ARGB.opaque(0xEEEEEE));
			this.limitMultiplayer.extractWidgetRenderState(context, mouseX, mouseY, delta);
		}

		if (cfg.hasPassword()) {
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.enter_password"), cx - 100, this.renderPwdPromptY, ARGB.opaque(0xEEEEEE));
			this.password.extractWidgetRenderState(context, mouseX, mouseY, delta);
		} else {
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.set_password"), cx - 100, this.renderPwdPromptY, ARGB.opaque(0xEEEEEE));
			this.password.extractWidgetRenderState(context, mouseX, mouseY, delta);
			context.text(this.font, Component.translatable("com.ccmm.screentime.settings.confirm_password"), cx - 100, this.renderPwdConfirmPromptY, ARGB.opaque(0xEEEEEE));
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
