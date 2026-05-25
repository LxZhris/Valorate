package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import net.minecraft.client.Minecraft;

public class Licht extends Module {

    public Licht() {
        super("Licht", Category.RENDER, false);
    }

    public void onTick() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        if (isEnabled()) {
            client.player.addEffect(
                    new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.NIGHT_VISION,
                            999999,
                            0,
                            false,
                            false
                    )
            );
        } else {
            client.player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        }
    }
}