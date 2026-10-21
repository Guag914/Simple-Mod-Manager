package net.guag.simplemodmanager.screen;

import net.guag.simplemodmanager.modguard.modrinth.api.ModrinthFetch;
import net.guag.simplemodmanager.modguard.modrinth.api.ModrinthRequest;
import net.guag.simplemodmanager.modguard.general.ProjectType;
import net.guag.simplemodmanager.modguard.general.buttons.CustomFacetButton;
import net.guag.simplemodmanager.modguard.general.buttons.CustomProjectWidget;
import net.guag.simplemodmanager.modguard.general.services.Parser;
import net.guag.simplemodmanager.widgets.CustomTextWidget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class InstallerScreen extends Screen {

    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");

    // layout
    private static final int LEFT = 10;
    private static final int FACET_W = 150;
    private static final int LIST_X = LEFT + FACET_W + 10;
    private static final int LIST_Y = 70;
    private static final int SPACING = 8;
    private static final int FACET_SIZE = 11;
    private static final int CARD_H = 72;

    private final Minecraft mc;
    private final ModrinthFetch fetcher = new ModrinthFetch();

    // one of each per tab
    private final ArrayList<CustomProjectWidget> modProjects = new ArrayList<>();
    private final ArrayList<CustomProjectWidget> resourceProjects = new ArrayList<>();
    private final ArrayList<CustomProjectWidget> shaderProjects = new ArrayList<>();

    private final ArrayList<CustomFacetButton> modFacets = new ArrayList<>();
    private final ArrayList<CustomFacetButton> resourceFacets = new ArrayList<>();
    private final ArrayList<CustomFacetButton> shaderFacets = new ArrayList<>();

    private final ModrinthRequest modRequest = new ModrinthRequest(ProjectType.MOD);
    private final ModrinthRequest resourceRequest = new ModrinthRequest(ProjectType.RESOURCEPACK);
    private final ModrinthRequest shaderRequest = new ModrinthRequest(ProjectType.SHADERPACK);

    private final List<Button> tabButtons = new ArrayList<>();
    private EditBox searchBox;
    private String searchQuery = "";

    private int activeTab = 0; // 0 = mods, 1 = resource packs, 2 = shaders
    private double scroll = 0;

    // used to know when a request needs to be re-sent / to drop stale responses
    private final String[] lastKeys = new String[3];
    private int fetchId = 0;

    public InstallerScreen(Minecraft mc) {
        super(Component.literal("Installer"));
        this.mc = mc;

        // NOTE: ModrinthRequest keeps facets in a Map<name, value>, so use one value per facet name
        modFacets.add(new CustomFacetButton(0, 0, FACET_SIZE, false, "categories", "optimization", "Optimization"));
        modFacets.add(new CustomFacetButton(0, 0, FACET_SIZE, false, "open_source", "true", "Open source"));

        resourceFacets.add(new CustomFacetButton(0, 0, FACET_SIZE, false, "categories", "vanilla-like", "Vanilla-like"));
        resourceFacets.add(new CustomFacetButton(0, 0, FACET_SIZE, false, "open_source", "true", "Open source"));

        shaderFacets.add(new CustomFacetButton(0, 0, FACET_SIZE, false, "categories", "performance", "Performance"));
        shaderFacets.add(new CustomFacetButton(0, 0, FACET_SIZE, false, "open_source", "true", "Open source"));

        setTab(0);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        tabButtons.clear();

        // search bar
        this.searchBox = new CustomTextWidget(this.font, this.width - 20, 20, Component.literal("Search"));
        this.searchBox.setX(10);
        this.searchBox.setY(10);
        this.searchBox.setValue(searchQuery);
        this.searchBox.setResponder(query -> this.searchQuery = query);
        this.searchBox.setMaxLength(100);
        this.addWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        // tab buttons
        int tabSpacing = 4;
        int tabW = (this.width - 20 - tabSpacing * 2) / 3;
        String[] names = {"Mods", "Resource Packs", "Shaders"};
        for (int i = 0; i < 3; i++) {
            int tab = i;
            Button b = Button.builder(Component.literal(names[i]), _ -> setTab(tab))
                    .bounds(10 + i * (tabW + tabSpacing), 36, tabW, 20).build();
            tabButtons.add(b);
            addWidget(b);
        }

        for (CustomFacetButton f : modFacets) addWidget(f);
        for (CustomFacetButton f : resourceFacets) addWidget(f);
        for (CustomFacetButton f : shaderFacets) addWidget(f);
    }

    private void setTab(int tab) {
        activeTab = tab;
        scroll = 0;
        // only the active tab's facet buttons are visible/clickable
        for (CustomFacetButton f : modFacets) f.visible = tab == 0;
        for (CustomFacetButton f : resourceFacets) f.visible = tab == 1;
        for (CustomFacetButton f : shaderFacets) f.visible = tab == 2;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // universal elements
        g.fill(0, 0, this.width, this.height, 0xAA000000);
        searchBox.extractRenderState(g, mouseX, mouseY, delta);
        for (Button tab : tabButtons) tab.extractRenderState(g, mouseX, mouseY, delta);

        if (activeTab == 0) renderMods(g, mouseX, mouseY, delta);
        else if (activeTab == 1) renderResourcePacks(g, mouseX, mouseY, delta);
        else renderShaders(g, mouseX, mouseY, delta);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void renderMods(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        syncRequest(modFacets, modRequest, modProjects);

        // facet panel
        g.fill(LEFT, LIST_Y, LEFT + FACET_W, this.height - 10, 0x88111111);
        for (int i = 0; i < modFacets.size(); i++) {
            CustomFacetButton f = modFacets.get(i);
            f.setX(LEFT + 8);
            f.setY(LIST_Y + 8 + i * (FACET_SIZE + SPACING));
            f.extractRenderState(g, mouseX, mouseY, delta);
        }

        // project panel
        g.fill(LIST_X, LIST_Y, LIST_X + CARD_W() + 16, this.height - 10, 0x88111111);
        for (int i = 0; i < modProjects.size(); i++) {
            int y = LIST_Y + SPACING + i * (CARD_H + SPACING) - (int) scroll;
            if (y < LIST_Y || y + CARD_H > this.height - 10) continue;
            CustomProjectWidget w = modProjects.get(i);
            w.setX(LIST_X + 8);
            w.setY(y);
            w.extractRenderState(g, mouseX, mouseY, delta);
        }
    }

    private void renderResourcePacks(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        syncRequest(resourceFacets, resourceRequest, resourceProjects);

        g.fill(LEFT, LIST_Y, LEFT + FACET_W, this.height - 10, 0x88111111);
        for (int i = 0; i < resourceFacets.size(); i++) {
            CustomFacetButton f = resourceFacets.get(i);
            f.setX(LEFT + 8);
            f.setY(LIST_Y + 8 + i * (FACET_SIZE + SPACING));
            f.extractRenderState(g, mouseX, mouseY, delta);
        }

        g.fill(LIST_X, LIST_Y, LIST_X + CARD_W() + 16, this.height - 10, 0x88111111);
        for (int i = 0; i < resourceProjects.size(); i++) {
            int y = LIST_Y + SPACING + i * (CARD_H + SPACING) - (int) scroll;
            if (y < LIST_Y || y + CARD_H > this.height - 10) continue;
            CustomProjectWidget w = resourceProjects.get(i);
            w.setX(LIST_X + 8);
            w.setY(y);
            w.extractRenderState(g, mouseX, mouseY, delta);
        }
    }

    private void renderShaders(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        syncRequest(shaderFacets, shaderRequest, shaderProjects);

        g.fill(LEFT, LIST_Y, LEFT + FACET_W, this.height - 10, 0x88111111);
        for (int i = 0; i < shaderFacets.size(); i++) {
            CustomFacetButton f = shaderFacets.get(i);
            f.setX(LEFT + 8);
            f.setY(LIST_Y + 8 + i * (FACET_SIZE + SPACING));
            f.extractRenderState(g, mouseX, mouseY, delta);
        }

        g.fill(LIST_X, LIST_Y, LIST_X + CARD_W() + 16, this.height - 10, 0x88111111);
        for (int i = 0; i < shaderProjects.size(); i++) {
            int y = LIST_Y + SPACING + i * (CARD_H + SPACING) - (int) scroll;
            if (y < LIST_Y || y + CARD_H > this.height - 10) continue;
            CustomProjectWidget w = shaderProjects.get(i);
            w.setX(LIST_X + 8);
            w.setY(y);
            w.extractRenderState(g, mouseX, mouseY, delta);
        }
    }

    // project widgets are 380 wide (see CustomProjectWidget)
    private static int CARD_W() { return 380; }

    // adds the facets to the request depending on each facet's toggled boolean
    private void applyFacets(List<CustomFacetButton> facets, ModrinthRequest request) {
        request.clearFacets();
        for (CustomFacetButton f : facets) {
            if (f.isToggled()) request.putFacet(f.name(), f.content());
        }
    }

    // re-sends the request whenever the facets or search text changed (also covers first load / tab switch)
    private void syncRequest(List<CustomFacetButton> facets, ModrinthRequest request, ArrayList<CustomProjectWidget> projects) {
        StringBuilder key = new StringBuilder(searchQuery).append('|');
        for (CustomFacetButton f : facets) key.append(f.isToggled() ? '1' : '0');
        if (key.toString().equals(lastKeys[activeTab])) return; //if they are different return, otherwise resend to api
        lastKeys[activeTab] = key.toString();

        applyFacets(facets, request);
        request.setQuery(searchQuery);
        scroll = 0;

        int id = ++fetchId;
        Thread.startVirtualThread(() -> {
            try {
                ArrayList<CustomProjectWidget> parsed = Parser.parse(fetcher.fetchSearchResults(request));
                mc.execute(() -> {
                    if (id != fetchId || parsed == null) return; // stale response
                    projects.clear();
                    projects.addAll(parsed);
                });
            } catch (Exception e) {
                LOGGER.error("Modrinth search failed", e);
            }
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int count = switch (activeTab) {
            case 0 -> modProjects.size();
            case 1 -> resourceProjects.size();
            default -> shaderProjects.size();
        };
        double maxScroll = Math.max(0, count * (CARD_H + SPACING) + SPACING - (this.height - 10 - LIST_Y));
        scroll = Math.max(0, Math.min(scroll - verticalAmount * 15, maxScroll));
        return true;
    }
}