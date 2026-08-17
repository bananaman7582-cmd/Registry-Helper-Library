package com.bananaman;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryHelperLibrary implements ModInitializer {
	public static final String MOD_ID = "registry-helper-library";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Registry Helper Library has been initialized.");
	}

	// 1.20.1 has no ResourceLocation.fromNamespaceAndPath factory, so use the constructor.
	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}