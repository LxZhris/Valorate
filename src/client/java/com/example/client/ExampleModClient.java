package com.example.client;

import com.example.client.systems.commands.CommandManager;
import com.example.client.systems.config.ConfigManager;
import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import com.example.client.systems.modules.render.ArraylistRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {
    private static final String MOD_ID = "modid";

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        CommandManager.init();
        ConfigManager.load();

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith(".")) {
                CommandManager.execute(message);
                return false;
            }

            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Wichtig: alle Module ticken, damit Step/Sprint/Licht usw. funktionieren.
            for (Module module : ModuleManager.MODULES) {
                module.onTick();
            }

            if (client.screen != null) {
                for (Module module : ModuleManager.MODULES) {
                    module.setKeyPressed(false);
                }

                return;
            }

            long window = GLFW.glfwGetCurrentContext();

            for (Module module : ModuleManager.MODULES) {
                if (module.getKey() == 0) {
                    continue;
                }

                boolean pressed = GLFW.glfwGetKey(window, module.getKey()) == GLFW.GLFW_PRESS;

                if (pressed && !module.isKeyPressed()) {
                    module.toggle();
                    ConfigManager.save();

                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal(
                                module.getName() + " " + (module.isEnabled() ? "enabled" : "disabled")
                        ));
                    }
                }

                module.setKeyPressed(pressed);
            }
        });

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

        ArraylistRenderer.render(extractor);

        if (!ModuleManager.HUD.isWatermark()) {
            return;
        }

        MutableComponent watermarkText = Component.empty()
                .append(Component.literal("Valorate")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(" "))
                .append(Component.literal("[v" + resolveModVersion() + "]")
                        .withStyle(ChatFormatting.AQUA));

        int x = 8;
        int y = 8;

        extractor.text(client.font, watermarkText, x, y, 0xFFFFFFFF, true);
        extractor.text(client.font, "FPS:" + client.getFps(), x, y + 12, 0xFFFFFFFF, true);
    }

    private static String resolveModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                .orElse("1");
    }
}