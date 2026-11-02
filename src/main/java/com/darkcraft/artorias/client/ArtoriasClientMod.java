package com.darkcraft.artorias.client;

import com.darkcraft.artorias.ArtoriasMod;
import com.darkcraft.artorias.entity.ArtoriasRenderer;
import com.darkcraft.artorias.network.ModMessages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ArtoriasClientMod implements ClientModInitializer {
    public static KeyBinding rollKeyBinding;

    @Override
    public void onInitializeClient() {
        // Register Artorias Entity Renderer
        EntityRendererRegistry.register(ArtoriasMod.ARTORIAS_ENTITY, ArtoriasRenderer::new);

        // Register Dodge Roll Keybinding (Default: Left Alt)
        rollKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.artorias.dodge_roll",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                KeyBinding.Category.MOVEMENT
        ));

        // Listen for client roll input
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (rollKeyBinding.wasPressed()) {
                double inputX = 0;
                double inputZ = 0;

                if (client.player.input != null) {
                    Vec2f moveVec = client.player.input.getMovementInput();
                    inputX = moveVec.x;
                    inputZ = moveVec.y;
                }

                // Send Dodge Roll packet to server
                if (ClientPlayNetworking.canSend(ModMessages.DodgeRollPayload.ID)) {
                    ClientPlayNetworking.send(new ModMessages.DodgeRollPayload(inputX, inputZ));
                }
            }
        });
    }
}
