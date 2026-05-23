package net.guag.simplemodmanager.mixin;

import net.guag.simplemodmanager.screen.ContentManagerScreen;
import net.guag.simplemodmanager.util.FileUtils;
import net.guag.simplemodmanager.widgets.CustomMenuWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PauseScreen.class)
public abstract class PauseWidget extends Screen {

    protected PauseWidget(Component title) { super(title); }

    @Unique
    CustomMenuWidget iconWidget = new CustomMenuWidget(0, 0, 20, 20, Component.literal(""), _ ->
            Minecraft.getInstance().setScreen(
                    new ContentManagerScreen(Minecraft.getInstance(),
                            FileUtils.getModToggles(),
                            FileUtils.getResourceToggles(),
                            FileUtils.getShaderToggles()
                    )
            ));

//    @Inject(method = "createPauseMenu", at = @At("RETURN"))
//    private void addOpenGUIButton(CallbackInfo ci){
//        addRenderableWidget(iconWidget);
//        iconWidget.setX(this.width / 2 + 103);
//        iconWidget.setY(this.height / 2);
//    }
}