package net.guag.simplemodmanager.widgets;

import net.guag.simplemodmanager.util.DrawingUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class CustomMenuWidget extends Button {
    public CustomMenuWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));

        Identifier logoID = Identifier.fromNamespaceAndPath("simplemodmanager", "textures/gui/render-icon.png");
        DrawingUtils.renderImage(logoID, graphics, getX(), getY(), 20);
    }

}
