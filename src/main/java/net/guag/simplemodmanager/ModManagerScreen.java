package net.guag.simplemodmanager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModManagerScreen extends Screen{
    private final MinecraftClient client;
    private final List<ModToggle> modToggles;
    private final List<ModToggle> resourceToggles;
    private final List<ModToggle> shaderToggles;

    DrawingUtils drawUtil = new DrawingUtils();

    // Scrolling state
    private double scrollAmount = 0;
    private double maxScroll = 0;
    private final double scrollStep = 15;

    private TextFieldWidget searchBox;
    private String searchQuery = "";

    //Button Info
    int btnHeight = 20;

    private final List<ButtonWidget> modToggleButtons = new ArrayList<>();
    private final List<ButtonWidget> shaderButtons = new ArrayList<>();
    private final List<ButtonWidget> resourceButtons = new ArrayList<>();
    private final List<ButtonWidget> modResetButtons = new ArrayList<>();
    private final List<ButtonWidget> modMetadataButtons = new ArrayList<>();
    private final Map<ButtonWidget, String> tooltipMap = new HashMap<>();
    private final List<ButtonWidget> reloadButtons = new ArrayList<>();

    private final List<ButtonWidget> resourceMetadataButtons = new ArrayList<>();
    private final List<ButtonWidget> shaderMetadataButtons = new ArrayList<>();
    private final List<ButtonWidget> resourceToggleButtons = new ArrayList<>();
    private final List<ButtonWidget> shaderToggleButtons = new ArrayList<>();
    private final List<ButtonWidget> resourceResetButtons = new ArrayList<>();
    private final List<ButtonWidget> shaderResetButtons = new ArrayList<>();

    private final List<ButtonWidget> headerButtons = new ArrayList<>();


    public String getModId(ModToggle mod) {
        File modFile = mod.getFile();

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
            e.printStackTrace();
        }
        return null;
    }

    public ModManagerScreen(MinecraftClient client, List<ModToggle> modToggles, List<ModToggle> resourceToggles, List<ModToggle> shaderToggles) {
        super(Text.of("Realism Mod Manager"));
        this.client = client;
        this.modToggles = modToggles;
        this.resourceToggles = resourceToggles;
        this.shaderToggles = shaderToggles;
    }


    public String getMetadataSummaryForMod(ModToggle mod) {
        File modFile = mod.getFile();

        String modId = getModId(mod);
        Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(modId);

        String modName;
        boolean jarName;
        if (containerOpt.isPresent()) {
            modName = containerOpt.get().getMetadata().getName();
            jarName = false;
        } else {
            modName = mod.getJarName(); // fallback to jar name if metadata is missing
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
                } else if (!jarName) { summary.append(modName + ":");

                    if (version != null) {
                        summary.append(" v").append(version);
                    }
                }
                return summary.toString().isEmpty() ? "No metadata" : summary.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading metadata";
        }
    }

    public String getExtraInfo(ModToggle mod) {
        File modFile = mod.getFile();
        String modName;
        modName = mod.getJarName(); // fallback to jar name (this is because tooltip needs full file)

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return "No metadata";

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return "Invalid metadata";

                JsonObject root = je.getAsJsonObject();

                // Extract fields
                String version = root.has("version") ? root.get("version").getAsString() : null;
                // Authors can be array or string, handle both
                String authors = null;
                if (root.has("authors")) {
                    if (root.get("authors").isJsonArray()) {
                        authors = root.get("authors").getAsJsonArray().toString();
                    } else {
                        authors = root.get("authors").getAsString();
                    }
                }

                // Compose a summary string (truncate description for brevity)
                StringBuilder extraSummary = new StringBuilder();

                extraSummary.append(modName + ":");

                if (version != null) {
                    extraSummary.append(" v").append(version);
                }
                if (authors != null) {
                    extraSummary.append(" by ").append(authors);
                }

                return extraSummary.toString().isEmpty() ? "No metadata" : extraSummary.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading metadata";
        }
    }

    protected  void fillScreen(DrawContext context){
        context.fill(0, 0, this.width, this.height, 0xFF202020);
    }

    @Override
    protected void init() {
        ResourceUtils resourceUtil = new ResourceUtils(client);
        this.modToggleButtons.clear();
        this.shaderButtons.clear();
        this.resourceButtons.clear();

        this.modResetButtons.clear();
        this.shaderResetButtons.clear();
        this.resourceResetButtons.clear();
        this.headerButtons.clear();

        this.clearChildren();

        int centerX = this.width / 2;
        int buttonWidth = 40;
        int buttonHeight = 20;
        int spacing = 25;
        int y = 10;

        // --- Mods ---


        this.searchBox = new TextFieldWidget(this.textRenderer, 0, 0, this.width, 20, Text.of("Search"));
        this.searchBox.setChangedListener(query -> {
            this.searchQuery = query.toLowerCase();
            // Don't call updateVisibleButtons here, let render() handle it
        });
        this.searchBox.setMaxLength(100);
        this.searchBox.setEditable(true);
        this.addSelectableChild(this.searchBox);
        this.setInitialFocus(this.searchBox);

        ModToggle.initializeDefaultDisabledMods();

        int index = 0;

        ButtonWidget modsHeader = ButtonWidget.builder(Text.of("Mods"),
                button -> {} // Do Nothing
        ).dimensions(centerX, this.height - 50, 240, 20).build();

        modsHeader.active = false;  // Disable interaction
        headerButtons.add(modsHeader);
        addDrawableChild(modsHeader);

        for (ModToggle toggle : modToggles) {
            String name = cleanName(toggle.getFile().getName());

            ButtonWidget toggleFunc = ButtonWidget.builder(Text.literal(toggle.getButtonText().getString()), button -> {
                toggle.toggle();
                button.setMessage(Text.literal(toggle.getButtonText().getString()));
            }).dimensions(centerX + 10, y, buttonWidth+20, buttonHeight).build();
            addDrawableChild(toggleFunc);
            modToggleButtons.add(toggleFunc);

            ButtonWidget resetFunc = ButtonWidget.builder(Text.of("Reset"), button -> {
                toggle.resetToDefault();
                // Optionally update toggle button text here if needed
            }).dimensions(centerX + 110, y, 60, buttonHeight).build();

            addDrawableChild(resetFunc);
            modResetButtons.add(resetFunc);

            // Metadata button (left column)
            ButtonWidget metadataFunc = ButtonWidget.builder(
                    Text.literal(getMetadataSummaryForMod(modToggles.get(index))),
                    button -> {} // no action on click
            ).dimensions(centerX, y, 180, buttonHeight).build();

            metadataFunc.active = false;  // disable interaction
            addDrawableChild(metadataFunc); // add to screen
            modMetadataButtons.add(metadataFunc); // keep track of it

            // keep track of it



            String extraInfo = getExtraInfo(modToggles.get(index));

            // Store the tooltip string for later
            tooltipMap.put(metadataFunc, extraInfo);// tooltipMap is a Map<ButtonWidget, String>

            y += spacing;
            index += 1;
        }

        index = 0;
        //Index 3 = mods, index 4  = resource packs, index 5 = shader packs

        ButtonWidget resourceHeader = ButtonWidget.builder(Text.of("Resource Packs"),
                button -> {} // Do Nothing
        ).dimensions(centerX, this.height - 50, 240, 20).build();

        resourceHeader.active = false;  // Disable interaction
        headerButtons.add(resourceHeader);
        addDrawableChild(resourceHeader);

        for (ModToggle toggle : resourceToggles) {
            ButtonWidget resourceMetadataFunc = ButtonWidget.builder(
                    Text.literal(resourceToggles.get(index).getFile().getName()),
                    button -> {
                    } // no action on click
            ).dimensions(centerX, y, 180, buttonHeight).build();

            resourceMetadataFunc.active = false;  // disable interaction
            addDrawableChild(resourceMetadataFunc); // add to screen
            resourceMetadataButtons.add(resourceMetadataFunc); // keep track of it

            ButtonWidget resourceToggleFunc = ButtonWidget.builder(Text.literal(toggle.getButtonText().getString()), button -> {
                toggle.toggle();
                resourceUtil.toggleResourcePack(toggle.getFile().getName(), toggle.isEnabled());
                button.setMessage(Text.literal(toggle.getButtonText().getString()));
            }).dimensions(centerX + 10, y, buttonWidth + 20, buttonHeight).build();
            addDrawableChild(resourceToggleFunc);
            resourceToggleButtons.add(resourceToggleFunc);


            y += spacing;
            index += 1;
        }

        index = 0;

        ButtonWidget shaderHeader = ButtonWidget.builder(Text.of("Shader Packs"),
                button -> {} // Do Nothing
        ).dimensions(centerX, this.height - 50, 240, 20).build();

        shaderHeader.active = false;  // Disable interaction
        headerButtons.add(shaderHeader);
        addDrawableChild(shaderHeader);

        for (ModToggle toggle : shaderToggles) {
            ButtonWidget shaderMetadataFunc = ButtonWidget.builder(
                    Text.literal(shaderToggles.get(index).getFile().getName()),
                    button -> {
                    } // no action on click
            ).dimensions(centerX, y, 180, buttonHeight).build();

            shaderMetadataFunc.active = false;  // disable interaction
            addDrawableChild(shaderMetadataFunc); // add to screen
            shaderMetadataButtons.add(shaderMetadataFunc); // keep track of it

            ButtonWidget shaderToggleFunc = ButtonWidget.builder(Text.literal(toggle.getButtonText().getString()), button -> {
                toggle.toggle();
                resourceUtil.toggleShaderPack(toggle.getFile().getName(), toggle.isEnabled());
                button.setMessage(Text.literal(toggle.getButtonText().getString()));
            }).dimensions(centerX + 10, y, buttonWidth + 20, buttonHeight).build();
            addDrawableChild(shaderToggleFunc);
            shaderToggleButtons.add(shaderToggleFunc);


            y += spacing;
            index += 1;
        }

        ButtonWidget resourceFunc = ButtonWidget.builder(Text.of("Refresh Resources"), b -> {
            MinecraftClient.getInstance().reloadResources();
            client.setScreen(null);
        }).dimensions(centerX, y, 240, btnHeight).build();
        reloadButtons.add(resourceFunc);
        addDrawableChild(resourceFunc);

        int contentHeight = y + 20;
        int buttonY = this.height - 50;

        ButtonWidget applyFunc = ButtonWidget.builder(Text.of("Apply Changes"), button -> {
            for (ModToggle toggle : modToggles) toggle.applyChange();
            MinecraftClient.getInstance().reloadResources();
        }).dimensions(centerX - 130, 10, 120, 20).build();
        headerButtons.add(applyFunc);
        addDrawableChild(applyFunc);

        ButtonWidget cancelFunc = ButtonWidget.builder(Text.of("Cancel"), button -> {
            client.setScreen(null);
        }).dimensions(centerX + 10, 10, 120, 20).build();
        headerButtons.add(cancelFunc);
        addDrawableChild(cancelFunc);

        tooltipMap.put(applyFunc, "Restart the game to apply changes to mod settings.");
        maxScroll = Math.max(0, contentHeight+200 /** change content height to scroll less/more on screen**/ - (this.height - 80));

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        //Index 3 = mods, index 4  = resource packs, index 5 = shader packs (headers)
        context.fill(0, 0, this.width, this.height, 0xFF202020);

        // First, hide all buttons that don't match search
        updateVisibleButtonsBasedOnSearch();

        // Then render tooltips for visible buttons
        for (Map.Entry<ButtonWidget, String> entry : tooltipMap.entrySet()) {
            ButtonWidget button = entry.getKey();
            if (button.isHovered() && button.visible) {
                context.drawTooltip(
                        MinecraftClient.getInstance().textRenderer,
                        Text.literal(entry.getValue()),
                        mouseX,
                        mouseY
                );
                break; // Show only one tooltip at a time
            }
        }

        int y = -(int)scrollAmount;
        int centerX = this.width / 2;
        int spacing = 25;


        int offset = 40;
        int col1X = centerX - 200 + offset;  // mod name + metadata
        int col2X = centerX + offset;  // toggle button only
        int col3X = centerX + 70 + offset;  // reset button

        y += 50;

        // Only render if there are visible mods or if search is empty
        boolean hasVisibleMods = hasVisibleItemsInCategory("mods");
        if (hasVisibleMods || searchQuery.isEmpty()) {
            ButtonWidget modsHeader = headerButtons.getFirst();
            modsHeader.setX(centerX-120);
            modsHeader.setY(y);
            if (hasVisibleMods || searchQuery.isEmpty()) {
                modsHeader.render(context, mouseX, mouseY, delta);
            }
            y += 25;
        }

        for (int i = 0; i < modToggles.size(); i++) {
            ModToggle toggle = modToggles.get(i);
            if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) continue;

            drawUtil.renderModIcon(modToggles.get(i), context, col1X - offset, y, 20);

            // Position and render toggle button
            ButtonWidget toggleBtn = modToggleButtons.get(i);
            toggleBtn.setX(col2X);
            toggleBtn.setY(y);
            toggleBtn.render(context, mouseX, mouseY, delta);

            // Position and render reset button
            ButtonWidget resetBtn = modResetButtons.get(i);
            resetBtn.setX(col3X);
            resetBtn.setY(y);
            resetBtn.render(context, mouseX, mouseY, delta);

            ButtonWidget metadataBtn = modMetadataButtons.get(i);
            metadataBtn.setX(col1X);
            metadataBtn.setY(y);
            metadataBtn.render(context, mouseX, mouseY, delta);

            y += 25; // spacing between rows
        }

        // Only render if there are visible resource packs or if search is empty
        boolean hasVisibleResources = hasVisibleItemsInCategory("resourcepacks");
        if (hasVisibleResources || searchQuery.isEmpty()) {
            y += 25;
            ButtonWidget resourceHeader = headerButtons.get(1);
            resourceHeader.setX(centerX-120);
            resourceHeader.setY(y);
            resourceHeader.render(context, mouseX, mouseY, delta);
            y += 25;
        }

        for (int i = 0; i < resourceToggles.size(); i++) {
            ModToggle toggle = resourceToggles.get(i);
            if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) continue;

            drawUtil.renderModIcon(resourceToggles.get(i), context, col1X - offset, y, 20);

            //Position and render toggle button
            ButtonWidget resourceToggleBtn = resourceToggleButtons.get(i);
            resourceToggleBtn.setX(col2X+30);
            resourceToggleBtn.setY(y);
            resourceToggleBtn.render(context, mouseX, mouseY, delta);

            ButtonWidget resourceMetadataBtn = resourceMetadataButtons.get(i);
            resourceMetadataBtn.setX(col1X+30);
            resourceMetadataBtn.setY(y);
            resourceMetadataBtn.render(context, mouseX, mouseY, delta);

            y += 25; // spacing between rows
        }

        // Only render if there are visible shader packs or if search is empty
        boolean hasVisibleShaders = hasVisibleItemsInCategory("shaderpacks");
        if (hasVisibleShaders || searchQuery.isEmpty()) {
            y += 25;
            ButtonWidget shaderHeader = headerButtons.get(2);
            shaderHeader.setX(centerX-120);
            shaderHeader.setY(y);
            shaderHeader.render(context, mouseX, mouseY, delta);
            y += 25;
        }

        for (int i = 0; i < shaderToggles.size(); i++) {
            ModToggle toggle = shaderToggles.get(i);
            if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) continue;

            drawUtil.renderModIcon(shaderToggles.get(i), context, col1X - offset, y, 20);

            // Position and render toggle button
            ButtonWidget shaderToggleBtn = shaderToggleButtons.get(i);
            shaderToggleBtn.setX(col2X+30);
            shaderToggleBtn.setY(y);
            shaderToggleBtn.render(context, mouseX, mouseY, delta);

            ButtonWidget shaderMetadataBtn = shaderMetadataButtons.get(i);
            shaderMetadataBtn.setX(col1X+30);
            shaderMetadataBtn.setY(y);
            shaderMetadataBtn.render(context, mouseX, mouseY, delta);

            y += 25; // spacing between rows
        }

        y += 25;

        ButtonWidget resourceButton = reloadButtons.getFirst();
        resourceButton.setX(centerX-120);
        resourceButton.setY(y);
        resourceButton.render(context, mouseX, mouseY, delta);

        y +=25;

        ButtonWidget applyBtn = headerButtons.get(3);
        applyBtn.setX(centerX - 130);
        applyBtn.setY(y);
        applyBtn.render(context, mouseX, mouseY, delta);

        ButtonWidget cancelBtn = headerButtons.get(4);
        cancelBtn.setX(centerX + 10);
        cancelBtn.setY(y);
        cancelBtn.render(context, mouseX, mouseY, delta);

        y += btnHeight + spacing;// move y down for any following content

        super.render(context, mouseX, mouseY, delta);

        context.fillGradient(0, 20, this.width, 30, 0xC0000000, 0x00000000);
        context.fillGradient(0, this.height-10, this.width, this.height, 0x00000000, 0xC0000000);
        this.searchBox.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollAmount -= verticalAmount * scrollStep;
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));
        return true;
    }

    private String cleanName(String filename) {
        return filename.replaceAll("\\.(jar|zip|json)$", "");
    }

    private void updateShaderpackButtons(String selected) {
        for (ButtonWidget btn : shaderButtons) {
            boolean isSelected = cleanName(selected).equals(btn.getMessage().getString());
            btn.active = !isSelected;
        }
    }

    private void updateResourcepackButtons(String selected) {
        for (ButtonWidget btn : resourceButtons) {
            boolean isSelected = cleanName(selected).equals(btn.getMessage().getString());
            btn.active = !isSelected;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean hasVisibleItemsInCategory(String category) {
        switch (category) {
            case "mods":
                for (ModToggle toggle : modToggles) {
                    if (toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        return true;
                    }
                }
                return false;
            case "resourcepacks":
                for (ModToggle toggle : resourceToggles) {
                    if (toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        return true;
                    }
                }
                return false;
            case "shaderpacks":
                for (ModToggle toggle : shaderToggles) {
                    if (toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }

    private void updateVisibleButtonsBasedOnSearch() {
        // Update mod buttons visibility
        for (int i = 0; i < modToggles.size(); i++) {
            ModToggle toggle = modToggles.get(i);
            boolean shouldShow = toggle.getDisplayName().toLowerCase().contains(searchQuery);

            modToggleButtons.get(i).visible = shouldShow;
            modResetButtons.get(i).visible = shouldShow;
            modMetadataButtons.get(i).visible = shouldShow;
        }

        // Update resource pack buttons visibility
        for (int i = 0; i < resourceToggles.size(); i++) {
            ModToggle toggle = resourceToggles.get(i);
            boolean shouldShow = toggle.getDisplayName().toLowerCase().contains(searchQuery);

            resourceToggleButtons.get(i).visible = shouldShow;
            resourceMetadataButtons.get(i).visible = shouldShow;
        }

        // Update shader pack buttons visibility
        for (int i = 0; i < shaderToggles.size(); i++) {
            ModToggle toggle = shaderToggles.get(i);
            boolean shouldShow = toggle.getDisplayName().toLowerCase().contains(searchQuery);

            shaderToggleButtons.get(i).visible = shouldShow;
            shaderMetadataButtons.get(i).visible = shouldShow;
        }

        headerButtons.get(0).visible = hasVisibleItemsInCategory("mods") || searchQuery.isEmpty(); // Mods header
        headerButtons.get(1).visible = hasVisibleItemsInCategory("resourcepacks") || searchQuery.isEmpty(); // Resource packs header
        headerButtons.get(2).visible = hasVisibleItemsInCategory("shaderpacks") || searchQuery.isEmpty(); // Shader packs header
    }

    public void updateVisibleButtons(String type){
        if (type.equals("mods")){
            for (int i = 0; i < modToggles.size(); i++) {
                ModToggle toggle = modToggles.get(i);
                ButtonWidget btn = modToggleButtons.get(i);
                btn.setMessage(toggle.getButtonText());
                btn.active = true;

            }
        } else if (type.equals("resourcepacks")){
            for (int i = 0; i < resourceToggles.size(); i++) {
                ModToggle toggle = resourceToggles.get(i);
                ButtonWidget btn = resourceToggleButtons.get(i);
                btn.setMessage(toggle.getButtonText());
                btn.active = true;

            }
        } else if (type.equals("shaderpacks")){
            for (int i = 0; i < shaderToggles.size(); i++) {
                ModToggle toggle = shaderToggles.get(i);
                ButtonWidget btn = shaderToggleButtons.get(i);
                btn.setMessage(toggle.getButtonText());
                btn.active = true;
            }
        } else if (type.equals("headers")){
            for (int i = 0; i < headerButtons.size(); i++) {
                ButtonWidget btn = headerButtons.get(i);
                if (!(btn.active == true)){btn.active = false;}
                else if (btn.active == true){btn.active = true;}

            }
        }

        if (type.equals("all")){
            updateVisibleButtons("mods");
            updateVisibleButtons("resourcepacks");
            updateVisibleButtons("shaderpacks");
            updateVisibleButtons("headers");
        }
    }

}