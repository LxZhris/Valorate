package com.example.client.systems.modules.combat.KillAura;


import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.combat.KillAura.modes.KillAuraMode;
import com.example.client.systems.modules.combat.KillAura.modes.SingleMode;

public class KillAura extends Module {
    private KillAuraMode mode = new SingleMode();

    public KillAura() {
        super("KillAura", Category.COMBAT, false);
    }

    public KillAuraMode getMode() {
        return mode;
    }

    public void setMode(KillAuraMode mode) {
        this.mode = mode;
    }

    public void onTick() {
        if (!isEnabled()) return;

        mode.onTick();
    }
}