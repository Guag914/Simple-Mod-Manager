package net.guag.simplemodmanager;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DrawingUtils {

    public DrawingUtils() {}

    private final Map<String, Identifier> iconCache = new HashMap<>();

    // Use Minecraft texture as the primary default (this definitely exists)
    private static final Identifier DEFAULT_ICON = Identifier.of("simplemodmanager", "textures/gui/mod_icon.png");

    private String cleanName(String filename) {
        return filename.replaceAll("\\.(jar|zip|json)$", "");
    }

    public Identifier getModIcon(ModToggle mod) {
        File modFile = mod.getFile();
        String modId = cleanName(modFile.getName());

        // Check cache first
        if (iconCache.containsKey(modId)) {
            return iconCache.get(modId);
        }

        // Check if file exists in the correct location
        if (!modFile.exists()) {
            File enabledFile = new File("mods", modFile.getName());
            File disabledFile = new File("disabled-mods", modFile.getName());

            if (enabledFile.exists()) {
                modFile = enabledFile;
            } else if (disabledFile.exists()) {
                modFile = disabledFile;
            } else {
                // File doesn't exist, cache null and return null
                iconCache.put(modId, null);
                return null;
            }
        }

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("pack.png");
            if (entry == null) {
                // Try alternative icon locations
                entry = jar.getJarEntry("icon.png");
                if (entry == null) {
                    entry = jar.getJarEntry("assets/" + modId + "/icon.png");
                    if (entry == null) {
                        entry = jar.getJarEntry("assets/" + modId + "/textures/icon.png");
                    }
                }
            }

            if (entry == null) {
                iconCache.put(modId, null);
                return null;
            }

            try (InputStream stream = jar.getInputStream(entry)) {
                NativeImage image = NativeImage.read(stream);

                // Ensure image is valid
                if (image == null) {
                    iconCache.put(modId, null);
                    return null;
                }

                Identifier textureId = Identifier.of("simplemodmanager", "modicon/" + modId);

                // Simplified texture creation - remove the supplier function
                NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "modicon/" + modId, image);

                // Register texture safely
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getTextureManager() != null) {
                    try {
                        client.getTextureManager().registerTexture(textureId, texture);
                        iconCache.put(modId, textureId);
                        return textureId;
                    } catch (Exception e) {
                        // If registration fails, close image and cache null
                        try {
                            image.close();
                        } catch (Exception ignored) {}
                        iconCache.put(modId, null);
                        return null;
                    }
                } else {
                    // Close the image if we can't register the texture
                    try {
                        image.close();
                    } catch (Exception ignored) {}
                    iconCache.put(modId, null);
                    return null;
                }

            } catch (Exception e) {
                System.err.println("Error reading icon from " + modFile.getName() + ": " + e.getMessage());
                iconCache.put(modId, null);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error opening jar file " + modFile.getName() + ": " + e.getMessage());
            iconCache.put(modId, null);
            return null;
        }
    }

    public void renderImage(Identifier textureId, DrawContext context, int x, int y, int iconSize) {
        if (textureId == null) {
            return;
        }

        try {
            RenderPipeline pipeline = RenderPipeline.builder().build();
            context.drawTexture(
                    pipeline,
                    textureId,
                    x, y,
                    0.0F, 0.0F,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        } catch (Exception e) {
            // Don't spam console with errors for missing textures
            // System.err.println("Error rendering texture " + textureId + ": " + e.getMessage());
        }
    }

    // Method to get default icon
    public Identifier getDefaultIcon() {
        return DEFAULT_ICON;
    }

    // Method to render with automatic fallback
    public void renderModIcon(ModToggle mod, DrawContext context, int x, int y, int iconSize) {
        Identifier iconTexture = getModIcon(mod);
        if (iconTexture != null) {
            renderImage(iconTexture, context, x, y, iconSize);
        } else {
            // Use Minecraft's bundle texture as fallback
            renderImage(DEFAULT_ICON, context, x, y, iconSize);
        }
    }

    // Method to clear cache if needed
    public void clearIconCache() {
        iconCache.clear();
    }

    // Method to remove specific icon from cache
    public void removeIconFromCache(String modId) {
        iconCache.remove(modId);
    }
}