package com.example.client.systems.modules;

import com.example.client.systems.modules.movement.Sprint;
import com.example.client.systems.modules.render.Watermark;
import com.example.client.systems.modules.render.Licht;
import com.example.client.systems.modules.render.HUD;
import com.example.client.systems.modules.render.ClickGUI;

import java.util.ArrayList;
import java.util.List;


public class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    public static final Watermark WATERMARK = new Watermark();
    public static final Licht LICHT = new Licht();
    public static final HUD HUD = new HUD();
    public static final Sprint SPRINT = new Sprint();




    public static void init() {
        MODULES.add(LICHT);
        MODULES.add(HUD);
        MODULES.add(CLICK_GUI);
        MODULES.add(SPRINT);

    }

    public static Module getModuleByName(String name) {
        for (Module module : MODULES) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }

        return null;
    }

    public static final ClickGUI CLICK_GUI = new ClickGUI();

}