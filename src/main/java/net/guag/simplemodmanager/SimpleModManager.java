package net.guag.simplemodmanager;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.api.ModInitializer;

import net.guag.simplemodmanager.screen.ContentManagerScreen;
import net.guag.simplemodmanager.screen.InstallerScreen;
import net.guag.simplemodmanager.util.FileUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

//test seed: -781932279150905825

public class SimpleModManager implements ModInitializer, ClientModInitializer {
	private static KeyMapping openUIBinding;
	private static KeyMapping testAPIPipeline;
	public static boolean configWhitelist = false;

	@Override
	public void onInitialize() {
		onInitializeClient();
		System.out.println("Simple Mod Manager initialized!");
	}

	@Override
	public void onInitializeClient() {

		//register mapping
		KeyMapping.Category keybinds = KeyMapping.Category.register(
				Identifier.bySeparator("simplemodmanager:keybinds", ':')
		);

		//register f8 to open mod gui
		openUIBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.SimpleModManager.openui",
				InputConstants.Type.KEYBOARD,
				InputConstants.KEY_F8,
				keybinds
		));

		testAPIPipeline = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.SimpleModManager.testAPI",
				InputConstants.Type.KEYBOARD,
				InputConstants.KEY_F9,
				keybinds
		));

		//check for key presses on each client tick
		ClientTickEvents.END_CLIENT_TICK.register(_ -> {
			while (openUIBinding.consumeClick()) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.gui.screen() == null) {
					mc.gui.setScreen(new ContentManagerScreen(mc, FileUtils.getModToggles("all"), FileUtils.getResourceToggles(), FileUtils.getShaderToggles()));
				}
			}

			while (testAPIPipeline.consumeClick()) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.gui.screen() == null) mc.gui.setScreen(new InstallerScreen(mc));
			}
		});
	}

}
