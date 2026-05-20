package com.example.client;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

public class ExampleModClient implements ClientModInitializer {
    private static final String MOD_ID = "modid";

    private static final Component MAIN_MENU_LABEL = Component.empty()
            .append(Component.literal("Valorate").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" "))
            .append(Component.literal("client").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" by "))
            .append(Component.literal("Chris").withStyle(ChatFormatting.LIGHT_PURPLE))
            .append(Component.literal(" & "))
            .append(Component.literal("Maxi").withStyle(ChatFormatting.LIGHT_PURPLE));

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "valorate_watermark"),
                ExampleModClient::renderInGameWatermark
        );
    }

    private static void renderInGameWatermark(GuiGraphicsExtractor extractor, net.minecraft.client.DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.options.hideGui) {
            return;
        }

        MutableComponent watermarkText = Component.empty()
                .append(Component.literal("Valorate").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" "))
                .append(Component.literal("[v" + resolveModVersion() + "]"));

        Window window = client.getWindow();

        int textWidth = client.font.width(watermarkText);
        int x = window.getGuiScaledWidth() - textWidth - 8;
        int y = 8;

        extractor.text(client.font, watermarkText, x, y, 0xFFFFFFFF, true);
        extractor.text(client.font, "FPS: " + client.getFps(), x, y + 12, 0xFFFFFFFF, true);
    }

    private static String resolveModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                .orElse("1");
    }
}