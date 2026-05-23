package net.guag.simplemodmanager.util;

import net.guag.simplemodmanager.screen.FileToggle;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    // Folders for mods (active and disabled).
    public static final File MODS_FOLDER = new File("mods");
    public static final File DISABLED_MODS_FOLDER = new File("mods", "disabled-mods");
    // Folders for resource packs.
    public static final File RESOURCEPACKS_FOLDER = new File("resourcepacks");
    public static final File DISABLED_RESOURCEPACKS_FOLDER = new File("resourcepacks","disabled-resourcepacks");
    // Folders for shader packs.
    public static final File SHADERPACKS_FOLDER = new File("shaderpacks");
    public static final File DISABLED_SHADERPACKS_FOLDER = new File("shaderpacks","disabled-shaderpacks");

    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");


    static {
        // Collect all paths into a list for clean, iterative processing
        List<File> folders = List.of(
                MODS_FOLDER,
                DISABLED_MODS_FOLDER,
                RESOURCEPACKS_FOLDER,
                DISABLED_RESOURCEPACKS_FOLDER,
                SHADERPACKS_FOLDER,
                DISABLED_SHADERPACKS_FOLDER
        );

        for (File folder : folders) {
            try { Files.createDirectories(folder.toPath());
            } catch (IOException e) {
                LOGGER.error("Critical: Could not initialize mod directory: {}", folder.getAbsolutePath(), e);
            } catch (Exception e) {
                LOGGER.error("Unexpected error during directory setup for {}", folder.getName(), e);
            }
        }
    }

    //--- Mod File Handlers ---
    public static File moveModToEnabled(File modFile){
        try {
            File newFile = new File(MODS_FOLDER, modFile.getName());
            Files.move(modFile.toPath(), new File(MODS_FOLDER, modFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFile;
        }
        catch(IOException e) {LOGGER.error("Failed to {} mod: {}", "enable", modFile.getName(), e);}

        return modFile;
    }

    public static File moveModToDisabled(File modFile) {
        try{
            File newFile = new File(DISABLED_MODS_FOLDER, modFile.getName());
            Files.move(modFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFile;
        }
        catch(IOException e) {LOGGER.error("Failed to {} mod: {}", "disable", modFile.getName(), e);}

        return modFile;
    }

    //--- Shader Pack File Handlers ---
    public static File moveShaderPackToEnabled(File modFile){
        try {
            File newFile = new File(SHADERPACKS_FOLDER, modFile.getName());
            Files.move(modFile.toPath(), new File(SHADERPACKS_FOLDER, modFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFile;
        }
        catch(IOException e) {LOGGER.error("Failed to {} shader pack: {}", "enable", modFile.getName(), e);}

        return modFile;
    }

    public static File moveShaderPackToDisabled(File modFile) {
        try{
            File newFile = new File(DISABLED_SHADERPACKS_FOLDER, modFile.getName());
            Files.move(modFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFile;
        }
        catch(IOException e) {LOGGER.error("Failed to {} shader pack: {}", "disable", modFile.getName(), e);}

        return modFile;
    }

    //--- Resource Pack File Handlers ---
    public static File moveResourcePackToEnabled(File modFile) {
        try {
            File newFile = new File(RESOURCEPACKS_FOLDER, modFile.getName());
            Files.move(modFile.toPath(), new File(RESOURCEPACKS_FOLDER, modFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFile;
        }
        catch(IOException e) {LOGGER.error("Failed to {} resource pack: {}", "enable", modFile.getName(), e);}

        return modFile;
    }

    public static File moveResourcePackToDisabled(File modFile) {
        try{
            File newFile = new File(DISABLED_RESOURCEPACKS_FOLDER, modFile.getName());
            Files.move(modFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFile;
        }
        catch(IOException e) {LOGGER.error("Failed to {} resource pack: {}", "disable", modFile.getName(), e);}

        return modFile;
    }

    //--- Resource Pack Minecraft API Handlers ---
    public static void enableResourcePack(String packName) {
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();

        repo.reload();

        String packId = "file/" + packName;

        List<String> selected = new ArrayList<>(repo.getSelectedIds());

        if (!selected.contains(packId)) {
            selected.add(packId);
        }

        repo.setSelected(selected);
        mc.options.resourcePacks = new ArrayList<>(selected);
        mc.options.save();
    }

    public static void disableResourcePack(String packName) {
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();

        repo.reload();

        String packId = "file/" + packName;

        List<String> selected = new ArrayList<>(repo.getSelectedIds());

        if (!selected.contains(packId)) {
            selected.remove(packId);
        }

        repo.setSelected(selected);
        mc.options.resourcePacks = new ArrayList<>(selected);
        mc.options.save();
    }


    // Scan the active and disabled mods folders and return a list of ModToggle objects.
    public static List<FileToggle> getModToggles() {
        List<FileToggle> toggles = new ArrayList<>();
        File[] enabled = MODS_FOLDER.listFiles((_, name) -> name.endsWith(".jar"));
        File[] disabled = DISABLED_MODS_FOLDER.listFiles((_, name) -> name.endsWith(".jar"));

        if (enabled != null) {
            for (File f : enabled) {
                toggles.add(new FileToggle(f, true));
            }
        }
        if (disabled != null) {
            for (File f : disabled) {
                toggles.add(new FileToggle(f, false));
            }
        }
        return toggles;
    }

    public static List<FileToggle> getResourceToggles() {
        List<FileToggle> ResourceToggles = new ArrayList<>();
        File[] enabled = RESOURCEPACKS_FOLDER.listFiles((_, name) -> !(name.endsWith(".txt")) && !(name.endsWith(".DS_Store")) && !(name.endsWith(".json")) && !(name.contains("disabled-resourcepacks")));
        File[] disabled = DISABLED_RESOURCEPACKS_FOLDER.listFiles((_, name) -> !(name.endsWith(".txt")) && !(name.endsWith(".DS_Store")) && !(name.endsWith(".json")) && !(name.contains("disabled-resourcepacks")));

        if (enabled != null) {
            for (File f : enabled) {
                ResourceToggles.add(new FileToggle(f, true));
            }
        }
        if (disabled != null) {
            for (File f : disabled) {
                ResourceToggles.add(new FileToggle(f, false));
            }
        }
        return ResourceToggles;
    }

    public static List<FileToggle> getShaderToggles() {
        List<FileToggle> shaderToggles = new ArrayList<>();
        File[] enabled = SHADERPACKS_FOLDER.listFiles((_, name) ->!(name.endsWith(".txt")) && !(name.endsWith(".DS_Store"))&& !(name.contains("disabled-shaderpacks")));
        File[] disabled = DISABLED_SHADERPACKS_FOLDER.listFiles((_, name) -> !(name.endsWith(".txt")) && !(name.endsWith(".DS_Store"))&& !(name.contains("disabled-shaderpacks")));


        if (enabled != null) {
            for (File f : enabled) {
                shaderToggles.add(new FileToggle(f, true));
            }
        }
        if (disabled != null) {
            for (File f : disabled) {
                shaderToggles.add(new FileToggle(f, false));
            }
        }
        return shaderToggles;
    }
}
