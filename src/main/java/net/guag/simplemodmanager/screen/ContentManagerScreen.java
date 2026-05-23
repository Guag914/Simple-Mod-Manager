package net.guag.simplemodmanager.screen;

import net.guag.simplemodmanager.util.DrawingUtils;
import net.guag.simplemodmanager.widgets.CustomButtonWidget;
import net.guag.simplemodmanager.widgets.CustomTextWidget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

import java.util.*;


public class ContentManagerScreen extends Screen{

    private final Minecraft client;
    private final List<FileToggle> modToggles;
    private final List<FileToggle> resourceToggles;
    private final List<FileToggle> shaderToggles;

    DrawingUtils drawUtil = new DrawingUtils();

    // Scrolling state
    private double scrollAmount = 0;
    private double maxScroll = 0;

    private EditBox searchBox;
    private String searchQuery = "";

    private String activeTab = "mods"; // "mods", "resourcepacks", "shaderpacks"
    private final List<Button> tabButtons = new ArrayList<>();

    //Button Info
    int btnHeight = 20;

    private final List<Button> modToggleButtons = new ArrayList<>();
    private final List<Button> modResetButtons = new ArrayList<>();
    private final List<Button> modMetadataButtons = new ArrayList<>();
    private final Map<Button, String> tooltipMap = new HashMap<>();

    private final List<Button> resourceMetadataButtons = new ArrayList<>();
    private final List<Button> shaderMetadataButtons = new ArrayList<>();
    private final List<Button> resourceToggleButtons = new ArrayList<>();
    private final List<Button> shaderToggleButtons = new ArrayList<>();

    private final List<Button> headerButtons = new ArrayList<>();

    private static final ArrayList<String> dependencies = new ArrayList<>();

    public ContentManagerScreen(Minecraft client, List<FileToggle> modToggles, List<FileToggle> resourceToggles, List<FileToggle> shaderToggles) {
        super(Component.literal("Simple Mod Manager"));
        this.client = client;
        this.modToggles = modToggles;
        this.resourceToggles = resourceToggles;
        this.shaderToggles = shaderToggles;
    }

