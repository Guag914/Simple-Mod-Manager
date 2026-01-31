package net.guag.simplemodmanager;

import net.minecraft.client.MinecraftClient;

import java.io.IOException;

public class ResourceUtils {
    private final MinecraftClient client;

    public ResourceUtils(MinecraftClient client) {
        this.client = client;
    }

    public void toggleResourcePack(String packName, boolean enable) {
        try {
            ModUtils.moveResourcePack(packName, enable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toggleShaderPack(String packName, boolean enable) {
        try {
            ModUtils.moveShaderPack(packName, enable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
