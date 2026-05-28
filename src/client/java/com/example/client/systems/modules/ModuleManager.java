package com.example.client.systems.modules;

import com.example.client.systems.modules.movement.*;
import com.example.client.systems.modules.render.*;
import com.example.client.systems.modules.combat.Killaura; // Import Killaura
import net.minecraft.world.item.Item;


import javax.naming.Name;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    public static final HUD HUD = new HUD();
    public static final Licht LICHT = new Licht();
    public static final ClickGUI CLICK_GUI = new ClickGUI();
    public static final Sprint SPRINT = new Sprint();
    public static final Step STEP = new Step();
    public static final Strafe STRAFE = new Strafe();
    public static final TargetStrafe TARGET_STRAFE = new TargetStrafe(); // Added TargetStrafe
    public static final Jesus JESUS = new Jesus();
    public static final xRay XRAY = new xRay();
    public static final Killaura KILLAURA = new Killaura();
    public static final NoFall NO_FALL = new NoFall();
    public static final ClickTP CLICK_TP = new ClickTP();
    public static final Notifications NOTIFICATIONS = new Notifications();

    public static void init() {
        MODULES.clear();

        MODULES.add(HUD);
        MODULES.add(LICHT);
        MODULES.add(CLICK_GUI);
        MODULES.add(SPRINT);
        MODULES.add(STEP);
        MODULES.add(JESUS);
        MODULES.add(XRAY);
        MODULES.add(STRAFE);
        MODULES.add(TARGET_STRAFE); // Added TargetStrafe to the list
        MODULES.add(KILLAURA); // Add Killaura to the list
        MODULES.add(NO_FALL);
        MODULES.add(CLICK_TP);
        MODULES.add(NOTIFICATIONS);
    }

    public static Module getModuleByName(String name) {
        for (Module module : MODULES) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }

        return null;
    }
}