    @Override
    protected void init() {
        this.modToggleButtons.clear();
        this.modResetButtons.clear();
        this.modMetadataButtons.clear();

        this.headerButtons.clear();
        this.tabButtons.clear();

        this.resourceToggleButtons.clear();
        this.resourceMetadataButtons.clear();

        this.shaderToggleButtons.clear();
        this.shaderMetadataButtons.clear();

        this.tooltipMap.clear();
        this.clearWidgets();

        // Search bar
        this.searchBox = new CustomTextWidget(this.font, this.width - 20, 20, Component.literal("Search"));
        this.searchBox.setX(10);
        this.searchBox.setY(10);
        this.searchBox.setResponder(query -> this.searchQuery = query.toLowerCase());
        this.searchBox.setMaxLength(100);
        this.searchBox.setEditable(true);
        this.addWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        // Tab buttons
        int tabY = 42;
        int tabSpacing = 4;
        int tabW = (this.width - 20 - tabSpacing * 2) / 3; // 20 = 10 px from each edge
        int tabStartX = 10;
        Button modsTab = CustomButtonWidget.builder(Component.literal("Mods"), _ -> { activeTab = "mods"; updateScroll(); })
                .bounds(tabStartX, tabY, tabW, 18).build();
        Button resourcesTab = CustomButtonWidget.builder(Component.literal("Resource Packs"), _ -> { activeTab = "resourcepacks"; updateScroll(); })
                .bounds(tabStartX+tabW+tabSpacing, tabY, tabW, 18).build();
        Button shadersTab = CustomButtonWidget.builder(Component.literal("Shaders"), _ -> { activeTab = "shaderpacks"; updateScroll(); })
                .bounds(tabStartX+(tabW+tabSpacing) * 2, tabY, tabW, 18).build();

        tabButtons.add(modsTab);
        tabButtons.add(resourcesTab);
        tabButtons.add(shadersTab);
        addWidget(modsTab);
        addWidget(resourcesTab);
        addWidget(shadersTab);

        FileToggle.initializeDefaultDisabledMods();

        int index = 0;

        //get dependencies
        for (FileToggle toggle : modToggles) {
            for (String dependency : toggle.getDependencies()) {
                if (!dependencies.contains(dependency)) dependencies.add(dependency);
            }
        }

        //sort putting non-toggleable mods last
        modToggles.sort((a, b) -> {
            boolean aDep = dependencies.contains(a.getModId());
            boolean bDep = dependencies.contains(b.getModId());
            if (aDep != bDep) return Boolean.compare(aDep, bDep); // dependencies last
            return Boolean.compare(!a.isEnabled(), !b.isEnabled()); // disabled last within each group
        });

        for (FileToggle toggle : modToggles) {
            Button toggleFunc =  Button.builder(Component.literal(toggle.getButtonText().getString()), button -> {
                if (!dependencies.contains(toggle.getModId())){
                    toggle.toggled();
                    button.setMessage(Component.literal(toggle.getButtonText().getString()));
                } else {
                    button.setTooltip(Tooltip.create(Component.literal("§7Dependency")));
                }
            }).bounds(0, 0, 60, btnHeight).build();

            toggleFunc.active = !dependencies.contains(toggle.getModId()); //set toggle state depending on if it's a dependency
//            System.out.println(toggle.getModId() + (dependencies.contains(toggle.getModId()) ? " IS dependency " : " is NOT dependency"));
//            System.out.println(dependencies);

            addWidget(toggleFunc);
            modToggleButtons.add(toggleFunc);

            // capture before lambda
            Button resetFunc = Button.builder(Component.literal("Reset"), _ -> {
                if (!dependencies.contains(toggle.getModId())){
                    toggle.resetToDefault();
                    toggleFunc.setMessage(Component.literal(toggle.getButtonText().getString()));
                }
            }).bounds(0, 0, 50, btnHeight).build();
            resetFunc.active = !dependencies.contains(toggle.getModId());
            addWidget(resetFunc);
            modResetButtons.add(resetFunc);

            Button metadataFunc = Button.builder(
                    Component.literal(modToggles.get(index).getMetadataSummaryForMod()), _ -> {}
            ).bounds(0, 0, 0, btnHeight).build();
            metadataFunc.active = false;
            addWidget(metadataFunc);
            modMetadataButtons.add(metadataFunc);
            tooltipMap.put(metadataFunc, modToggles.get(index).getDescription());
            index++;
        }

        index = 0;
        for (FileToggle toggle : resourceToggles) {
            Button resourceMetadataFunc = Button.builder(
                    Component.literal(resourceToggles.get(index).getFile().getName()), _ -> {}
            ).bounds(0, 0, 0, btnHeight).build();
            resourceMetadataFunc.active = false;
            addWidget(resourceMetadataFunc);
            resourceMetadataButtons.add(resourceMetadataFunc);

            Button resourceToggleFunc = Button.builder(Component.literal(toggle.getButtonText().getString()), button -> {
                toggle.toggled();
                button.setMessage(Component.literal(toggle.getButtonText().getString()));
            }).bounds(0, 0, 60, btnHeight).build();
            addWidget(resourceToggleFunc);
            resourceToggleButtons.add(resourceToggleFunc);
            index++;
        }

        index = 0;
        for (FileToggle toggle : shaderToggles) {
            Button shaderMetadataFunc = Button.builder(
                    Component.literal(shaderToggles.get(index).getFile().getName()), _ -> {}
            ).bounds(0, 0, 0, btnHeight).build();
            shaderMetadataFunc.active = false;
            addWidget(shaderMetadataFunc);
            shaderMetadataButtons.add(shaderMetadataFunc);

            Button shaderToggleFunc = Button.builder(Component.literal(toggle.getButtonText().getString()), button -> {
                toggle.toggled();
                button.setMessage(Component.literal(toggle.getButtonText().getString()));
            }).bounds(0, 0, 60, btnHeight).build();
            addWidget(shaderToggleFunc);
            shaderToggleButtons.add(shaderToggleFunc);
            index++;
        }

        Button applyFunc = Button.builder(Component.literal("Apply Changes"), _ -> {
            for (FileToggle toggle : modToggles) toggle.applyChange("mod");
            for (FileToggle toggle : resourceToggles) toggle.applyChange("resourcepack"); //FileUtils.toggleResourcePack(toggle.getFile().getName(), toggle.isEnabled());
            for (FileToggle toggle : shaderToggles) toggle.applyChange("shaderpack"); //FileUtils.toggleShaderPack(toggle.getFile().getName(), toggle.isEnabled());

            client.setScreen(null);
            Minecraft.getInstance().reloadResourcePacks();
        }).bounds(0, 0, 120, btnHeight).build();
        headerButtons.add(applyFunc);
        addWidget(applyFunc);
        tooltipMap.put(applyFunc, "Restart the game to fully apply changes to mod settings.");

        Button cancelFunc = Button.builder(Component.literal("Cancel"), _ -> client.setScreen(null))
                .bounds(0, 0, 120, btnHeight).build();
        headerButtons.add(cancelFunc);
        addWidget(cancelFunc);

        updateScroll();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Render tab buttons
        for (Button tab : tabButtons) {
            tab.extractRenderState(context, mouseX, mouseY, delta);
        }

        updateVisibleButtonsBasedOnSearch();

        // Tooltip handling
        for (Map.Entry<Button, String> entry : tooltipMap.entrySet()) {
            Button button = entry.getKey();
            if (button.isHovered() && button.visible) {
                context.setComponentTooltipForNextFrame(
                        Minecraft.getInstance().font,
                        Collections.singletonList(Component.literal(entry.getValue())),
                        mouseX, mouseY
                );
                break;
            }
        }

        int listTop = 70;
        int listBottom = this.height - 40;



        int y = listTop + 16 - (int) scrollAmount;
        int padding = 20;
        int nameW = this.width - 260;
        int toggleX = this.width - 190;
        int resetX = this.width - 120;

        //button container fills first so buttons are on top
        context.fill(10, listTop, this.width - 10, listBottom, 0x88111111);


        y += 12; // below column headers

        switch (activeTab) {
            case "mods" -> {
                for (int i = 0; i < modToggles.size(); i++) {
                    FileToggle toggle = modToggles.get(i);

                    // always increment y and skip if filtered
                    if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        y += 25;
                        continue;
                    }

                    // skip rendering if out of bounds but still increment y
                    if (y + btnHeight < listTop + 20 || y > listBottom - 20) {
                        modToggleButtons.get(i).setX(Integer.MIN_VALUE);
                        modToggleButtons.get(i).setY(Integer.MIN_VALUE);
                        modResetButtons.get(i).setX(Integer.MIN_VALUE);
                        modResetButtons.get(i).setY(Integer.MIN_VALUE);
                        modMetadataButtons.get(i).setX(Integer.MIN_VALUE);
                        modMetadataButtons.get(i).setY(Integer.MIN_VALUE);
                        y += 25;
                        continue;
                    }

                    drawUtil.renderModIcon(toggle, context, padding, y, 20);

                    Button metadataBtn = modMetadataButtons.get(i);
                    metadataBtn.setX(padding + 22);
                    metadataBtn.setY(y);
                    metadataBtn.setWidth(nameW - 22);
                    metadataBtn.extractRenderState(context, mouseX, mouseY, delta);

                    Button toggleBtn = modToggleButtons.get(i);
                    toggleBtn.setX(toggleX);
                    toggleBtn.setY(y);
                    toggleBtn.extractRenderState(context, mouseX, mouseY, delta);

                    Button resetBtn = modResetButtons.get(i);
                    resetBtn.setX(resetX);
                    resetBtn.setY(y);
                    resetBtn.extractRenderState(context, mouseX, mouseY, delta);

                    y += 25;
                }
            }
            case "resourcepacks" -> {
                for (int i = 0; i < resourceToggles.size(); i++) {
                    FileToggle toggle = resourceToggles.get(i);

                    // always increment y and skip if filtered
                    if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        y += 25;
                        continue;
                    }

                    // skip rendering if out of bounds but still increment y
                    if (y + btnHeight < listTop + 20 || y > listBottom - 20) {
                        this.resourceToggleButtons.get(i).setX(Integer.MIN_VALUE);
                        this.resourceToggleButtons.get(i).setY(Integer.MIN_VALUE);
                        this.resourceMetadataButtons.get(i).setX(Integer.MIN_VALUE);
                        this.resourceMetadataButtons.get(i).setY(Integer.MIN_VALUE);

                        y += 25;
                        continue;
                    }

                    drawUtil.renderModIcon(toggle, context, padding, y, 20);

                    Button metadataBtn = resourceMetadataButtons.get(i);
                    metadataBtn.setX(padding + 22);
                    metadataBtn.setY(y);
                    metadataBtn.setWidth(nameW - 22);
                    metadataBtn.extractRenderState(context, mouseX, mouseY, delta);

                    Button toggleBtn = resourceToggleButtons.get(i);
                    toggleBtn.setX(toggleX);
                    toggleBtn.setY(y);
                    toggleBtn.extractRenderState(context, mouseX, mouseY, delta);

                    y += 25;
                }
            }
            case "shaderpacks" -> {
                for (int i = 0; i < shaderToggles.size(); i++) {
                    FileToggle toggle = shaderToggles.get(i);

                    // always increment y and skip if filtered
                    if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        y += 25;
                        continue;
                    }

                    // skip rendering if out of bounds but still increment y
                    if (y + btnHeight < listTop + 20 || y > listBottom - 20) {
                        this.shaderToggleButtons.get(i).setX(Integer.MIN_VALUE);
                        this.shaderToggleButtons.get(i).setY(Integer.MIN_VALUE);
                        this.shaderMetadataButtons.get(i).setX(Integer.MIN_VALUE);
                        this.shaderMetadataButtons.get(i).setY(Integer.MIN_VALUE);
                        y += 25;
                        continue;
                    }

                    drawUtil.renderModIcon(toggle, context, padding, y, 20);

                    Button metadataBtn = shaderMetadataButtons.get(i);
                    metadataBtn.setX(padding + 22);
                    metadataBtn.setY(y);
                    metadataBtn.setWidth(nameW - 22);
                    metadataBtn.extractRenderState(context, mouseX, mouseY, delta);

                    Button toggleBtn = shaderToggleButtons.get(i);
                    toggleBtn.setX(toggleX);
                    toggleBtn.setY(y);
                    toggleBtn.extractRenderState(context, mouseX, mouseY, delta);

                    y += 25;
                }
            }
        }

        // Bottom bar
        int bottomY = this.height - 26;
        context.fill(0, bottomY - 4, this.width, this.height, 0x88111111);
        context.fill(0, bottomY - 5, this.width, bottomY - 4, 0xFF505050);

        int totalBtnWidth = 120 + 4 + 120;
        int startX = (this.width - totalBtnWidth) / 2;

        Button applyBtn = headerButtons.getFirst();
        applyBtn.setX(startX);
        applyBtn.setY(bottomY);
        applyBtn.extractRenderState(context, mouseX, mouseY, delta);

        Button cancelBtn = headerButtons.get(1);
        cancelBtn.setX(startX + 124);
        cancelBtn.setY(bottomY);
        cancelBtn.extractRenderState(context, mouseX, mouseY, delta);

        this.searchBox.extractRenderState(context, mouseX, mouseY, delta);

        //Render button container outlines last so they are on top
        context.fill(10, listTop, this.width - 10, listTop + 1, 0xFF505050);
        context.fill(10, listBottom - 1, this.width - 10, listBottom, 0xFF505050);
        context.fill(10, listTop, 11, listBottom, 0xFF505050);
        context.fill(this.width - 11, listTop, this.width - 10, listBottom, 0xFF505050);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double scrollStep = 15;
        scrollAmount -= verticalAmount * scrollStep;
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));
        return true;
    }

    private void updateScroll() {
        scrollAmount = 0;
        int count = switch (activeTab) {
            case "mods" -> modToggles.size();
            case "resourcepacks" -> resourceToggles.size();
            case "shaderpacks" -> shaderToggles.size();
            default -> 0;
        };
        maxScroll = Math.max(0, (count * 25) + 120 - (this.height - 80));
    }

    private void updateVisibleButtonsBasedOnSearch() {
        for (int i = 0; i < modToggles.size(); i++) {
            boolean show = activeTab.equals("mods") && modToggles.get(i).getDisplayName().toLowerCase().contains(searchQuery);
            modToggleButtons.get(i).visible = show;
            modResetButtons.get(i).visible = show;
            modMetadataButtons.get(i).visible = show;
        }
        for (int i = 0; i < resourceToggles.size(); i++) {
            boolean show = activeTab.equals("resourcepacks") && resourceToggles.get(i).getDisplayName().toLowerCase().contains(searchQuery);
            resourceToggleButtons.get(i).visible = show;
            resourceMetadataButtons.get(i).visible = show;
        }
        for (int i = 0; i < shaderToggles.size(); i++) {
            boolean show = activeTab.equals("shaderpacks") && shaderToggles.get(i).getDisplayName().toLowerCase().contains(searchQuery);
            shaderToggleButtons.get(i).visible = show;
            shaderMetadataButtons.get(i).visible = show;
        }
    }
}