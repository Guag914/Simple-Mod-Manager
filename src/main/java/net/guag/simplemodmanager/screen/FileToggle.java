package net.guag.simplemodmanager.screen;

import net.guag.simplemodmanager.SimpleModManager;
import net.guag.simplemodmanager.util.FileUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.guag.simplemodmanager.util.ToggleState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import com.google.gson.*;


public class FileToggle {
    private File file;
    private final String jarName;
    private ToggleState state;

    private static final File STATE_FILE = new File(Minecraft.getInstance().gameDirectory, "config/simplemodmanager_state.json");
    private static boolean initialized = false;

    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");

    private static final Set<String> defaultDisabledMods = new HashSet<>();
    private static final Set<String> defaultDisabledResourcepacks = new HashSet<>();
    private static final Set<String> defaultDisabledShaderpacks = new HashSet<>();

    public FileToggle(File file, ToggleState enabled) {
        this.file = file;
        this.state = enabled;
        this.jarName = file.getName();
    }

    public static void initializeDefaultDisabledMods() {
        if (initialized) return;

        if (STATE_FILE.exists()) {
            loadState();
        } else {
            scanDisabledModsFolder();
            scanDisabledResourcepacksFolder();
            scanDisabledShaderpacksFolder();

            initConfigState();
        }
        initialized = true;
    }

    public void applyChange(String type) {
        try {
            switch (type) {
                case "mod":
                    if (this.state == ToggleState.ENABLED || this.state == ToggleState.LOCKED_ENABLED) { this.file = FileUtils.moveModToEnabled(file);}
                    else { this.file = FileUtils.moveModToDisabled(file); }
                    break;
                case "resourcepack":
                    if (this.state == ToggleState.ENABLED || this.state == ToggleState.LOCKED_ENABLED) { this.file = FileUtils.moveResourcePackToEnabled(file); FileUtils.enableResourcePack(this.getFile().getName());}
                    else { this.file = FileUtils.moveResourcePackToDisabled(file); FileUtils.disableResourcePack(this.getFile().getName()); }
                    break;
                case "shaderpack":
                    if (this.state == ToggleState.ENABLED || this.state == ToggleState.LOCKED_ENABLED) { this.file = FileUtils.moveShaderPackToEnabled(file);}
                    else { this.file = FileUtils.moveShaderPackToDisabled(file); }
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to {} file: {}", (state == ToggleState.ENABLED || state == ToggleState.DISABLED) ? "enable" : "disable", file.getName(), e);
        }
    }

    private static void scanDisabledModsFolder() {
        File disabledDir = new File(Minecraft.getInstance().gameDirectory, "mods/disabled-mods");
        if (disabledDir.exists() && disabledDir.isDirectory()) {
            File[] files = disabledDir.listFiles((_, name) -> name.toLowerCase().endsWith(".jar"));
            if (files != null) {
                for (File file : files) {
                    defaultDisabledMods.add(file.getName().toLowerCase());
                }
            }
        }
    }

    private static void scanDisabledResourcepacksFolder() {
        File disabledDir = new File(Minecraft.getInstance().gameDirectory, "resourcepacks/disabled-resourcepacks");
        if (disabledDir.exists() && disabledDir.isDirectory()) {
            File[] files = disabledDir.listFiles((_, name) -> !name.endsWith(".txt") && !name.endsWith(".DS_Store") && !name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    defaultDisabledResourcepacks.add(file.getName().toLowerCase());
                }
            }
        }
    }

    private static void scanDisabledShaderpacksFolder() {
        File disabledDir = new File(Minecraft.getInstance().gameDirectory, "shaderpacks/disabled-shaderpacks");
        if (disabledDir.exists() && disabledDir.isDirectory()) {
            File[] files = disabledDir.listFiles((_, name) -> !name.endsWith(".txt") && !name.endsWith(".DS_Store"));
            if (files != null) {
                for (File file : files) {
                    defaultDisabledShaderpacks.add(file.getName().toLowerCase());
                }
            }
        }
    }

    public void resetToDefault() {
        boolean disabledByDefault = isDisabledByDefault(file.getName())
                || isResourcepackDisabledByDefault(file.getName())
                || isShaderpackDisabledByDefault(file.getName());

        ToggleState defaultState = disabledByDefault ? ToggleState.DISABLED : ToggleState.ENABLED;
        if (this.state != defaultState) {
            this.state = defaultState;
        }
    }

