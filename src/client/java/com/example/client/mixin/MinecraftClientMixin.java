package com.example.client.mixin;

import com.example.client.systems.ui.ValorateMainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    public net.minecraft.client.gui.screens.Screen screen;

    @Unique
    private boolean valorate$replacedTitleScreen = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void valorate$replaceTitleScreen(CallbackInfo ci) {
        if (this.screen instanceof TitleScreen && !valorate$replacedTitleScreen) {
            valorate$replacedTitleScreen = true;
            Minecraft.getInstance().setScreen(new ValorateMainMenu());
        }

        if (!(this.screen instanceof TitleScreen) && !(this.screen instanceof ValorateMainMenu)) {
            valorate$replacedTitleScreen = false;
        }
    }
}