package com.example.client.systems.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ChatUtils {

    public static MutableComponent prefix() {
        return Component.empty()
                .append(Component.literal("Valorate")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(": ")
                        .withStyle(ChatFormatting.GRAY));
    }

    public static void send(Component message) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(
                Component.empty()
                        .append(prefix())
                        .append(message)
        );
    }

    public static void info(Component message) {
        send(message);
    }

    public static void error(String message) {
        send(Component.literal(message).withStyle(ChatFormatting.RED));
    }
}