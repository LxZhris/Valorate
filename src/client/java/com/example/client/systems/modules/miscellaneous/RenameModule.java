package com.example.client.systems.modules.miscellaneous;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;

import java.util.HashMap;
import java.util.Map;

public class RenameModule extends Module {

    public static final Map<String, String> RENAMES = new HashMap<>();

    public RenameModule() {
        super("Rename", Category.MISCELLANEOUS, false,
                "Renames players clientside");
    }

    public static void rename(String originalName, String newName) {
        if (originalName == null || newName == null) return;

        RENAMES.put(
                originalName.toLowerCase(),
                newName.replace("&", "§")
        );
    }

    public static void reset(String originalName) {
        if (originalName == null) return;

        RENAMES.remove(originalName.toLowerCase());
    }

    public static String getRenamed(String originalName) {
        if (originalName == null) return null;

        return RENAMES.get(originalName.toLowerCase());
    }

    public static boolean hasRename(String originalName) {
        return getRenamed(originalName) != null;
    }
}