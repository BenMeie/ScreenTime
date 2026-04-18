package com.ccmm.screentime.client;

import com.ccmm.screentime.ScreenTime;
import com.ccmm.screentime.client.config.ScreenTimeConfig;
import com.ccmm.screentime.client.screen.BypassPasswordScreen;
import com.ccmm.screentime.client.screen.LimitReachedScreen;
import com.ccmm.screentime.client.util.ClientDisconnectUtil;
import com.ccmm.screentime.client.util.ToastUtil;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public final class ScreenTimeLimiter {
	private static final String CONFIG_FILE = "screentime.json";

	private static ScreenTimeConfig config;
	private static long lastTickMs = -1;
	private static long lastPersistedUsedMs = -1;
	private static long lastPersistWallMs = 0;

	private ScreenTimeLimiter() {}

	public static void init() {
		config = loadConfig();
		ensureDailyReset();
		persistIfNeeded(true);
	}

	public static ScreenTimeConfig getConfig() {
		if (config == null) {
			init();
		}
		return config;
	}

	public static void onClientTick(Minecraft client) {
		if (config == null) {
			init();
		}

		ensureDailyReset();

		if (isExpiredToday()) {
			enforceLockout(client);
			return;
		}

		boolean countTime = client.level != null && !client.isPaused();
		long now = System.currentTimeMillis();

		if (!countTime) {
			lastTickMs = -1;
			persistIfNeeded(false);
			return;
		}

		if (lastTickMs < 0) {
			lastTickMs = now;
			return;
		}

		long delta = now - lastTickMs;
		lastTickMs = now;
		if (delta <= 0) {
			return;
		}

		config.usedMsToday = safeAdd(config.usedMsToday, delta);
		persistIfNeeded(false);

		maybeToastWarnings(client);

		if (isExpiredToday()) {
			enforceLockout(client);
		}
	}

	public static void ensureNotExpiredScreen(Minecraft client) {
		if (config == null) {
			init();
		}
		ensureDailyReset();
		if (isExpiredToday()) {
			enforceLockout(client);
		}
	}

	public static boolean isExpiredToday() {
		long limitMs = config.limitMinutes <= 0 ? 0 : config.limitMinutes * 60_000L;
		if (limitMs <= 0) return false;
		if (isBypassedToday()) return false;
		return config.usedMsToday >= limitMs;
	}

	public static boolean isBypassedToday() {
		long today = LocalDate.now().toEpochDay();
		return config.bypassEpochDay == today;
	}

	public static void setBypassedToday(boolean bypassed) {
		config.bypassEpochDay = bypassed ? LocalDate.now().toEpochDay() : -1;
		persistIfNeeded(true);
	}

	public static void saveNow() {
		persistIfNeeded(true);
	}

	public static void enforceLockout(Minecraft client) {
		if (client == null) return;
		if (client.screen instanceof LimitReachedScreen) return;
		if (client.screen instanceof BypassPasswordScreen) return;

		LimitReachedScreen lockScreen = new LimitReachedScreen(client.screen);

		// Prefer a clean disconnect so singleplayer saves and server stops.
		if (client.level != null) {
			if (ClientDisconnectUtil.disconnectToScreen(client, lockScreen)) {
				return;
			}
		}

		client.setScreen(lockScreen);
	}

	public static void ensureDailyReset() {
		LocalDate today = LocalDate.now();
		LocalDate last = config.lastResetDate == null ? null : LocalDate.parse(config.lastResetDate);
		if (last == null || !last.equals(today)) {
			config.lastResetDate = today.toString();
			config.usedMsToday = 0;
			config.bypassEpochDay = -1;
			config.warnedMaskToday = 0;
			lastTickMs = -1;
		}
	}

	private static void maybeToastWarnings(Minecraft client) {
		long limitMs = config.limitMinutes <= 0 ? 0 : config.limitMinutes * 60_000L;
		if (limitMs <= 0) return;
		if (isBypassedToday()) return;
		if (client == null) return;

		long remainingMs = limitMs - config.usedMsToday;
		if (remainingMs <= 0) return;

		toastIfCrossed(client, remainingMs, 10 * 60_000L, 1, 10);
		toastIfCrossed(client, remainingMs, 5 * 60_000L, 2, 5);
		toastIfCrossed(client, remainingMs, 1 * 60_000L, 4, 1);
	}

	private static void toastIfCrossed(Minecraft client, long remainingMs, long thresholdMs, int maskBit, int minutesRemaining) {
		if ((config.warnedMaskToday & maskBit) != 0) return;
		if (remainingMs > thresholdMs) return;

		config.warnedMaskToday |= maskBit;
		persistIfNeeded(false);

		ToastUtil.show(
			client,
			Component.translatable("com.ccmm.screentime.toast.title"),
			Component.translatable("com.ccmm.screentime.toast.remaining_minutes", minutesRemaining)
		);
	}

	private static void persistIfNeeded(boolean force) {
		long now = System.currentTimeMillis();
		if (!force) {
			// Avoid hammering disk; save at most every ~10s or on meaningful changes.
			if (now - lastPersistWallMs < 10_000 && lastPersistedUsedMs == config.usedMsToday) {
				return;
			}
		}

		saveConfig(config);
		lastPersistWallMs = now;
		lastPersistedUsedMs = config.usedMsToday;
	}

	private static ScreenTimeConfig loadConfig() {
		Path path = configPath();
		if (!Files.exists(path)) {
			return ScreenTimeConfig.defaults();
		}

		try {
			String json = Files.readString(path);
			ScreenTimeConfig cfg = ScreenTimeConfig.fromJson(json);
			return cfg == null ? ScreenTimeConfig.defaults() : cfg.sanitize();
		} catch (IOException | JsonParseException e) {
			ScreenTime.LOGGER.warn("Failed to read config; using defaults", e);
			return ScreenTimeConfig.defaults();
		}
	}

	private static void saveConfig(ScreenTimeConfig cfg) {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, cfg.toJson());
		} catch (IOException e) {
			ScreenTime.LOGGER.warn("Failed to write config", e);
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
	}

	private static long safeAdd(long a, long b) {
		long r = a + b;
		if (((a ^ r) & (b ^ r)) < 0) {
			return Long.MAX_VALUE;
		}
		return r;
	}
}

