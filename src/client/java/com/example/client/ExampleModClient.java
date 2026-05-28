package com.example.client;

import com.example.client.systems.commands.CommandManager;
import com.example.client.systems.config.ConfigManager;
import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import com.example.client.systems.modules.render.ArraylistRenderer;
import com.example.client.systems.modules.render.NotificationRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

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
                if (module.getKey() == 0) continue;

                boolean pressed = GLFW.glfwGetKey(window, module.getKey()) == GLFW.GLFW_PRESS;

                if (pressed && !module.isKeyPressed()) {
                    module.toggle();
                    ConfigManager.save();
                    NotificationRenderer.add(module, module.isEnabled());

                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal(
                                module.getName() + " " + (module.isEnabled() ? "enabled" : "disabled")
                        ));
                    }
                }

                module.setKeyPressed(pressed);
            }
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> setWindowIcon());

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "valorate_watermark"),
                ExampleModClient::renderInGameWatermark
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GLFW.glfwSetWindowTitle(
                    GLFW.glfwGetCurrentContext(),
                    "SS - Valorate Client [v" + resolveModVersion() + "]"
            );
        });
    }

    private static void setWindowIcon() {
        try {
            InputStream stream16 = ExampleModClient.class.getResourceAsStream("/icons/icon_16x16.png");
            InputStream stream32 = ExampleModClient.class.getResourceAsStream("/icons/icon_32x32.png");

            if (stream16 == null || stream32 == null) {
                System.out.println("[Valorate] Window icons not found.");
                return;
            }

            BufferedImage image16 = ImageIO.read(stream16);
            BufferedImage image32 = ImageIO.read(stream32);

            ByteBuffer buffer16 = imageToBuffer(image16);
            ByteBuffer buffer32 = imageToBuffer(image32);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                GLFWImage.Buffer icons = GLFWImage.malloc(2, stack);

                icons.position(0);
                icons.width(image16.getWidth());
                icons.height(image16.getHeight());
                icons.pixels(buffer16);

                icons.position(1);
                icons.width(image32.getWidth());
                icons.height(image32.getHeight());
                icons.pixels(buffer32);

                icons.position(0);

                long window = GLFW.glfwGetCurrentContext();
                GLFW.glfwSetWindowIcon(window, icons);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ByteBuffer imageToBuffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];

                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }

        buffer.flip();
        return buffer;
    }

    private static void renderInGameWatermark(GuiGraphicsExtractor extractor, net.minecraft.client.DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.options.hideGui) return;

        ArraylistRenderer.render(extractor);
        NotificationRenderer.render(extractor);

        if (ModuleManager.CROSSHAIR != null && ModuleManager.CROSSHAIR.isEnabled()) {
            String s = "§l" + ModuleManager.CROSSHAIR.getSymbol();

            int sw = client.getWindow().getGuiScaledWidth();
            int sh = client.getWindow().getGuiScaledHeight();

            int x = sw / 2 - client.font.width(s);
            int y = sh / 2 - 8;

            extractor.text(
                    client.font,
                    s,
                    x,
                    y,
                    0xFFFF2222,
                    ModuleManager.CROSSHAIR.hasShadow()
            );

            extractor.text(
                    client.font,
                    s,
                    x + 1,
                    y,
                    0xFFFF2222,
                    false
            );
        }

        if (!ModuleManager.HUD.isWatermark()) return;

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