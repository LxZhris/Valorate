package com.example.client.systems.commands.commands;

import com.example.client.systems.commands.Command;
import com.example.client.systems.modules.miscellaneous.RenameModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class RenameCommand extends Command {

    public RenameCommand() {
        super("rename", "Renames players");
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();

        int start = args.length > 0 && args[0].equalsIgnoreCase("rename") ? 1 : 0;

        if (args.length - start < 2) {
            mc.player.sendSystemMessage(
                    Component.literal("Usage: .rename <original> <newName>")
            );
            return;
        }

        String original = args[start];

        StringBuilder builder = new StringBuilder();
        for (int i = start + 1; i < args.length; i++) {
            builder.append(args[i]);
            if (i < args.length - 1) builder.append(" ");
        }

        String newName = builder.toString();

        RenameModule.rename(original, newName);

        mc.player.sendSystemMessage(
                Component.literal("Renamed " + original + " to " + newName.replace("&", "§"))
        );
    }
}