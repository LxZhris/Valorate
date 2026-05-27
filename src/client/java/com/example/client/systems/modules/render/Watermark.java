package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;

public class Watermark extends Module {
    public Watermark() {
        super("Watermark", Category.RENDER, false,
                "Displays the client watermark");        setVisibleInArraylist(false);
    }
}