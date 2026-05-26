package com.example.client.systems.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import com.example.client.systems.modules.render.Nametags;

public class NametagRenderer {

    public static void render(PoseStack poseStack) {
        if (Nametags.INSTANCE == null || !Nametags.INSTANCE.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.isInvisible()) continue;

            double distance = mc.player.distanceTo(player);

            double x = player.getX() - mc.gameRenderer.getMainCamera().position().x;
            double y = player.getY() + mc.gameRenderer.getMainCamera().position().y;
            double z = player.getZ() - mc.gameRenderer.getMainCamera().position().z;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());

            float scale = getScale(distance);
            poseStack.scale(-scale, -scale, scale);

            float health = player.getHealth();
            float absorption = player.getAbsorptionAmount();

            String hpColor = absorption > 0 ? ChatFormatting.GOLD.toString() : ChatFormatting.GREEN.toString();

            String text =
                    ChatFormatting.YELLOW + player.getName().getString()
                            + ChatFormatting.GRAY + " | "
                            + hpColor + Math.round(health + absorption) + "❤"
                            + ChatFormatting.GRAY + " | "
                            + ChatFormatting.AQUA + Math.round(distance) + "m";

            drawText(mc, poseStack, buffer, text, -mc.font.width(text) / 2, 0, 0xFFFFFFFF);

            int yOff = 11;

            drawText(mc, poseStack, buffer, "Main: " + itemName(player.getMainHandItem()), -60, yOff, 0xFFDADADA);
            yOff += 9;

            drawText(mc, poseStack, buffer, "Off: " + itemName(player.getOffhandItem()), -60, yOff, 0xFFDADADA);
            yOff += 9;

            int armorX = -60;

            EquipmentSlot[] armorSlots = {
                    EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.FEET
            };

            for (EquipmentSlot slot : armorSlots) {

                ItemStack armorStack = player.getItemBySlot(slot);

                if (!armorStack.isEmpty()) {

                    drawText(
                            mc,
                            poseStack,
                            buffer,
                            itemName(armorStack),
                            armorX,
                            yOff,
                            armorStack.isEnchanted()
                                    ? 0xFF55FFFF
                                    : 0xFFDADADA
                    );

                    armorX += mc.font.width(itemName(armorStack)) + 6;
                }
            }

            yOff += 9;

            String effects = getEffects(player);
            if (!effects.isEmpty()) {
                drawText(mc, poseStack, buffer, effects, -mc.font.width(effects) / 2, yOff, 0xFFAAAAAA);
            }

            buffer.endBatch();
            poseStack.popPose();
        }
    }

    private static void drawText(Minecraft mc, PoseStack poseStack, MultiBufferSource.BufferSource buffer, String text, int x, int y, int color) {
        mc.font.drawInBatch(
                Component.literal(text),
                x,
                y,
                color,
                false,
                poseStack.last().pose(),
                buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0,
                15728880
        );
    }

    private static float getScale(double distance) {
        float scale = 0.025F + (float) distance * 0.0015F;

        if (scale < 0.025F) scale = 0.025F;
        if (scale > 0.07F) scale = 0.07F;

        return scale;
    }

    private static String itemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "None";

        String name = stack.getHoverName().getString();

        if (name.length() > 10) {
            name = name.substring(0, 10);
        }

        if (stack.isEnchanted()) {
            name += "*";
        }

        return name;
    }

    private static String getEffects(Player player) {
        StringBuilder builder = new StringBuilder();

        for (MobEffectInstance effect : player.getActiveEffects()) {
            String name = effect.getEffect().value().getDisplayName().getString();

            if (name.length() > 5) {
                name = name.substring(0, 5);
            }

            builder.append(name).append(" ");
        }

        return builder.toString();
    }
}