package com.ccmm.screentime.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ToastUtil {
	private ToastUtil() {}

	public static void show(Minecraft client, Component title, Component message) {
		if (client == null) return;

		Object toasts = getToasts(client);
		if (toasts == null) return;

		// Preferred: SystemToast.add(toasts, id, title, message)
		try {
			Class<?> systemToastClass = Class.forName("net.minecraft.client.gui.components.toasts.SystemToast");
			Class<?> toastComponentClass = Class.forName("net.minecraft.client.gui.components.toasts.ToastComponent");

			Object toastId = findAnyToastId(systemToastClass);
			if (toastId != null) {
				Method add = systemToastClass.getDeclaredMethod("add", toastComponentClass, toastId.getClass(), Component.class, Component.class);
				add.setAccessible(true);
				add.invoke(null, toasts, toastId, title, message);
				return;
			}
		} catch (Throwable ignored) {
			// fall through
		}

		// Fallback: toasts.addToast(new SystemToast(id, title, message))
		try {
			Class<?> toastClass = Class.forName("net.minecraft.client.gui.components.toasts.Toast");
			Class<?> systemToastClass = Class.forName("net.minecraft.client.gui.components.toasts.SystemToast");

			Object toastId = findAnyToastId(systemToastClass);
			if (toastId != null) {
				var ctor = systemToastClass.getDeclaredConstructor(toastId.getClass(), Component.class, Component.class);
				ctor.setAccessible(true);
				Object toast = ctor.newInstance(toastId, title, message);
				Method addToast = toasts.getClass().getMethod("addToast", toastClass);
				addToast.invoke(toasts, toast);
				return;
			}
		} catch (Throwable ignored) {
			// ignore
		}
	}

	private static Object getToasts(Minecraft client) {
		try {
			Method m = Minecraft.class.getMethod("getToasts");
			return m.invoke(client);
		} catch (Throwable ignored) {
		}

		try {
			Field f = Minecraft.class.getDeclaredField("toasts");
			f.setAccessible(true);
			return f.get(client);
		} catch (Throwable ignored) {
		}

		return null;
	}

	private static Object findAnyToastId(Class<?> systemToastClass) {
		// Mojmap has a nested class (often SystemToast$SystemToastIds) containing constants.
		for (Class<?> nested : systemToastClass.getDeclaredClasses()) {
			if (!nested.isEnum()) continue;
			try {
				Object[] constants = nested.getEnumConstants();
				if (constants != null && constants.length > 0) {
					// Prefer a stable built-in id if present, otherwise first constant.
					for (Object c : constants) {
						String n = c.toString();
						if (n.contains("TUTORIAL") || n.contains("PERIODIC") || n.contains("WORLD") || n.contains("PACK")) {
							return c;
						}
					}
					return constants[0];
				}
			} catch (Throwable ignored) {
			}
		}
		return null;
	}
}

