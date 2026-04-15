package com.ccmm.screentime;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenTime implements ModInitializer {
	public static final String MOD_ID = "screentime";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("ScreenTime loaded");
	}
}