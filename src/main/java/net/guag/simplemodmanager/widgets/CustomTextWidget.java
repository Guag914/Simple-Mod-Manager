package net.guag.simplemodmanager.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

public class CustomTextWidget extends EditBox {
    public CustomTextWidget(Font font, int width, int height, Component narration) {
        super(font, width, height, narration);
    }

    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
    }
}
