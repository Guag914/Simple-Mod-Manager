package net.guag.simplemodmanager.util;

import net.guag.simplemodmanager.screen.FileToggle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.platform.NativeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DrawingUtils {
    public DrawingUtils() {}

    private final Map<String, Identifier> iconCache = new HashMap<>();

    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");

    // Use Minecraft texture as the primary default (this definitely exists)
    private static final Identifier DEFAULT_ICON = Identifier.fromNamespaceAndPath("simplemodmanager", "textures/gui/mod_icon.png");

    public Identifier getModIcon(FileToggle mod) {
        File modFile = mod.getFile();
        String modId = modFile.getName();

        if (iconCache.containsKey(modId)) return iconCache.get(modId);

        if (!modFile.exists()) {
            File enabledFile = new File("mods", modFile.getName());
            File disabledFile = new File("mods/disabled-mods", modFile.getName());
            if (enabledFile.exists()) modFile = enabledFile;
            else if (disabledFile.exists()) modFile = disabledFile;
            else { iconCache.put(modId, null); return null; }
        }

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = findIconEntry(jar, modId, mod);
            if (entry == null) { iconCache.put(modId, null); return null; }

            try (InputStream stream = jar.getInputStream(entry)) {
                NativeImage image = NativeImage.read(stream);

                String safeId = modId.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
                Identifier textureId = Identifier.fromNamespaceAndPath("simplemodmanager", "modicon/" + safeId);
                DynamicTexture texture = new DynamicTexture(() -> "modicon/" + safeId, image);

                Minecraft client = Minecraft.getInstance();
                client.getTextureManager().register(textureId, texture);
                iconCache.put(modId, textureId);
                return textureId;
            }
        } catch (Exception e) {
            LOGGER.error("Error loading icon for {}: {}", modFile.getName(), e.getMessage());
        }

        iconCache.put(modId, null);
        return null;
    }

    private JarEntry findIconEntry(JarFile jar, String modId, FileToggle toggle) {
        // Check common-known locations first
        String primaryPath = toggle.getIconPath();

        String[] knownPaths = {
                primaryPath,
                "pack.png",
                "icon.png",
                "logo.png",
                "assets/icon.png",
                "assets/" + modId + "/icon.png",
                "assets/" + modId + "/logo.png",
                "assets/" + modId + "/textures/icon.png",
                "assets/" + modId + "/textures/logo.png",
        };

        for (String path : knownPaths) {
            JarEntry entry = jar.getJarEntry(path);
            if (entry != null) return entry;
        }

        // Fall back to scanning all entries for any png that looks like an icon
        return jar.entries().asIterator().next() == null ? null :
                jar.stream()
                        .filter(e -> {
                            String name = e.getName().toLowerCase();
                            return name.endsWith(".png"); //since many mods use their mod-id.png for pack name
//                            (//since many mods use their name
//                                    name.contains("icon") ||
//                                            name.contains("logo") ||
//                                            name.contains("pack")
//                            )
                        })
                        .findFirst()
                        .orElse(null);
    }

    public static void renderImage(Identifier textureId, GuiGraphicsExtractor context, int x, int y, int iconSize) {
        if (textureId == null) return;
        try {
            context.blit(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    textureId,
                    x, y,
                    0.0F, 0.0F,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        } catch (Exception _) {}
    }

    // Method to render with automatic fallback
    public void renderModIcon(FileToggle mod, GuiGraphicsExtractor context, int x, int y, int iconSize) {
        Identifier iconTexture = getModIcon(mod);
        // Use Minecraft's bundle texture as fallback
        renderImage(Objects.requireNonNullElse(iconTexture, DEFAULT_ICON), context, x, y, iconSize);
    }
}