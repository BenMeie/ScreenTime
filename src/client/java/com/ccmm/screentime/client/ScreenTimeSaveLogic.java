package com.ccmm.screentime.client;

import com.ccmm.screentime.client.config.ScreenTimeConfig;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Shared validation for applying daily limits + password rules (custom UI or Fzzy Config UI).
 * One password protects all limit fields.
 */
public final class ScreenTimeSaveLogic {
	private ScreenTimeSaveLogic() {}

	private static boolean needsFirstTimePassword(boolean separateLimits, int unifiedMinutes, int spMinutes, int mpMinutes) {
		if (!separateLimits) {
			return unifiedMinutes > 0;
		}
		return spMinutes > 0 || mpMinutes > 0;
	}

	public static Optional<Component> apply(
		ScreenTimeConfig cfg,
		boolean separateLimits,
		int unifiedMinutes,
		int spMinutes,
		int mpMinutes,
		String passwordCurrent,
		String passwordNew,
		String passwordConfirm
	) {
		int u = Math.max(0, unifiedMinutes);
		int sp = Math.max(0, spMinutes);
		int mp = Math.max(0, mpMinutes);

		if (!cfg.hasPassword()) {
			if (!needsFirstTimePassword(separateLimits, u, sp, mp)) {
				applyStoredLimits(cfg, separateLimits, u, sp, mp);
				ScreenTimeLimiter.saveNow();
				return Optional.empty();
			}

			String p1 = passwordNew == null ? "" : passwordNew;
			String p2 = passwordConfirm == null ? "" : passwordConfirm;
			if (p1.isBlank()) {
				return Optional.of(Component.translatable("com.ccmm.screentime.settings.password_required"));
			}
			if (!p1.equals(p2)) {
				return Optional.of(Component.translatable("com.ccmm.screentime.settings.passwords_not_same"));
			}

			cfg.setPassword(p1);
			applyStoredLimits(cfg, separateLimits, u, sp, mp);
			ScreenTimeLimiter.saveNow();
			return Optional.empty();
		}

		String cur = passwordCurrent == null ? "" : passwordCurrent;
		if (!cfg.verifyPassword(cur)) {
			return Optional.of(Component.translatable("com.ccmm.screentime.settings.incorrect_password"));
		}

		applyStoredLimits(cfg, separateLimits, u, sp, mp);
		ScreenTimeLimiter.saveNow();
		return Optional.empty();
	}

	private static void applyStoredLimits(ScreenTimeConfig cfg, boolean separateLimits, int unifiedMinutes, int spMinutes, int mpMinutes) {
		cfg.separateLimitsForSpMp = separateLimits;
		if (separateLimits) {
			cfg.limitMinutesSingleplayer = spMinutes;
			cfg.limitMinutesMultiplayer = mpMinutes;
			cfg.limitMinutes = 0;
		} else {
			cfg.limitMinutes = unifiedMinutes;
			cfg.limitMinutesSingleplayer = 0;
			cfg.limitMinutesMultiplayer = 0;
		}
	}
}
