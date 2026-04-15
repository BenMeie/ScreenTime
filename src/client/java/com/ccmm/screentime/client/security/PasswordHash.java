package com.ccmm.screentime.client.security;

import com.google.gson.JsonObject;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHash {
	private static final SecureRandom RNG = new SecureRandom();

	// Reasonable defaults for client-side gating; can be increased later.
	private static final int ITERATIONS = 120_000;
	private static final int SALT_BYTES = 16;
	private static final int KEY_BYTES = 32;

	public String algorithm;   // e.g. "PBKDF2WithHmacSHA256"
	public int iterations;
	public String saltB64;
	public String hashB64;

	public static PasswordHash create(String password) {
		byte[] salt = new byte[SALT_BYTES];
		RNG.nextBytes(salt);

		PasswordHash ph = new PasswordHash();
		ph.algorithm = "PBKDF2WithHmacSHA256";
		ph.iterations = ITERATIONS;
		ph.saltB64 = Base64.getEncoder().encodeToString(salt);
		ph.hashB64 = Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
		return ph;
	}

	public boolean verify(String password) {
		try {
			byte[] salt = Base64.getDecoder().decode(saltB64);
			byte[] expected = Base64.getDecoder().decode(hashB64);
			byte[] actual = derive(password, salt, iterations);
			return constantTimeEquals(expected, actual);
		} catch (Throwable t) {
			return false;
		}
	}

	public boolean isSane() {
		return algorithm != null
			&& iterations > 0
			&& saltB64 != null
			&& hashB64 != null;
	}

	private static byte[] derive(String password, byte[] salt, int iterations) {
		try {
			PBEKeySpec spec = new PBEKeySpec(
				(password == null ? "" : password).toCharArray(),
				salt,
				iterations,
				KEY_BYTES * 8
			);
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			return skf.generateSecret(spec).getEncoded();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean constantTimeEquals(byte[] a, byte[] b) {
		if (a == null || b == null) return false;
		if (a.length != b.length) return false;
		int r = 0;
		for (int i = 0; i < a.length; i++) {
			r |= a[i] ^ b[i];
		}
		return r == 0;
	}

	public JsonObject toJsonObject() {
		JsonObject o = new JsonObject();
		o.addProperty("algorithm", algorithm);
		o.addProperty("iterations", iterations);
		o.addProperty("saltB64", saltB64);
		o.addProperty("hashB64", hashB64);
		return o;
	}

	public static PasswordHash fromJsonObject(JsonObject o) {
		PasswordHash ph = new PasswordHash();
		if (o.has("algorithm")) ph.algorithm = o.get("algorithm").getAsString();
		if (o.has("iterations")) ph.iterations = o.get("iterations").getAsInt();
		if (o.has("saltB64")) ph.saltB64 = o.get("saltB64").getAsString();
		if (o.has("hashB64")) ph.hashB64 = o.get("hashB64").getAsString();
		return ph;
	}
}

