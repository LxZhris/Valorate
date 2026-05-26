package com.example.client.systems.modules;

import com.example.client.systems.modules.movement.Sprint;
import com.example.client.systems.modules.movement.Step;
import com.example.client.systems.modules.render.*;
import com.example.client.systems.modules.movement.Jesus;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    public static final HUD HUD = new HUD();
    public static final Licht LICHT = new Licht();
    public static final ClickGUI CLICK_GUI = new ClickGUI();
    public static final Sprint SPRINT = new Sprint();
    public static final Step STEP = new Step();
    public static final Jesus JESUS = new Jesus();
    public static final xRay XRAY = new xRay();

    public static void init() {
        MODULES.clear();

        MODULES.add(HUD);
        MODULES.add(LICHT);
        MODULES.add(CLICK_GUI);
        MODULES.add(SPRINT);
        MODULES.add(STEP);
        MODULES.add(JESUS);
        MODULES.add(XRAY);
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