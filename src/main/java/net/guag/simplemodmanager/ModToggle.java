package net.guag.simplemodmanager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class ModToggle {
    private File file;
    private final String jarName;
    private boolean enabled;

    // This set now represents the persisted disabled mods list
    private static Set<String> defaultDisabledMods = new HashSet<>();

    public ModToggle(File file, boolean enabled) {
        this.file = file;
        this.enabled = enabled;
        this.jarName = file.getName();

    }

    public void toggle() {
        this.enabled = !this.enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public File getFile() {
        return file;
    }

    public Text getButtonText() {
        return Text.of(enabled ? "§aEnabled " : "§7Disabled ");
    }

    public void applyChange() {
        try {
            if (enabled) {
                this.file = ModUtils.moveModToEnabled(file);
            } else {
                this.file = ModUtils.moveModToDisabled(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static boolean isDisabledByDefault(String modFileName) {
        return defaultDisabledMods.contains(modFileName.toLowerCase());
    }

    public void resetToDefault() {
        boolean shouldBeEnabled = !isDisabledByDefault(file.getName());
        if (this.isEnabled() != shouldBeEnabled) {
            this.setEnabled(shouldBeEnabled);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private static final File STATE_FILE = new File(MinecraftClient.getInstance().runDirectory, "config/simplemodmanager_state.json");
    private static boolean initialized = false;

    public static void initializeDefaultDisabledMods() {
        if (initialized) return;

        if (STATE_FILE.exists()) {
            loadState();
        } else {
            scanDisabledModsFolder();
            saveState();
        }
        initialized = true;
    }

    private static void scanDisabledModsFolder() {
        File disabledDir = new File(MinecraftClient.getInstance().runDirectory, "disabled-mods");
        if (disabledDir.exists() && disabledDir.isDirectory()) {
            File[] files = disabledDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (files != null) {
                for (File file : files) {
                    defaultDisabledMods.add(file.getName().toLowerCase());
                }
            }
        }
    }

    private static void loadState() {
        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE))) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("defaultDisabledMods")) {
                JsonArray arr = obj.getAsJsonArray("defaultDisabledMods");
                for (int i = 0; i < arr.size(); i++) {
                    defaultDisabledMods.add(arr.get(i).getAsString().toLowerCase());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: scan folder if loading fails
            scanDisabledModsFolder();
        }
    }

    public static void saveState() {
        try {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String mod : defaultDisabledMods) {
                arr.add(mod);
            }
            obj.add("defaultDisabledMods", arr);

            STATE_FILE.getParentFile().mkdirs(); // Make sure config dir exists
            try (FileWriter writer = new FileWriter(STATE_FILE)) {
                new Gson().toJson(obj, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Call this method if you want to update the saved default disabled mods list dynamically
    public static void updateDefaultDisabledMods(Set<String> newDisabledMods) {
        defaultDisabledMods = new HashSet<>();
        for (String mod : newDisabledMods) {
            defaultDisabledMods.add(mod.toLowerCase());
        }
        saveState();
    }

    public String getJarName() {return this.jarName;}

    public String getDisplayName() {/** Remove extentions from name **/return jarName.replaceAll("\\.(jar|zip)$", "");}

}