    //Metadata getters
    public String getIconPath(){
        File modFile = this.getFile();

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return "Error reading metadata";

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return "Invalid metadata";

                return  je.getAsJsonObject().has("icon") ?  je.getAsJsonObject().get("icon").getAsString() : null;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get mod icon path: {}", this, e);
            return "Error reading metadata";
        }
    }

    public String getMetadataSummaryForMod() {
        File modFile = this.getFile();

        String modId = this.getModId();
        Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(modId);

        String modName;
        boolean jarName;
        if (containerOpt.isPresent()) {
            modName = containerOpt.get().getMetadata().getName();
            jarName = false;
        } else {
            modName = this.getJarName(); // fallback to jar name if metadata is missing
            jarName = true;
        }

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return "No metadata";

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return "Invalid metadata";

                JsonObject root = je.getAsJsonObject();

                // Extract fields
                String version = root.has("version") ? root.get("version").getAsString() : null;

                // Compose a summary string (truncate description for brevity)
                StringBuilder summary = new StringBuilder();

                if (jarName){
                    summary.append(modName);
                } else  { summary.append(modName).append(":");

                    if (version != null) {
                        summary.append(" v").append(version);
                    }
                }
                return summary.toString().isEmpty() ? "No metadata" : summary.toString();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get mod metadata summary: {}", this, e);
            return "Error reading metadata";
        }
    }

    public String getDescription(){
        File modFile = this.getFile();

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return "No metadata";

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return "Invalid metadata";

                return  je.getAsJsonObject().has("description") ?  je.getAsJsonObject().get("description").getAsString() : null;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get mod description: {}", this, e);
            return "Error reading metadata";
        }
    }

    public String getModId() {
        File modFile = this.getFile();

        if (!modFile.exists()) {
            // Try the other folder — e.g., if enabled, check disabled, or vice versa
            File altFile = new File("run/disabled-mods", modFile.getName());
            if (altFile.exists()) modFile = altFile;
        }

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return null;

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return null;

                JsonObject root = je.getAsJsonObject();

                if (root.has("id")) {
                    return root.get("id").getAsString();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get modID: {}", this, e);
        }
        return null;
    }

    public ArrayList<String> getDependencies() {
        File modFile = this.getFile();

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return new ArrayList<>(Collections.singleton("No dependencies"));

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return new ArrayList<>(Collections.singleton("Invalid dependencies"));

                JsonObject depends = je.getAsJsonObject().getAsJsonObject("depends");
                if (depends == null || !depends.isJsonObject()) return new ArrayList<>(Collections.singleton("No dependencies"));

                ArrayList<String> dependencies = new ArrayList<>();

                HashMap<String, String> converted = new HashMap<>();
                for (String key : depends.keySet()) {
                    JsonElement value = depends.get(key);
                    if (value.isJsonArray()) {
                        // Take the first element of the array as the version string
                        JsonArray arr = value.getAsJsonArray();
                        converted.put(key, !arr.isEmpty() ? arr.get(0).getAsString() : "");
                    } else {
                        converted.put(key, value.getAsString());
                    }
                }

                Set<String> ignored = new HashSet<>(Set.of("fabricloader", "minecraft", "java"));

                for (Map.Entry<String, String> dependency : converted.entrySet()) {
                    if (!ignored.contains(dependency.getKey())) {
                        dependencies.add(dependency.getKey());
                    }
                }

                return dependencies;

            }
        } catch (IOException e) {
            LOGGER.error("Failed to get mod description: {}", this, e);
            return new ArrayList<>(Collections.singleton("Error reading dependencies"));
        }
    }

    //Getters
    public String getJarName() {return this.jarName;}
    public String getDisplayName() {return jarName.replaceAll("\\.(jar|zip)$", "");}

    //cycle through states
    public void toggled() {
        if (ContentManagerScreen.whitelist_enabled.equals("init") && SimpleModManager.configWhitelist) {
            this.state = switch (this.state) {
                case ENABLED -> ToggleState.DISABLED;
                case DISABLED -> ToggleState.LOCKED_DISABLED;
                case LOCKED_DISABLED -> ToggleState.LOCKED_ENABLED;
                case LOCKED_ENABLED -> ToggleState.ENABLED;
            };
        } else {
            this.state = switch (this.state) {
                case ENABLED -> ToggleState.DISABLED;
                case DISABLED -> ToggleState.ENABLED;
                default -> this.state; //locked states stay locked
            };
        }
    }

    public ToggleState getState() { return state; }
    public File getFile() {return file;}

    public Component getButtonText() {
        if (this.state == ToggleState.ENABLED){ return Component.literal("§aEnabled "); }
        if (this.state == ToggleState.DISABLED){ return Component.literal("§7Disabled "); }
        if (this.state == ToggleState.LOCKED_ENABLED){ return Component.literal("§aLocked "); }
        if (this.state == ToggleState.LOCKED_DISABLED){ return Component.literal("§7Locked "); }

        return Component.empty();
    }

    public static boolean isDisabledByDefault(String modFileName) {return defaultDisabledMods.contains(modFileName.toLowerCase());}
    public static boolean isResourcepackDisabledByDefault(String fileName) { return defaultDisabledResourcepacks.contains(fileName.toLowerCase()); }
    public static boolean isShaderpackDisabledByDefault(String fileName) { return defaultDisabledShaderpacks.contains(fileName.toLowerCase()); }

    public int getSortPriority() {
        if (ContentManagerScreen.dependencies.contains(this.getModId())) return 4; // dependencies last
        return switch (this.getState()) {
            case ToggleState.ENABLED -> 0;
            case ToggleState.DISABLED -> 1;
            case ToggleState.LOCKED_ENABLED -> 2;
            case ToggleState.LOCKED_DISABLED -> 3;
        };
    }

    //Setters
    public void setState(ToggleState state) { this.state = state; }

    //Initializes config file
    public static void initConfigState() {
        try {
            JsonObject obj = new JsonObject();
            JsonArray modArr = new JsonArray();
            JsonArray resourceArr = new JsonArray();
            JsonArray shaderArr = new JsonArray();

            for (String mod : defaultDisabledMods) { modArr.add(mod); }
            for (String mod : defaultDisabledResourcepacks) { resourceArr.add(mod); }
            for (String mod : defaultDisabledShaderpacks) { shaderArr.add(mod); }

            obj.add("defaultDisabledMods", modArr);
            obj.add("defaultDisabledResourcepacks", resourceArr);
            obj.add("defaultDisabledShaderpacks", shaderArr);

            //whitelist features
            obj.addProperty("whitelist_enabled", "init");
            ContentManagerScreen.whitelist_enabled = "init";


            obj.add("allowed_mods", new JsonArray());
            obj.add("allowed_resourcepacks", new JsonArray());
            obj.add("allowed_shaderpacks", new JsonArray());

            obj.add("locked_mods", new JsonObject());
            obj.add("locked_resourcepacks", new JsonObject());
            obj.add("locked_shaderpacks", new JsonObject());

            STATE_FILE.getParentFile().mkdirs(); // Make sure config dir exists
            try (FileWriter writer = new FileWriter(STATE_FILE)) {
                new Gson().toJson(obj, writer);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to save mod state configuration to {}", STATE_FILE.getName(), e);
        }
    }

    //Moves defaults from config into memory
    private static void loadState() {
        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE))) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("defaultDisabledMods")) {
                JsonArray arr = obj.getAsJsonArray("defaultDisabledMods");
                for (int i = 0; i < arr.size(); i++) {
                    defaultDisabledMods.add(arr.get(i).getAsString().toLowerCase());
                }
            }

            if (obj.has("defaultDisabledResourcepacks")) {
                JsonArray arr = obj.getAsJsonArray("defaultDisabledResourcepacks");
                for (int i = 0; i < arr.size(); i++) {
                    defaultDisabledResourcepacks.add(arr.get(i).getAsString().toLowerCase());
                }
            }

            if (obj.has("defaultDisabledShaderpacks")) {
                JsonArray arr = obj.getAsJsonArray("defaultDisabledShaderpacks");
                for (int i = 0; i < arr.size(); i++) {
                    defaultDisabledShaderpacks.add(arr.get(i).getAsString().toLowerCase());
                }
            }


        } catch (Exception e) {
            LOGGER.error("Failed to parse state file {}. Falling back to folder scan.", STATE_FILE.getName(), e);
            scanDisabledModsFolder();
            scanDisabledResourcepacksFolder();
            scanDisabledShaderpacksFolder();
        }
    }

    //Moves whitelist from config file into memory
    public static void loadWhitelistState() {
        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE))) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

            if (obj.has("whitelist_enabled")) {
                String isEnabledStr = obj.get("whitelist_enabled").getAsString();
                if (isEnabledStr.isEmpty() || isEnabledStr.equals("init")) { ContentManagerScreen.whitelist_enabled = "init"; }
                else if (isEnabledStr.equals("true")) { ContentManagerScreen.whitelist_enabled = "true"; }
                else { ContentManagerScreen.whitelist_enabled = "false"; }
            }

            if (obj.has("allowed_mods")) {
                JsonArray arr = obj.getAsJsonArray("allowed_mods");
                for (int i = 0; i < arr.size(); i++) {
                    ContentManagerScreen.whitelist.add(arr.get(i).getAsString());
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load whitelist state from {}.", STATE_FILE.getName(), e);
        }
    }

    //Moves memory whitelist into config file
    public static void updateConfigWhitelist() {
        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE))) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

            JsonArray whitelistArr = new JsonArray();
            for (String mod : ContentManagerScreen.whitelist) { whitelistArr.add(mod); }

            obj.add("allowed_mods", whitelistArr);
            obj.addProperty("whitelist_enabled", ContentManagerScreen.whitelist_enabled);

            STATE_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(STATE_FILE)) {
                new Gson().toJson(obj, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update whitelist in config.", e);
        }
    }

    //locked state methods
    public static void saveLockedStates(List<FileToggle> mods, List<FileToggle> resourcepacks, List<FileToggle> shaderpacks) {
        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE))) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

            JsonObject lockedMods = new JsonObject();
            JsonObject lockedResourcepacks = new JsonObject();
            JsonObject lockedShaderpacks = new JsonObject();
            for (FileToggle toggle : mods) {
                if (toggle.getState() == ToggleState.LOCKED_ENABLED || toggle.getState() == ToggleState.LOCKED_DISABLED) {
                    lockedMods.addProperty(toggle.getModId(), toggle.getState().name());
                }
            }

            for (FileToggle toggle : resourcepacks) {
                if (toggle.getState() == ToggleState.LOCKED_ENABLED || toggle.getState() == ToggleState.LOCKED_DISABLED) {
                    lockedResourcepacks.addProperty(toggle.getJarName(), toggle.getState().name());
                }
            }

            for (FileToggle toggle : shaderpacks) {
                if (toggle.getState() == ToggleState.LOCKED_ENABLED || toggle.getState() == ToggleState.LOCKED_DISABLED) {
                    lockedShaderpacks.addProperty(toggle.getJarName(), toggle.getState().name());
                }
            }
            obj.add("locked_mods", lockedMods);
            obj.add("locked_resourcepacks", lockedResourcepacks);
            obj.add("locked_shaderpacks", lockedShaderpacks);

            STATE_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(STATE_FILE)) {
                new Gson().toJson(obj, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save locked states.", e);
        }
    }

    public static void loadLockedStates(List<FileToggle> mods, List<FileToggle> resourcepacks, List<FileToggle> shaderpacks) {
        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE))) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

            if (obj.has("locked_mods")) {
                JsonObject lockedMods = obj.getAsJsonObject("locked_mods");
                for (FileToggle toggle : mods) {
                    String modId = toggle.getModId();
                    if (modId != null && lockedMods.has(modId)) {
                        toggle.setState(ToggleState.valueOf(lockedMods.get(modId).getAsString()));
                    }
                }
            }

            if (obj.has("locked_resourcepacks")) {
                JsonObject lockedResourcepacks = obj.getAsJsonObject("locked_resourcepacks");
                for (FileToggle toggle : resourcepacks) {
                    String modId = toggle.getModId();
                    if (modId != null && lockedResourcepacks.has(modId)) {
                        toggle.setState(ToggleState.valueOf(lockedResourcepacks.get(modId).getAsString()));
                    }
                }
            }

            if (obj.has("locked_shaderpacks")) {
                JsonObject lockedShaderpacks = obj.getAsJsonObject("locked_shaderpacks");
                for (FileToggle toggle : shaderpacks) {
                    String modId = toggle.getModId();
                    if (modId != null && lockedShaderpacks.has(modId)) {
                        toggle.setState(ToggleState.valueOf(lockedShaderpacks.get(modId).getAsString()));
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load locked states.", e);
        }
    }
}