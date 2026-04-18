package com.ccmm.screentime.client.config;

import com.ccmm.screentime.client.security.PasswordHash;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Objects;

public final class ScreenTimeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	// If <= 0, no limit is enforced (used when separateLimitsForSpMp is false).
	public int limitMinutes;

	/**
	 * When true, {@link #limitMinutesSingleplayer} applies in singleplayer and
	 * {@link #limitMinutesMultiplayer} applies on multiplayer; {@link #limitMinutes} is ignored for enforcement.
	 */
	public boolean separateLimitsForSpMp;

	/** Minutes cap for singleplayer / integrated server / LAN host when separate limits are enabled. */
	public int limitMinutesSingleplayer;

	/** Minutes cap for multiplayer (remote server, etc.) when separate limits are enabled. */
	public int limitMinutesMultiplayer;

	// Playtime tracking (resets daily by local date).
	public long usedMsToday;
	public String lastResetDate; // ISO local date (yyyy-MM-dd)

	// Bypass state (valid only for current day).
	public long bypassEpochDay = -1;

	// Warning toasts (bitmask) for current day: 1=10m, 2=5m, 4=1m.
	public int warnedMaskToday = 0;

	// Password: stored as PBKDF2 hash, never plaintext.
	public PasswordHash passwordHash;

	public static ScreenTimeConfig defaults() {
		ScreenTimeConfig c = new ScreenTimeConfig();
		c.limitMinutes = 0;
		c.separateLimitsForSpMp = false;
		c.limitMinutesSingleplayer = 0;
		c.limitMinutesMultiplayer = 0;
		c.usedMsToday = 0;
		c.lastResetDate = null;
		c.bypassEpochDay = -1;
		c.warnedMaskToday = 0;
		c.passwordHash = null;
		return c;
	}

	public ScreenTimeConfig sanitize() {
		if (limitMinutes < 0) limitMinutes = 0;
		if (limitMinutesSingleplayer < 0) limitMinutesSingleplayer = 0;
		if (limitMinutesMultiplayer < 0) limitMinutesMultiplayer = 0;
		if (usedMsToday < 0) usedMsToday = 0;
		if (bypassEpochDay < -1) bypassEpochDay = -1;
		if (warnedMaskToday < 0) warnedMaskToday = 0;
		if (passwordHash != null && !passwordHash.isSane()) passwordHash = null;
		if (lastResetDate != null) lastResetDate = lastResetDate.trim();
		if (lastResetDate != null && lastResetDate.isEmpty()) lastResetDate = null;
		return this;
	}

	public boolean hasPassword() {
		return passwordHash != null;
	}

	public boolean verifyPassword(String candidate) {
		if (passwordHash == null) return false;
		return passwordHash.verify(candidate == null ? "" : candidate);
	}

	public void setPassword(String newPassword) {
		Objects.requireNonNull(newPassword, "newPassword");
		this.passwordHash = PasswordHash.create(newPassword);
	}

	public String toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("limitMinutes", limitMinutes);
		root.addProperty("separateLimitsForSpMp", separateLimitsForSpMp);
		root.addProperty("limitMinutesSingleplayer", limitMinutesSingleplayer);
		root.addProperty("limitMinutesMultiplayer", limitMinutesMultiplayer);
		root.addProperty("usedMsToday", usedMsToday);
		root.addProperty("lastResetDate", lastResetDate);
		root.addProperty("bypassEpochDay", bypassEpochDay);
		root.addProperty("warnedMaskToday", warnedMaskToday);
		if (passwordHash != null) {
			root.add("passwordHash", passwordHash.toJsonObject());
		}
		return GSON.toJson(root);
	}

	public static ScreenTimeConfig fromJson(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		ScreenTimeConfig c = new ScreenTimeConfig();
		if (root.has("limitMinutes")) c.limitMinutes = root.get("limitMinutes").getAsInt();
		if (root.has("separateLimitsForSpMp")) c.separateLimitsForSpMp = root.get("separateLimitsForSpMp").getAsBoolean();
		if (root.has("limitMinutesSingleplayer")) c.limitMinutesSingleplayer = root.get("limitMinutesSingleplayer").getAsInt();
		if (root.has("limitMinutesMultiplayer")) c.limitMinutesMultiplayer = root.get("limitMinutesMultiplayer").getAsInt();
		if (root.has("usedMsToday")) c.usedMsToday = root.get("usedMsToday").getAsLong();
		if (root.has("lastResetDate") && !root.get("lastResetDate").isJsonNull()) c.lastResetDate = root.get("lastResetDate").getAsString();
		if (root.has("bypassEpochDay")) c.bypassEpochDay = root.get("bypassEpochDay").getAsLong();
		if (root.has("warnedMaskToday")) c.warnedMaskToday = root.get("warnedMaskToday").getAsInt();
		if (root.has("passwordHash") && root.get("passwordHash").isJsonObject()) {
			c.passwordHash = PasswordHash.fromJsonObject(root.getAsJsonObject("passwordHash"));
		}
		return c.sanitize();
	}
}

