package com.darkcraft.artorias.client;

import com.darkcraft.artorias.ArtoriasMod;
import com.darkcraft.artorias.combat.DodgeRollManager;
import com.darkcraft.artorias.entity.artorias.ArtoriasModel;
import com.darkcraft.artorias.entity.artorias.ArtoriasRenderer;
import com.darkcraft.artorias.entity.goblin.GoblinModel;
import com.darkcraft.artorias.entity.goblin.GoblinRenderer;
import com.darkcraft.artorias.entity.margit.MargitDaggerRenderer;
import com.darkcraft.artorias.entity.margit.MargitRenderer;
import com.darkcraft.artorias.entity.omen.OmenModel;
import com.darkcraft.artorias.entity.omen.OmenRenderer;
import com.darkcraft.artorias.entity.warg.WargModel;
import com.darkcraft.artorias.entity.warg.WargRenderer;
import com.darkcraft.artorias.network.ModMessages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ArtoriasClientMod implements ClientModInitializer {
    public static KeyBinding rollKeyBinding;
    private static int clientRollCooldown = 0;

    public static int getRollCooldown() {
        return clientRollCooldown;
    }

    @Override
    public void onInitializeClient() {
        // Register Artorias Custom Model Layer and Renderer
        EntityModelLayerRegistry.registerModelLayer(ArtoriasRenderer.ARTORIAS_LAYER, ArtoriasModel::getTexturedModelData);
        EntityRendererRegistry.register(ArtoriasMod.ARTORIAS_ENTITY, ArtoriasRenderer::new);

        // Register Warg Model Layer and Renderers
        EntityModelLayerRegistry.registerModelLayer(WargRenderer.WARG_LAYER, WargModel::getTexturedModelData);
        EntityRendererRegistry.register(ArtoriasMod.GIANT_WARG_ENTITY, WargRenderer::new);
        EntityRendererRegistry.register(ArtoriasMod.LESSER_WARG_ENTITY, WargRenderer::new);

        // Register Omen Model Layer and Renderers
        EntityModelLayerRegistry.registerModelLayer(OmenRenderer.OMEN_LAYER, OmenModel::getTexturedModelData);
        EntityRendererRegistry.register(ArtoriasMod.OMEN_CLEAVER_ENTITY, OmenRenderer::new);
        EntityRendererRegistry.register(ArtoriasMod.OMEN_BRAWLER_ENTITY, OmenRenderer::new);

        // Register Goblin Model Layer and Renderers
        EntityModelLayerRegistry.registerModelLayer(GoblinRenderer.GOBLIN_LAYER, GoblinModel::getTexturedModelData);
        EntityRendererRegistry.register(ArtoriasMod.GOBLIN_ENTITY, GoblinRenderer::new);
        EntityRendererRegistry.register(ArtoriasMod.GOBLIN_KNIGHT_ENTITY, GoblinRenderer::new);

        // Register Margit, the Fell Omen GeckoLib Boss & Dagger Renderers
        EntityRendererRegistry.register(ArtoriasMod.MARGIT_ENTITY, MargitRenderer::new);
        EntityRendererRegistry.register(ArtoriasMod.MARGIT_DAGGER_ENTITY, MargitDaggerRenderer::new);

        // Register Ground Slam feature (keybindings, networking, HUD cooldown)
        GroundSlamClient.initClient();

        // Register Dodge Roll Keybinding (Default: Left Alt)
        rollKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.artorias.dodge_roll",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                KeyBinding.Category.MOVEMENT
        ));

        // Listen for client tick events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Tick Ground Slam
            GroundSlamClient.onClientTick(client);

            // Tick Dodge Roll
            if (clientRollCooldown > 0) {
                clientRollCooldown--;
            }

            if (client.player == null) return;

            while (rollKeyBinding.wasPressed()) {
                if (clientRollCooldown > 0
                        || !client.player.isAlive()
                        || client.player.isSpectator()
                        || client.player.hasVehicle()
                        || client.player.getHungerManager().getFoodLevel() <= 2) {
                    continue;
                }

                double inputX = 0;
                double inputZ = 0;

                if (client.player.input != null) {
                    Vec2f moveVec = client.player.input.getMovementInput();
                    inputX = moveVec.x;
                    inputZ = moveVec.y;
                }

                // Instant client-side physics prediction for butter-smooth dodge
                Vec3d look = client.player.getRotationVec(1.0f);
                Vec3d forward = new Vec3d(look.x, 0, look.z).normalize();
                Vec3d left = new Vec3d(forward.z, 0, -forward.x);

                Vec3d rollDirection;
                if (Math.abs(inputX) < 0.01 && Math.abs(inputZ) < 0.01) {
                    // Backstep
                    rollDirection = forward.multiply(-0.95);
                } else {
                    rollDirection = forward.multiply(inputZ).add(left.multiply(inputX)).normalize();
                }

                double horizontalPower = DodgeRollManager.ROLL_HORIZONTAL_POWER;
                double verticalHop = DodgeRollManager.ROLL_VERTICAL_HOP;
                client.player.setVelocity(
                        rollDirection.x * horizontalPower,
                        verticalHop,
                        rollDirection.z * horizontalPower
                );
                client.player.velocityDirty = true;
                clientRollCooldown = 14;

                // Immediate client sound feedback (zero latency)
                client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.35f);
                client.player.playSound(net.minecraft.sound.SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value(), 0.75f, 1.25f);

                // Send Dodge Roll packet to server for validation, i-frames, and server sync
                if (ClientPlayNetworking.canSend(ModMessages.DodgeRollPayload.ID)) {
                    ClientPlayNetworking.send(new ModMessages.DodgeRollPayload(inputX, inputZ));
                }
            }
        });
    }
}
