package net.guag.simplemodmanager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import net.fabricmc.api.ModInitializer;


public class SimpleModManager implements ModInitializer, ClientModInitializer {
	private static KeyBinding openUIBinding;

	private static String currentShaderpack = "";
	private static String currentResourcepack = "";

	@Override
	public void onInitialize() {
		onInitializeClient();
		System.out.println("Simple Mod Manager initialized!");
	}

	@Override
	public void onInitializeClient() {

		// Register a keybind (F8) to open the mod manager GUI.
		openUIBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.SimpleModManager.open_ui",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_F8,
				KeyBinding.Category.create(Identifier.of("simplemodmanager:keybinds"))
		));

		// On each client tick, if F8 is pressed and no screen is open, open the GUI.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openUIBinding.wasPressed()) {
				MinecraftClient mc = MinecraftClient.getInstance();
				if (mc.currentScreen == null) {
					// Pass the client and the hardcoded mod toggles (loaded from file folders)
					mc.setScreen(new ModManagerScreen(mc, ModUtils.getModToggles(), ModUtils.getResourceToggles(), ModUtils.getShaderToggles()));
				}
			}
		});
	}

	public static List<String> getAvailableShaderpacks() {
		File folder = new File(MinecraftClient.getInstance().runDirectory, "shaderpacks");
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".zip"));
		List<String> result = new ArrayList<>();
		if (files != null) {
			for (File file : files) result.add(file.getName());
		}
		return result;
	}

	public static List<String> getAvailableResourcepacks() {
		File folder = new File(MinecraftClient.getInstance().runDirectory, "resourcepacks");
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".zip"));
		List<String> result = new ArrayList<>();
		if (files != null) {
			for (File file : files) result.add(file.getName());
		}
		return result;
	}

	public static void setShaderpack(String name) {
		currentShaderpack = name;
		System.out.println("Shaderpack set to: " + name);
	}

	public static void setResourcepack(String name) {
		currentResourcepack = name;
		System.out.println("Resourcepack set to: " + name);
	}

	public static String getActiveShaderpack() {
		return currentShaderpack;
	}

	public static String getActiveResourcepack() {
		return currentResourcepack;
	}

}
