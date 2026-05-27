package com.example.client.mixin;

import com.example.client.systems.modules.miscellaneous.RenameModule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void valorate$rename(CallbackInfoReturnable<Component> cir) {

        Player player = (Player)(Object)this;

        String renamed = RenameModule.getRenamed(
                player.getName().getString()
        );

        if (renamed != null) {
            cir.setReturnValue(Component.literal(renamed));
        }
    }
}