package com.darkcraft.artorias.entity.margit;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

/**
 * Custom AI Goal for Margit, the Fell Omen.
 * Synchronizes server ticks with GeckoLib animations to deliver frame-perfect delayed attacks,
 * active damage frames, punish recovery windows, and adaptive reactive counter-attacks (Tail Swipe).
 */
public class MargitAttackGoal extends Goal {
    private final MargitEntity boss;
    private boolean hitAppliedThisState = false;

    public MargitAttackGoal(MargitEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = boss.getTarget();
        return target != null && target.isAlive()
                && !boss.isStunned()
                && !boss.isPinned()
                && !boss.isInPhaseTransition();
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    @Override
    public void start() {
        hitAppliedThisState = false;
    }

    @Override
    public void stop() {
        boss.setAttackState(MargitAttackState.IDLE);
        boss.setAttackTicks(0);
        hitAppliedThisState = false;
    }

    @Override
    public void tick() {
        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive()) return;

        MargitAttackState currentState = boss.getAttackState();
        double distSq = boss.squaredDistanceTo(target);
        double dist = Math.sqrt(distSq);

        // Vector math: calculate boss horizontal facing and vector towards player
        Vec3d bossFacing = boss.getRotationVec(1.0f);
        Vec3d toTarget = target.getEntityPos().subtract(boss.getEntityPos()).normalize();
        // Dot product in horizontal plane (X-Z)
        double dotProduct = (bossFacing.x * toTarget.x) + (bossFacing.z * toTarget.z);

        // =========================================================================
        // 1. Reactive Anti-Behind Counter: Tail Swipe check
        // If player is strictly behind boss (dot < -0.45) within 4.5 blocks,
        // Margit immediately cancels current action or wind-up and performs TAIL_SWIPE!
        // =========================================================================
        if (dotProduct < -0.45 && dist <= 4.5 && currentState != MargitAttackState.TAIL_SWIPE) {
            // Cancel any idle or combo wind-up into tail swipe
            if (currentState == MargitAttackState.IDLE || currentState == MargitAttackState.STAFF_SWING_COMBO) {
                initiateAttack(MargitAttackState.TAIL_SWIPE);
                return;
            }
        }

        // =========================================================================
        // 2. Active Attack Execution & Timing Lifecycles
        // =========================================================================
        if (currentState != MargitAttackState.IDLE) {
            int currentTick = boss.getAttackTicks();
            boss.incrementAttackTicks();

            // Always track target during wind-up phase
            if (currentState.isWindupPhase(currentTick)) {
                boss.getLookControl().lookAt(target, 30.0f, 30.0f);
            }

            // Attack-specific frame mechanics
            switch (currentState) {
                case STAFF_SWING_COMBO -> handleStaffSwingCombo(currentTick, target);
                case STAFF_LEAP -> handleStaffLeap(currentTick, target);
                case DAGGER_TOSS -> handleDaggerToss(currentTick, target);
                case TAIL_SWIPE -> handleTailSwipe(currentTick, target);
                case HAMMER_SLAM -> handleHammerSlam(currentTick, target);
                default -> {}
            }

            // Animation complete: return to neutral IDLE and set spacing combo cooldown
            if (currentTick >= currentState.getTotalDuration()) {
                boss.setAttackState(MargitAttackState.IDLE);
                boss.setAttackTicks(0);
                hitAppliedThisState = false;
                boss.setComboPauseTicks(boss.getRandom().nextInt(15) + 10); // 10-25 ticks pause
            }
            return;
        }

        // =========================================================================
        // 3. Adaptive Neutral Spacing & Move Selection (IDLE State)
        // =========================================================================
        boss.getLookControl().lookAt(target, 30.0f, 30.0f);

        // If in combo recovery pause, smoothly circle/stalk the player
        if (boss.getComboPauseTicks() > 0) {
            boss.getNavigation().startMovingTo(target, 0.85);
            return;
        }

        // Move Selection based on distance
        if (dist > 12.0) {
            // Long range: 50% chance leap slam, 50% chance spectral dagger toss
            if (boss.getRandom().nextBoolean()) {
                initiateAttack(MargitAttackState.STAFF_LEAP);
            } else {
                initiateAttack(MargitAttackState.DAGGER_TOSS);
            }
        } else if (dist < 4.0) {
            // Close range: Fast horizontal combo, or Phase 2 Hammer Slam
            if (boss.isPhaseTwo() && boss.getRandom().nextFloat() < 0.40f) {
                initiateAttack(MargitAttackState.HAMMER_SLAM);
            } else {
                initiateAttack(MargitAttackState.STAFF_SWING_COMBO);
            }
        } else {
            // Mid range (4 to 12 blocks): Approach with intimidating boss pace
            boss.getNavigation().startMovingTo(target, 1.15);

            // Chance to jump-slam or toss daggers while closing distance
            if (boss.isPhaseTwo() && boss.getRandom().nextFloat() < 0.05f) {
                initiateAttack(MargitAttackState.HAMMER_SLAM);
            } else if (boss.getRandom().nextFloat() < 0.04f) {
                initiateAttack(MargitAttackState.DAGGER_TOSS);
            }
        }
    }

    private void initiateAttack(MargitAttackState state) {
        boss.setAttackState(state);
        boss.setAttackTicks(0);
        hitAppliedThisState = false;
        boss.getNavigation().stop();
    }

    // =========================================================================
    // Attack Phase Timing Implementations
    // =========================================================================

