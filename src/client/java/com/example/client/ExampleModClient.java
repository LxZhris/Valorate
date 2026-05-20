package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ExampleModClient implements ClientModInitializer {
	private static final String MOD_ID = "modid";
	private static final Text MAIN_MENU_LABEL = Text.empty()
		.append(Text.literal("Valorate").formatted(Formatting.YELLOW))
		.append(Text.literal(" "))
		.append(Text.literal("client").formatted(Formatting.AQUA))
		.append(Text.literal(" by "))
		.append(Text.literal("Chris").formatted(Formatting.LIGHT_PURPLE))
		.append(Text.literal(" & "))
		.append(Text.literal("Maxi").formatted(Formatting.LIGHT_PURPLE));

	private static int currentFps;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> currentFps = MinecraftClient.getCurrentFps());

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof TitleScreen)) {
				return;
			}

			ScreenEvents.afterRender(screen).register(this::renderMainMenuLabel);
		});

		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || client.options.hudHidden) {
				return;
			}

			renderInGameWatermark(drawContext, client);
		});
	}

	private void renderMainMenuLabel(net.minecraft.client.gui.screen.Screen screen, DrawContext drawContext, int mouseX, int mouseY, float tickDelta) {
		int textWidth = screen.getTextRenderer().getWidth(MAIN_MENU_LABEL);
		int x = screen.width - textWidth - 8;
		int y = 8;
		drawContext.drawTextWithShadow(screen.getTextRenderer(), MAIN_MENU_LABEL, x, y, 0xFFFFFFFF);
	}

	private void renderInGameWatermark(DrawContext drawContext, MinecraftClient client) {
		Text versionText = Text.literal("[v" + resolveModVersion() + "]");
		Text watermarkText = Text.empty()
			.append(Text.literal("Valorate").formatted(Formatting.YELLOW))
			.append(Text.literal(" "))
			.append(versionText);

		int textWidth = client.textRenderer.getWidth(watermarkText);
		int x = drawContext.getScaledWindowWidth() - textWidth - 8;
		int y = 8;

		drawContext.drawTextWithShadow(client.textRenderer, watermarkText, x, y, 0xFFFFFFFF);
		drawContext.drawTextWithShadow(client.textRenderer, Text.literal("FPS: " + currentFps), x, y + 12, 0xFFFFFFFF);
	}

	private String resolveModVersion() {
		return FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
			.orElse("1");
	}
}
