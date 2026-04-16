package com.ccmm.screentime.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ToastUtil {
	private ToastUtil() {}

	public static void show(Minecraft client, Component title, Component message) {
		if (client == null) return;

		Toast t = new SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
		client.getToastManager().addToast(t);
	}

}

