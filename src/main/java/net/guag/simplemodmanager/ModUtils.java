package net.guag.simplemodmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ModUtils {
    // Folders for mods (active and disabled).
    public static final File MODS_FOLDER = new File("mods");
    public static final File DISABLED_MODS_FOLDER = new File("disabled-mods");
    // Folders for resource packs.
    public static final File RESOURCEPACKS_FOLDER = new File("resourcepacks");
    public static final File DISABLED_RESOURCEPACKS_FOLDER = new File("resourcepacks","disabled-resourcepacks");
    // Folders for shader packs.
    public static final File SHADERPACKS_FOLDER = new File("shaderpacks");
    public static final File DISABLED_SHADERPACKS_FOLDER = new File("shaderpacks","disabled-shaderpacks");

    static {
        // Ensure all folders exist.
        MODS_FOLDER.mkdirs();
        DISABLED_MODS_FOLDER.mkdirs();
        RESOURCEPACKS_FOLDER.mkdirs();
        DISABLED_RESOURCEPACKS_FOLDER.mkdirs();
        SHADERPACKS_FOLDER.mkdirs();
        DISABLED_SHADERPACKS_FOLDER.mkdirs();
    }

    // Moves a mod .jar into the active mods folder.
    public static File moveModToEnabled(File modFile) throws IOException {
        File newFile = new File(MODS_FOLDER, modFile.getName());
        Files.move(modFile.toPath(), new File(MODS_FOLDER, modFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        return newFile;
    }

    // Moves a mod .jar into the disabled mods folder.
    public static File moveModToDisabled(File modFile) throws IOException {
        File newFile = new File(MODS_FOLDER, modFile.getName());
        Files.move(modFile.toPath(), new File(DISABLED_MODS_FOLDER, modFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        return newFile;
    }

    // Moves a resource pack file (folder or zip) to the appropriate folder.
    public static void moveResourcePack(String packName, boolean enable) throws IOException {
        File from = new File(enable ? DISABLED_RESOURCEPACKS_FOLDER : RESOURCEPACKS_FOLDER, packName);
        File to = new File(enable ? RESOURCEPACKS_FOLDER : DISABLED_RESOURCEPACKS_FOLDER, packName);
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // Moves a shader pack file (folder or zip) to the appropriate folder.
    public static void moveShaderPack(String packName, boolean enable) throws IOException {
        File from = new File(enable ? DISABLED_SHADERPACKS_FOLDER : SHADERPACKS_FOLDER, packName);
        File to = new File(enable ? SHADERPACKS_FOLDER : DISABLED_SHADERPACKS_FOLDER, packName);
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // Scan the active and disabled mods folders and return a list of ModToggle objects.
    public static List<ModToggle> getModToggles() {
        List<ModToggle> toggles = new ArrayList<>();
        File[] enabled = MODS_FOLDER.listFiles((dir, name) -> name.endsWith(".jar"));
        File[] disabled = DISABLED_MODS_FOLDER.listFiles((dir, name) -> name.endsWith(".jar"));

        if (enabled != null) {
            for (File f : enabled) {
                toggles.add(new ModToggle(f, true));
            }
        }
        if (disabled != null) {
            for (File f : disabled) {
                toggles.add(new ModToggle(f, false));
            }
        }
        return toggles;
    }

    public static List<ModToggle> getResourceToggles() {
        List<ModToggle> ResourceToggles = new ArrayList<>();
        File[] enabled = RESOURCEPACKS_FOLDER.listFiles((dir, name) -> !(name.endsWith(".txt")) && !(name.endsWith(".DS_Store")) && !(name.contains("disabled-resourcepacks")));
        File[] disabled = DISABLED_RESOURCEPACKS_FOLDER.listFiles((dir, name) -> !(name.endsWith(".txt")) && !(name.endsWith(".DS_Store"))&& !(name.contains("disabled-resourcepacks")));

        if (enabled != null) {
            for (File f : enabled) {
                ResourceToggles.add(new ModToggle(f, true));
            }
        }
        if (disabled != null) {
            for (File f : disabled) {
                ResourceToggles.add(new ModToggle(f, false));
            }
        }
        return ResourceToggles;
    }

    public static List<ModToggle> getShaderToggles() {
        List<ModToggle> shaderToggles = new ArrayList<>();
        File[] enabled = SHADERPACKS_FOLDER.listFiles((dir, name) ->!(name.endsWith(".txt")) && !(name.endsWith(".DS_Store"))&& !(name.contains("disabled-shaderpacks")));
        File[] disabled = DISABLED_SHADERPACKS_FOLDER.listFiles((dir, name) -> !(name.endsWith(".txt")) && !(name.endsWith(".DS_Store"))&& !(name.contains("disabled-shaderpacks")));


        if (enabled != null) {
            for (File f : enabled) {
                shaderToggles.add(new ModToggle(f, true));
            }
        }
        if (disabled != null) {
            for (File f : disabled) {
                shaderToggles.add(new ModToggle(f, false));
            }
        }
        return shaderToggles;
    }

    // Similar file-scanning functions could be added for resource packs and shader packs if needed.
}
