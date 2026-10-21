package net.guag.simplemodmanager.modguard.general.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class CustomFacetButton extends Button {

    private boolean toggled;
    private final String name;
    private final String content;
    private final String displayName;

    public CustomFacetButton(int x, int y, int size, boolean initialState, String n, String c, String dn) {
        super(
                x,
                y,
                size,
                size,
                Component.literal(""),
                button -> ((CustomFacetButton) button).setOppositeToggled(),
                DEFAULT_NARRATION
        );

        this.toggled = initialState;
        this.name = n;
        this.content = c;
        this.displayName = dn;
    }

    @Override
    protected void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int fillColor = toggled ? 0xFF8c8f8d : 0x00000000;

        // Main square
        graphics.fill(x+2, y+2, x + w-2, y + h-2, fillColor);

        // Border (1px white outline)
        graphics.fill(x, y, x + w, y + 1, 0xFF8c8f8d);           // top
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF8c8f8d);   // bottom
        graphics.fill(x, y, x + 1, y + h, 0xFF8c8f8d);           // left
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF8c8f8d);   // right

        graphics.text(Minecraft.getInstance().font, Component.literal(this.displayName), x+w+5, y+1, 0xFF8c8f8d);

    }

    //Helper methods
    public void setOppositeToggled() { this.toggled = !this.toggled; }

    public boolean isToggled() { return toggled; }

    public String name(){ return this.name; }
    public String content(){ return this.content; }
    public String displayName(){ return this.displayName; }

}