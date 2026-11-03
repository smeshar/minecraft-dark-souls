package com.darkcraft.artorias.combat;

import com.darkcraft.artorias.ArtoriasMod;
import com.darkcraft.artorias.network.ModMessages;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ground Slam / Plunge Attack mechanic.
 * Triggered by pressing Ctrl while airborne.
 * Downward plunge dealing AoE impact damage, shockwave particles, screen shake, and 5s cooldown.
 */
public class GroundSlamManager {
    public static final int COOLDOWN_TICKS = 100; // 5.0 seconds (100 ticks @ 20 TPS)
    public static final int MAX_AIR_TICKS = 60;   // Timeout safety
    public static final double SLAM_RADIUS = 4.5;
    public static final float BASE_DAMAGE = 14.0f;
    public static final double DOWNWARD_SPEED = -1.95;

    private static final Map<UUID, Integer> SLAM_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SLAMMING_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FALL_IMMUNITY = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> RECOVERY_TICKS = new ConcurrentHashMap<>();

    public static boolean canSlam(ServerPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator() || player.hasVehicle() || player.getAbilities().flying) {
            return false;
        }
        if (player.isOnGround() || player.isClimbing() || player.isSubmergedInWater()) {
            return false;
        }
        if (SLAM_COOLDOWNS.containsKey(player.getUuid())) {
            return false;
        }
        return player.getHungerManager().getFoodLevel() > 2;
    }

    public static boolean hasFallImmunity(ServerPlayerEntity player) {
        return FALL_IMMUNITY.containsKey(player.getUuid());
    }

    public static int getCooldown(ServerPlayerEntity player) {
        return SLAM_COOLDOWNS.getOrDefault(player.getUuid(), 0);
    }

    public static void initiateSlam(ServerPlayerEntity player) {
        if (!canSlam(player)) {
            return;
        }

        UUID uuid = player.getUuid();
        SLAM_COOLDOWNS.put(uuid, COOLDOWN_TICKS);
        SLAMMING_PLAYERS.put(uuid, 1);
        FALL_IMMUNITY.put(uuid, 40);

        // Stamina cost
        player.getHungerManager().addExhaustion(2.0f);
        player.fallDistance = 0.0f;

        // Apply high downward velocity with dampened horizontal movement
        Vec3d v = player.getVelocity();
        player.setVelocity(v.x * 0.25, DOWNWARD_SPEED, v.z * 0.25);
        player.velocityDirty = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

        if (player.getEntityWorld() instanceof ServerWorld sw) {
            sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_MACE_SMASH_AIR, SoundCategory.PLAYERS, 1.4f, 0.95f);
            sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 0.65f);

            // Broadcast slam start to nearby tracking players
            ModMessages.GroundSlamStartPayload startPayload = new ModMessages.GroundSlamStartPayload(player.getId());
            for (ServerPlayerEntity tracking : PlayerLookup.tracking(player)) {
                ServerPlayNetworking.send(tracking, startPayload);
            }
            ServerPlayNetworking.send(player, startPayload);
        }
    }

    public static void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        // Tick Cooldown
        if (SLAM_COOLDOWNS.containsKey(uuid)) {
            int cd = SLAM_COOLDOWNS.get(uuid) - 1;
            if (cd <= 0) {
                SLAM_COOLDOWNS.remove(uuid);
            } else {
                SLAM_COOLDOWNS.put(uuid, cd);
            }
        }

        // Tick Fall Immunity
        if (FALL_IMMUNITY.containsKey(uuid)) {
            int fi = FALL_IMMUNITY.get(uuid) - 1;
            if (fi <= 0) {
                FALL_IMMUNITY.remove(uuid);
            } else {
                FALL_IMMUNITY.put(uuid, fi);
            }
        }

        // Tick Recovery
        if (RECOVERY_TICKS.containsKey(uuid)) {
            int rec = RECOVERY_TICKS.get(uuid) - 1;
            if (rec <= 0) {
                RECOVERY_TICKS.remove(uuid);
            } else {
                RECOVERY_TICKS.put(uuid, rec);
            }
        }

        // Tick Slamming State
        if (SLAMMING_PLAYERS.containsKey(uuid)) {
            int airTicks = SLAMMING_PLAYERS.get(uuid) + 1;
            SLAMMING_PLAYERS.put(uuid, airTicks);

            // Keep terminal plunge speed
            if (player.getVelocity().y > -1.2) {
                player.setVelocity(player.getVelocity().x * 0.75, DOWNWARD_SPEED, player.getVelocity().z * 0.75);
                player.velocityDirty = true;
            }

            // Descent particles
            if (player.getEntityWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.3, player.getZ(), 3, 0.15, 0.25, 0.15, 0.02);
                sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 0.35, player.getZ(), 1, 0, 0, 0, 0.0);
                sw.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.12, 0.12, 0.12, 0.01);
            }

            // Check if player has landed or hit a block
            if (player.isOnGround() || isTouchingBlockBeneath(player) || airTicks >= MAX_AIR_TICKS) {
                performImpact(player);
            }
        }
    }

    private static boolean isTouchingBlockBeneath(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().offset(0, -0.22, 0);
        return !player.getEntityWorld().isSpaceEmpty(player, box);
    }

    public static void performImpact(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        SLAMMING_PLAYERS.remove(uuid);
        FALL_IMMUNITY.put(uuid, 25);
        RECOVERY_TICKS.put(uuid, 8);
        player.fallDistance = 0.0f;

        if (!(player.getEntityWorld() instanceof ServerWorld sw)) {
            return;
        }

        Vec3d pos = player.getEntityPos();

        // 1. Calculate Damage
        float damage = BASE_DAMAGE;
        if (player.getMainHandStack().isOf(ArtoriasMod.ARTORIAS_GREATSWORD)) {
            damage += 10.0f; // Boss weapon bonus! (Total 24 dmg)
        }

        // 2. AoE Damage & Heavy Shockwave Knockback
        Box hitBox = new Box(
                pos.x - SLAM_RADIUS, pos.y - 1.5, pos.z - SLAM_RADIUS,
                pos.x + SLAM_RADIUS, pos.y + 2.5, pos.z + SLAM_RADIUS
        );

        for (Entity entity : sw.getOtherEntities(player, hitBox)) {
            if (entity instanceof LivingEntity target) {
                if (target.isTeammate(player)) continue;
                if (target instanceof TameableEntity tameable && tameable.isOwner(player)) continue;

                double distSq = target.squaredDistanceTo(pos);
                if (distSq <= SLAM_RADIUS * SLAM_RADIUS) {
                    double dist = Math.sqrt(distSq);
                    float proximity = 1.0f - (float) (dist / SLAM_RADIUS) * 0.4f;
                    float finalDamage = damage * Math.max(0.6f, proximity);

                    target.damage(sw, sw.getDamageSources().playerAttack(player), finalDamage);

                    Vec3d diff = target.getEntityPos().subtract(pos);
                    Vec3d push = new Vec3d(diff.x, 0, diff.z).normalize().multiply(1.35);
                    target.takeKnockback(1.3, -push.x, -push.z);
                    target.addVelocity(0, 0.42, 0);
                    target.velocityDirty = true;
                }
            }
        }

        // 3. Spectacular Visual Shockwave
        // Large gust emitter from 1.21 mace mechanics
        sw.spawnParticles(ParticleTypes.GUST_EMITTER_LARGE, pos.x, pos.y + 0.1, pos.z, 1, 0, 0, 0, 0);
        sw.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.15, pos.z, 2, 0.3, 0.1, 0.3, 0.0);

        // Circular expanding soul flame ring
        for (int i = 0; i < 36; i++) {
            double angle = i * (Math.PI * 2 / 36);
            double rx = Math.cos(angle);
            double rz = Math.sin(angle);
            sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x + rx * 1.6, pos.y + 0.15, pos.z + rz * 1.6, 1, rx * 0.18, 0.04, rz * 0.18, 0.02);
            sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x + rx * 3.0, pos.y + 0.2, pos.z + rz * 3.0, 1, rx * 0.12, 0.03, rz * 0.12, 0.01);
        }

        // Block debris particles from impacted block
        BlockPos groundPos = player.getBlockPos().down();
        BlockState groundState = sw.getBlockState(groundPos);
        if (groundState.isAir()) {
            groundState = sw.getBlockState(player.getBlockPos());
        }
        if (!groundState.isAir()) {
            sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, groundState),
                    pos.x, pos.y + 0.1, pos.z, 50, 1.4, 0.3, 1.4, 0.18);
        }

        // Ground dust ring
        sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 0.1, pos.z, 16, 1.2, 0.1, 1.2, 0.03);

        // 4. Punchy Layered Audio
        sw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1.6f, 0.85f);
        sw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.85f, 1.25f);
        sw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_HEAVY_CORE_STEP, SoundCategory.PLAYERS, 1.4f, 0.65f);

        // 5. Hand swing animation
        player.swingHand(Hand.MAIN_HAND, true);

        // 6. Broadcast Impact to tracking clients
        ModMessages.GroundSlamImpactPayload impactPayload = new ModMessages.GroundSlamImpactPayload(player.getId(), pos.x, pos.y, pos.z);
        for (ServerPlayerEntity tracking : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracking, impactPayload);
        }
        ServerPlayNetworking.send(player, impactPayload);
    }
}

