package com.darkcraft.artorias.combat;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Elden Ring Dodge Roll mechanic with invulnerability frames (i-frames).
 */
public class DodgeRollManager {
    private static final Map<UUID, Integer> IFRAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ROLL_COOLDOWN = new ConcurrentHashMap<>();

    public static final int IFRAME_DURATION_TICKS = 8; // ~0.4s of invulnerability
    public static final int COOLDOWN_TICKS = 14;        // ~0.7s cooldown between rolls

    public static void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (IFRAMES.containsKey(uuid)) {
            int ticks = IFRAMES.get(uuid) - 1;
            if (ticks <= 0) {
                IFRAMES.remove(uuid);
            } else {
                IFRAMES.put(uuid, ticks);
            }
        }

        if (ROLL_COOLDOWN.containsKey(uuid)) {
            int cd = ROLL_COOLDOWN.get(uuid) - 1;
            if (cd <= 0) {
                ROLL_COOLDOWN.remove(uuid);
            } else {
                ROLL_COOLDOWN.put(uuid, cd);
            }
        }
    }

    public static boolean hasIFrames(ServerPlayerEntity player) {
        return IFRAMES.getOrDefault(player.getUuid(), 0) > 0;
    }

    public static boolean canRoll(ServerPlayerEntity player) {
        return !player.isSpectator()
                && player.isAlive()
                && !player.hasVehicle()
                && !ROLL_COOLDOWN.containsKey(player.getUuid())
                && player.getHungerManager().getFoodLevel() > 2;
    }

    public static void performRoll(ServerPlayerEntity player, double inputX, double inputZ) {
        if (!canRoll(player)) {
            return;
        }

        UUID uuid = player.getUuid();
        IFRAMES.put(uuid, IFRAME_DURATION_TICKS);
        ROLL_COOLDOWN.put(uuid, COOLDOWN_TICKS);

        // Exhaustion (stamina cost)
        player.getHungerManager().addExhaustion(1.2f);

        // Calculate direction relative to player look angle
        // In Minecraft: inputZ > 0 is forward, inputZ < 0 is backward
        // inputX > 0 is left (A), inputX < 0 is right (D)
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d forward = new Vec3d(look.x, 0, look.z).normalize();
        Vec3d left = new Vec3d(forward.z, 0, -forward.x);

        Vec3d rollDirection;
        if (Math.abs(inputX) < 0.01 && Math.abs(inputZ) < 0.01) {
            // Standing still: Dark Souls backstep
            rollDirection = forward.multiply(-0.95);
        } else {
            rollDirection = forward.multiply(inputZ).add(left.multiply(inputX)).normalize();
        }

        double horizontalPower = 1.30;
        double verticalHop = 0.22;

        player.setVelocity(
                rollDirection.x * horizontalPower,
                verticalHop,
                rollDirection.z * horizontalPower
        );
        player.velocityDirty = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

        if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
            Vec3d pos = player.getEntityPos();
            // Sharp sweep slash & abyssal soul trail
            serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, pos.x + rollDirection.x * 0.6, pos.y + 0.3, pos.z + rollDirection.z * 0.6, 1, 0, 0, 0, 0.0);
            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.1, pos.z, 10, 0.25, 0.08, 0.25, 0.02);
            serverWorld.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.05, pos.z, 6, 0.3, 0.05, 0.3, 0.03);

            // Dynamic Dark Souls roll audio: sharp whoosh & armor clink
            serverWorld.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.35f);
            serverWorld.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.PLAYERS, 0.85f, 1.25f);
        }
    }
}
