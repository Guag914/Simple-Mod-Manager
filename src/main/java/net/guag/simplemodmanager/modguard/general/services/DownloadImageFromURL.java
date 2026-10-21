package net.guag.simplemodmanager.modguard.general.services;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadImageFromURL {

    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");

    //caches for downloading, saving, and safeguarding icon rendering
    private static final Set<Identifier> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> LOADED = ConcurrentHashMap.newKeySet();

    public static boolean isLoaded(Identifier id) { return id != null && LOADED.contains(id); }

    public static Identifier download(String id, URL iconURL) {
        if (iconURL == null) return null;

        Identifier logoIdentifier = Identifier.fromNamespaceAndPath(
                "simplemodmanager", ("dynamic_icon/" + id).toLowerCase());

        if (!REQUESTED.add(logoIdentifier)) return logoIdentifier; // already downloaded/downloading

        Thread.startVirtualThread(() -> {
            try (InputStream input = iconURL.openStream()) {
                BufferedImage bufferedImage = ImageIO.read(input);
                if (bufferedImage == null) return;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);

                // NativeImage is CPU-only, fine off-thread
                NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));

                // anything that touches the GPU goes on the render thread
                Minecraft.getInstance().execute(() -> {
                    DynamicTexture dynamicTexture = new DynamicTexture(() -> "mod_icon_" + id, nativeImage);
                    Minecraft.getInstance().getTextureManager().register(logoIdentifier, dynamicTexture);
                });
            } catch (Exception e) {
                LOGGER.error("Critical: Could not cache images:", e);
            }
        });

        return logoIdentifier;
    }
}
