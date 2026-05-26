package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;

public class Nametags extends Module {

    public static Nametags INSTANCE;

    public Nametags() {
        super("Nametags", Category.RENDER,false);
        INSTANCE = this;
    }
}
