package com.ccmm.screentime.client.screen;

import com.ccmm.screentime.client.ScreenTimeLimiter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class LimitReachedScreen extends Screen {
	private final Screen parent;

	public LimitReachedScreen(Screen parent) {
		super(Component.translatable("com.ccmm.screentime.lock.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 2 + 16;

		this.addRenderableWidget(Button.builder(
			Component.translatable("com.ccmm.screentime.lock.bypass"),
			b -> this.minecraft.setScreen(new BypassPasswordScreen(this))
		).bounds(cx - 100, y, 200, 20).build());

		this.addRenderableWidget(Button.builder(
			Component.translatable("com.ccmm.screentime.lock.quit"),
			b -> Minecraft.getInstance().stop()
		).bounds(cx - 100, y + 24, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 30, ARGB.opaque(0xFFFFFF));

		long limitMs = ScreenTimeLimiter.getConfig().limitMinutes <= 0 ? 0 : ScreenTimeLimiter.getConfig().limitMinutes * 60_000L;
		long usedMs = ScreenTimeLimiter.getConfig().usedMsToday;
		String used = formatDuration(usedMs);
		String limit = limitMs <= 0 ? Component.translatable("com.ccmm.screentime.lock.no_limit").getString() : formatDuration(limitMs);

		context.centeredText(this.font, Component.translatable("com.ccmm.screentime.lock.used_today", used, limit), this.width / 2, this.height / 2 - 14, ARGB.opaque(0xDDDDDD));
		context.centeredText(this.font, Component.translatable("com.ccmm.screentime.lock.resets_daily"), this.width / 2, this.height / 2 + 2, ARGB.opaque(0xAAAAAA));

		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void onClose() {
		// Don’t allow returning to the game while locked out.
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

