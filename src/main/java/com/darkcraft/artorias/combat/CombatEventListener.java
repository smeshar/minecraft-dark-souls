package com.darkcraft.artorias.combat;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;

public class CombatEventListener {
    public static void register() {
        // Intercept damage to check for Dodge Roll i-frames and Ground Slam fall immunity
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (DodgeRollManager.hasIFrames(player)) {
                    // Attack passed through dodge i-frames unharmed!
                    return false;
                }
                if (GroundSlamManager.hasFallImmunity(player) && source.isOf(DamageTypes.FALL)) {
                    // Ground Slam impact negates landing fall damage!
                    return false;
                }
            }
            return true;
        });

        // Tick combat abilities timers for all players
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                DodgeRollManager.tick(player);
                GroundSlamManager.tick(player);
            }
        });
    }
}
