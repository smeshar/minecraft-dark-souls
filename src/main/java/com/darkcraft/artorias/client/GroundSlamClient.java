package com.darkcraft.artorias.client;

import com.darkcraft.artorias.combat.DodgeRollManager;
import com.darkcraft.artorias.combat.GroundSlamManager;
import com.darkcraft.artorias.network.ModMessages;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class GroundSlamClient {
    public static KeyBinding slamKeyBinding;

    private static int clientCooldown = 0;
    private static boolean clientIsSlamming = false;
    private static int clientShakeTicks = 0;
    private static boolean wasCtrlDown = false;

    // Track other players' animation states (for multiplayer)
    private static final Map<Integer, Integer> SLAMMING_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> RECOVERY_ENTITIES = new ConcurrentHashMap<>();

    public static void initClient() {
        // Register keybinding defaulting to Left Control
        slamKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.artorias.ground_slam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                KeyBinding.Category.MOVEMENT
        ));

        // Register S2C Packet Listeners
        ClientPlayNetworking.registerGlobalReceiver(ModMessages.GroundSlamStartPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                SLAMMING_ENTITIES.put(payload.entityId(), 1);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModMessages.GroundSlamImpactPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                SLAMMING_ENTITIES.remove(payload.entityId());
                RECOVERY_ENTITIES.put(payload.entityId(), 8);

                MinecraftClient mc = context.client();
                if (mc.player != null && mc.player.getId() == payload.entityId()) {
                    // Trigger camera shake on the impacting player
                    clientShakeTicks = 5;
                }
            });
        });

        // Register HUD Cooldown Bar (renders both Roll and Slam symmetrical indicators)
        HudRenderCallback.EVENT.register(GroundSlamClient::renderHud);
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        ClientPlayerEntity player = client.player;

        // 1. Tick cooldown
        if (clientCooldown > 0) {
            clientCooldown--;
            if (clientCooldown == 0) {
                // Subtle Souls ability ready chime
                player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.65f, 1.75f);
            }
        }

        // 2. Camera shake on impact
        if (clientShakeTicks > 0) {
            float kick = (clientShakeTicks >= 4) ? 1.7f : -0.8f;
            player.setPitch(player.getPitch() + kick);
            clientShakeTicks--;
        }

        // 3. Tick remote players animation states
        for (Map.Entry<Integer, Integer> entry : SLAMMING_ENTITIES.entrySet()) {
            int ticks = entry.getValue() + 1;
            if (ticks > GroundSlamManager.MAX_AIR_TICKS) {
                SLAMMING_ENTITIES.remove(entry.getKey());
            } else {
                SLAMMING_ENTITIES.put(entry.getKey(), ticks);
            }
        }

        for (Map.Entry<Integer, Integer> entry : RECOVERY_ENTITIES.entrySet()) {
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                RECOVERY_ENTITIES.remove(entry.getKey());
            } else {
                RECOVERY_ENTITIES.put(entry.getKey(), ticks);
            }
        }

        // 4. Local Slamming descent physics & landing detection
        if (clientIsSlamming) {
            // Local visual trail
            player.getEntityWorld().addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.3, player.getZ(), 0.0, 0.05, 0.0);
            player.getEntityWorld().addParticleClient(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 0.35, player.getZ(), 0.0, 0.0, 0.0);

            // Maintain rapid plunge velocity
            if (player.getVelocity().y > -1.2) {
                player.setVelocity(player.getVelocity().x * 0.75, GroundSlamManager.DOWNWARD_SPEED, player.getVelocity().z * 0.75);
                player.velocityDirty = true;
            }

            // Impact check
            if (player.isOnGround()) {
                clientIsSlamming = false;
                clientShakeTicks = 5;
                player.swingHand(Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 0.85f);
                RECOVERY_ENTITIES.put(player.getId(), 8);
            }
        }

        // 5. Input Detection for Ground Slam (Ctrl in mid-air)
        boolean ctrlPressed = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean ctrlJustPressed = ctrlPressed && !wasCtrlDown;
        wasCtrlDown = ctrlPressed;

        boolean keyTriggered = slamKeyBinding.wasPressed() || ctrlJustPressed;

        if (keyTriggered) {
            if (clientCooldown <= 0
                    && !player.isOnGround()
                    && !player.isClimbing()
                    && !player.isSubmergedInWater()
                    && !player.getAbilities().flying
                    && player.isAlive()
                    && !player.isSpectator()
                    && !player.hasVehicle()
                    && player.getHungerManager().getFoodLevel() > 2) {

                // Start local prediction
                clientCooldown = GroundSlamManager.COOLDOWN_TICKS;
                clientIsSlamming = true;

                Vec3d currentVel = player.getVelocity();
                player.setVelocity(currentVel.x * 0.25, GroundSlamManager.DOWNWARD_SPEED, currentVel.z * 0.25);
                player.velocityDirty = true;

                player.playSound(SoundEvents.ITEM_MACE_SMASH_AIR, 1.35f, 0.95f);
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 0.65f);

                // Send request packet to server
                if (ClientPlayNetworking.canSend(ModMessages.GroundSlamPayload.ID)) {
                    ClientPlayNetworking.send(new ModMessages.GroundSlamPayload());
                }
            }
        }
    }

    public static boolean isSlamming(int entityId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getId() == entityId) {
            return clientIsSlamming;
        }
        return SLAMMING_ENTITIES.containsKey(entityId);
    }

    public static boolean isRecovering(int entityId) {
        return RECOVERY_ENTITIES.containsKey(entityId);
    }

    public static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.player.isSpectator()) {
            return;
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // 1. Symmetrical Left Indicator: Roll Ready
        renderRollHud(context, client, screenWidth, screenHeight);

        // 2. Symmetrical Right Indicator: Slam Ready
        renderSlamHud(context, client, screenWidth, screenHeight);
    }

    private static void renderRollHud(DrawContext context, MinecraftClient client, int screenWidth, int screenHeight) {
        int widgetWidth = 72;
        int widgetHeight = 16;

        // Position: symmetrically to the LEFT of the hotbar (hotbar starts at screenWidth / 2 - 91)
        int x = screenWidth / 2 - 98 - widgetWidth;
        int y = screenHeight - 20;

        if (x < 4) {
            x = 4;
        }

        // Background: obsidian slate
        context.fill(x, y, x + widgetWidth, y + widgetHeight, 0xD00A0D14);

        int rollCooldown = ArtoriasClientMod.getRollCooldown();

        if (client.player.getHungerManager().getFoodLevel() <= 2) {
            // Starving / exhausted: cannot roll
            context.fill(x + 2, y + 2, x + widgetWidth - 2, y + widgetHeight - 2, 0x40550000);
            drawBorder(context, x, y, widgetWidth, widgetHeight, 0xFF882222);

            String text = "NO STAMINA";
            int textX = x + (widgetWidth - client.textRenderer.getWidth(text)) / 2;
            int textY = y + 4;
            context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFF5555);
        } else if (rollCooldown > 0) {
            // ON ROLL COOLDOWN / ACTIVE ROLL
            float progress = 1.0f - ((float) rollCooldown / (float) DodgeRollManager.COOLDOWN_TICKS);
            int fillW = (int) ((widgetWidth - 4) * progress);

            boolean inIFrames = rollCooldown > (DodgeRollManager.COOLDOWN_TICKS - DodgeRollManager.IFRAME_DURATION_TICKS);

            if (inIFrames) {
                // Active i-frames invulnerability
                context.fill(x + 2, y + 2, x + widgetWidth - 2, y + widgetHeight - 2, 0x60FFA000);
                drawBorder(context, x, y, widgetWidth, widgetHeight, 0xFFFFD700);

                String text = "⚡ i-FRAMES";
                int textX = x + (widgetWidth - client.textRenderer.getWidth(text)) / 2;
                int textY = y + 4;
                context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFFF176);
            } else {
                // Recharging roll cooldown (emerald stamina gradient)
                if (fillW > 0) {
                    context.fill(x + 2, y + 2, x + 2 + fillW, y + widgetHeight - 2, 0x902E7D32);
                }
                drawBorder(context, x, y, widgetWidth, widgetHeight, 0xFF2A3442);

                float secondsLeft = rollCooldown / 20.0f;
                String text = String.format(java.util.Locale.ROOT, "ROLL %.1fs", secondsLeft);
                int textX = x + (widgetWidth - client.textRenderer.getWidth(text)) / 2;
                int textY = y + 4;
                context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFB0BEC5);
            }
        } else {
            // READY STATE
            context.fill(x + 2, y + 2, x + widgetWidth - 2, y + widgetHeight - 2, 0x401E293B);
            drawBorder(context, x, y, widgetWidth, widgetHeight, 0xFF4A5D6E);

            String text = "🛡 ROLL READY";
            int textX = x + (widgetWidth - client.textRenderer.getWidth(text)) / 2;
            int textY = y + 4;
            context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFFD700);
        }
    }

    private static void renderSlamHud(DrawContext context, MinecraftClient client, int screenWidth, int screenHeight) {
        int widgetWidth = 72;
        int widgetHeight = 16;

        // Position: symmetrically to the RIGHT of the hotbar (hotbar ends at screenWidth / 2 + 91)
        int x = screenWidth / 2 + 98;
        int y = screenHeight - 20;

        // Fallback positioning for narrow windows
        if (x + widgetWidth > screenWidth - 2) {
            x = screenWidth - widgetWidth - 4;
        }

        // Background: obsidian slate
        context.fill(x, y, x + widgetWidth, y + widgetHeight, 0xD00A0D14);

        if (clientCooldown > 0) {
            // ON COOLDOWN STATE
            float progress = 1.0f - ((float) clientCooldown / (float) GroundSlamManager.COOLDOWN_TICKS);
            int fillW = (int) ((widgetWidth - 4) * progress);

            // Recharging bar gradient (Abyssal blue)
            if (fillW > 0) {
                context.fill(x + 2, y + 2, x + 2 + fillW, y + widgetHeight - 2, 0x900088AA);
            }

            // Outer dark iron border
            drawBorder(context, x, y, widgetWidth, widgetHeight, 0xFF2A3442);

            // Text: SLAM 4.2s
            float secondsLeft = clientCooldown / 20.0f;
            String text = String.format(java.util.Locale.ROOT, "SLAM %.1fs", secondsLeft);
            int textX = x + (widgetWidth - client.textRenderer.getWidth(text)) / 2;
            int textY = y + 4;
            context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFB0BEC5);
        } else {
            // READY STATE
            boolean inAir = !client.player.isOnGround()
                    && !client.player.isClimbing()
                    && !client.player.isSubmergedInWater()
                    && !client.player.getAbilities().flying;

            if (inAir) {
                // Airborne: glowing active prompt!
                long time = System.currentTimeMillis();
                boolean pulse = (time / 300) % 2 == 0;
                int borderColor = pulse ? 0xFF00E5FF : 0xFF0099CC;

                context.fill(x + 2, y + 2, x + widgetWidth - 2, y + widgetHeight - 2, 0x60005577);
                drawBorder(context, x, y, widgetWidth, widgetHeight, borderColor);

                String text = "⚡ [CTRL] SLAM";
                int textX = x + (widgetWidth - client.textRenderer.getWidth(text)) / 2;
                int textY = y + 4;
                context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFF00FFFF);
            } else {
                // Grounded: primed ready badge
                context.fill(x + 2, y + 2, x + widgetWidth - 2, y + widgetHeight - 2, 0x401E293B);
                drawBorder(context, x, y, widgetWidth, widgetHeight, 0xFF4A5D6E);

                String text = "⚔ SLAM READY";
                int textX = x + (widgetWidth - client.textRenderer.getWidth(text)) / 2;
                int textY = y + 4;
                context.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFFD700);
            }
        }
    }

    private static void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);                 // Top
        context.fill(x, y + h - 1, x + w, y + h, color);         // Bottom
        context.fill(x, y, x + 1, y + h, color);                 // Left
        context.fill(x + w - 1, y, x + w, y + h, color);         // Right
    }
}