    /**
     * Staff Swing Combo: Deliberate windup, then wide horizontal sweep across the front.
     */
    private void handleStaffSwingCombo(int tick, LivingEntity target) {
        // Active hitbox window: ticks 19-26
        if (MargitAttackState.STAFF_SWING_COMBO.isActiveFrame(tick) && !hitAppliedThisState) {
            Vec3d forward = boss.getRotationVec(1.0f).multiply(2.2);
            Box hitBox = Box.of(boss.getEntityPos().add(forward.x, 1.2, forward.z), 3.4, 2.4, 3.4);

            applyMeleeHitbox(hitBox, 14.0f, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.55);
            hitAppliedThisState = true;
        }
    }

    /**
     * Staff Leap: Boss leaps high into the air during windup, descending in a crushing slam.
     */
    private void handleStaffLeap(int tick, LivingEntity target) {
        // Tick 14: Leap launch physics towards player position
        if (tick == 14) {
            Vec3d leapDir = target.getEntityPos().subtract(boss.getEntityPos()).normalize();
            boss.setVelocity(leapDir.x * 1.3, 0.72, leapDir.z * 1.3);
            boss.velocityDirty = true;

            World world = boss.getEntityWorld();
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 1.5f, 0.7f);
            }
        }

        // Active landing impact window: ticks 29-36
        if (MargitAttackState.STAFF_LEAP.isActiveFrame(tick) && !hitAppliedThisState && boss.isOnGround()) {
            Box impactBox = Box.of(boss.getEntityPos().add(0, 0.6, 0), 4.6, 2.5, 4.6);
            applyMeleeHitbox(impactBox, 18.0f, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.85);

            World world = boss.getEntityWorld();
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 0.2, boss.getZ(), 2, 0.4, 0.1, 0.4, 0.0);
            }
            hitAppliedThisState = true;
        }
    }

    /**
     * Dagger Toss: Throws 2 spectral golden daggers consecutively at frame-perfect timings.
     */
    private void handleDaggerToss(int tick, LivingEntity target) {
        World world = boss.getEntityWorld();
        if (world.isClient()) return;

        // Dagger 1 spawn at tick 13
        if (tick == 13) {
            spawnSpectralDagger(target, 0.0);
        }
        // Dagger 2 spawn at tick 24
        if (tick == 24) {
            spawnSpectralDagger(target, 0.15); // Slight lead angle
        }
    }

    private void spawnSpectralDagger(LivingEntity target, double leadAngle) {
        World world = boss.getEntityWorld();
        MargitDaggerEntity dagger = new MargitDaggerEntity(world, boss);

        Vec3d targetPos = target.getEyePos().subtract(0, 0.2, 0);
        Vec3d launchVec = targetPos.subtract(dagger.getEntityPos()).normalize();

        dagger.setVelocity(launchVec.x + leadAngle, launchVec.y + 0.05, launchVec.z + leadAngle, 1.85f, 1.0f);
        world.spawnEntity(dagger);

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.HOSTILE, 1.2f, 1.6f);
        }
    }

    /**
     * Tail Swipe: Instant 360-degree sweep around and behind the boss.
     */
    private void handleTailSwipe(int tick, LivingEntity target) {
        // Active frames: ticks 9-16
        if (MargitAttackState.TAIL_SWIPE.isActiveFrame(tick) && !hitAppliedThisState) {
            // Hitbox covers rear and full 360 circle around Margit
            Vec3d rear = boss.getRotationVec(1.0f).multiply(-1.0);
            Box tailBox = Box.of(boss.getEntityPos().add(rear.x, 0.8, rear.z), 4.8, 2.0, 4.8);

            applyMeleeHitbox(tailBox, 13.0f, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.95);

            World world = boss.getEntityWorld();
            if (world instanceof ServerWorld serverWorld) {
                // Circular sweep particles
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI / 16) * i;
                    double px = boss.getX() + 2.2 * Math.cos(angle);
                    double pz = boss.getZ() + 2.2 * Math.sin(angle);
                    serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, px, boss.getY() + 0.6, pz, 1, 0, 0, 0, 0);
                }
            }
            hitAppliedThisState = true;
        }
    }

    /**
     * Hammer Slam: Phase 2 jump slam with massive radial ground shockwave.
     */
    private void handleHammerSlam(int tick, LivingEntity target) {
        // Tick 18: Launch high into air
        if (tick == 18) {
            Vec3d slamDir = target.getEntityPos().subtract(boss.getEntityPos()).normalize();
            boss.setVelocity(slamDir.x * 1.4, 0.92, slamDir.z * 1.4);
            boss.velocityDirty = true;
        }

        // Active slam window on landing: ticks 36-44
        if (MargitAttackState.HAMMER_SLAM.isActiveFrame(tick) && !hitAppliedThisState && boss.isOnGround()) {
            boss.performRadialShockwave(6.5, 22.0f);
            hitAppliedThisState = true;
        }
    }

    /**
     * Utility method to apply melee damage and knockback within a given Box.
     */
    private void applyMeleeHitbox(Box box, float damage, net.minecraft.sound.SoundEvent sound, double knockbackStrength) {
        World world = boss.getEntityWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        List<LivingEntity> targets = serverWorld.getEntitiesByClass(LivingEntity.class, box, e -> e != boss && e.isAlive());
        for (LivingEntity entity : targets) {
            DamageSource damageSource = world.getDamageSources().mobAttack(boss);
            entity.damage(serverWorld, damageSource, damage);

            Vec3d knockbackDir = entity.getEntityPos().subtract(boss.getEntityPos()).normalize().multiply(knockbackStrength);
            entity.addVelocity(knockbackDir.x, 0.35, knockbackDir.z);
            entity.velocityDirty = true;
        }

        serverWorld.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                sound, SoundCategory.HOSTILE, 1.6f, 0.85f);
    }
}
