package com.darkcraft.artorias.entity.margit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Margit, the Fell Omen - Epic Elden Ring Boss for Fabric 1.21.1 with GeckoLib.
 * Features frame-accurate delayed attacks, stance/poise break & riposte,
 * dynamic phase 2 transition with spectral golden hammer & sword,
 * and adaptive AI response to player positioning (tail swipe on behind-rolls).
 */
public class MargitEntity extends HostileEntity implements GeoEntity {

    public static final float MAX_POISE = 120.0f;
    private static final int POISE_RECOVERY_DELAY = 100; // 5 seconds (100 ticks)

    private static final TrackedData<Integer> ATTACK_STATE = DataTracker.registerData(MargitEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ATTACK_TICKS = DataTracker.registerData(MargitEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> PHASE_TWO = DataTracker.registerData(MargitEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> POISE = DataTracker.registerData(MargitEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SHACKLE_USES = DataTracker.registerData(MargitEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final ServerBossBar bossBar = new ServerBossBar(
            Text.translatable("entity.artorias.margit_the_fell_omen"),
            BossBar.Color.YELLOW,
            BossBar.Style.NOTCHED_10
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int timeSinceLastHit = 0;
    private int stunTicks = 0;
    private int pinnedTicks = 0;
    private int comboPauseTicks = 0;

    public MargitEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 8000;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 850.0) // Boss HP bar
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
                .add(EntityAttributes.GENERIC_ARMOR, 14.0)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 6.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_STATE, MargitAttackState.IDLE.getId());
        builder.add(ATTACK_TICKS, 0);
        builder.add(PHASE_TWO, false);
        builder.add(POISE, MAX_POISE);
        builder.add(SHACKLE_USES, 0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MargitAttackGoal(this));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.85));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    // ==========================================
    // GeckoLib Animation Controllers
    // ==========================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, this::movementPredicate));
        controllers.add(new AnimationController<>(this, "attack", 2, this::attackPredicate));
    }

    private PlayState movementPredicate(AnimationState<MargitEntity> state) {
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.margit.walk"));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.margit.idle"));
    }

    private PlayState attackPredicate(AnimationState<MargitEntity> state) {
        MargitAttackState attack = getAttackState();
        if (attack != MargitAttackState.IDLE) {
            return state.setAndContinue(RawAnimation.begin().thenPlay(attack.getAnimationName()));
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==========================================
    // State Machine & Timings
    // ==========================================

    public MargitAttackState getAttackState() {
        return MargitAttackState.fromId(this.dataTracker.get(ATTACK_STATE));
    }

    public void setAttackState(MargitAttackState state) {
        this.dataTracker.set(ATTACK_STATE, state.getId());
        this.setAttackTicks(0);
    }

    public int getAttackTicks() {
        return this.dataTracker.get(ATTACK_TICKS);
    }

    public void setAttackTicks(int ticks) {
        this.dataTracker.set(ATTACK_TICKS, ticks);
    }

    public void incrementAttackTicks() {
        this.setAttackTicks(this.getAttackTicks() + 1);
    }

    public boolean isPhaseTwo() {
        return this.dataTracker.get(PHASE_TWO);
    }

    public void setPhaseTwo(boolean phaseTwo) {
        this.dataTracker.set(PHASE_TWO, phaseTwo);
    }

    public float getPoise() {
        return this.dataTracker.get(POISE);
    }

    public void setPoise(float poise) {
        this.dataTracker.set(POISE, Math.max(0.0f, Math.min(MAX_POISE, poise)));
    }

    public int getShackleUses() {
        return this.dataTracker.get(SHACKLE_USES);
    }

    public void incrementShackleUses() {
        this.dataTracker.set(SHACKLE_USES, getShackleUses() + 1);
    }

    public boolean isStunned() {
        return getAttackState() == MargitAttackState.STUNNED;
    }

    public boolean isPinned() {
        return getAttackState() == MargitAttackState.PINNED;
    }

    public boolean isInPhaseTransition() {
        return getAttackState() == MargitAttackState.PHASE_TRANSITION;
    }

    public int getComboPauseTicks() {
        return comboPauseTicks;
    }

    public void setComboPauseTicks(int ticks) {
        this.comboPauseTicks = ticks;
    }

    // ==========================================
    // Server Tick & Mechanics Loop
    // ==========================================

    @Override
    public void tick() {
        super.tick();

        World world = this.getWorld();
        if (!world.isClient()) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

            // 1. Phase 2 Transition Check (HP <= 60%)
            if (!isPhaseTwo() && this.getHealth() <= this.getMaxHealth() * 0.60f) {
                triggerPhaseTwoTransition();
            }

            // 2. Handle Phase 2 Roar Animation
            if (isInPhaseTransition()) {
                handlePhaseTransitionTick();
                return;
            }

            // 3. Handle Stun / Poise Break Duration
            if (isStunned()) {
                handleStunTick();
                return;
            }

            // 4. Handle Pinned Duration (Margit's Shackle)
            if (isPinned()) {
                handlePinnedTick();
                return;
            }

            // 5. Poise Natural Regeneration (after 5 seconds without being hit)
            timeSinceLastHit++;
            if (timeSinceLastHit > POISE_RECOVERY_DELAY && getPoise() < MAX_POISE) {
                setPoise(getPoise() + 0.5f); // +10 poise per second
            }

            if (comboPauseTicks > 0) {
                comboPauseTicks--;
            }
        }
    }

    private void triggerPhaseTwoTransition() {
        setPhaseTwo(true);
        setAttackState(MargitAttackState.PHASE_TRANSITION);
        this.getNavigation().stop();

        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1.8f, 0.85f);
        }
    }

    private void handlePhaseTransitionTick() {
        this.getNavigation().stop();
        int ticks = getAttackTicks();
        incrementAttackTicks();

        // Spawn brilliant golden aura particles swirling around Margit
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            double angle = (ticks * 18.0) * Math.PI / 180.0;
            double radius = 1.6;
            double px = this.getX() + radius * Math.cos(angle);
            double pz = this.getZ() + radius * Math.sin(angle);
            serverWorld.spawnParticles(ParticleTypes.WAX_ON, px, this.getY() + 1.2, pz, 3, 0.1, 0.2, 0.1, 0.02);
            serverWorld.spawnParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 1.5, this.getZ(), 4, 0.8, 1.0, 0.8, 0.05);

            if (ticks == 40) {
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_TRIDENT_THUNDER.value(), SoundCategory.HOSTILE, 1.6f, 0.7f);
            }
        }

        if (ticks >= MargitAttackState.PHASE_TRANSITION.getTotalDuration()) {
            setAttackState(MargitAttackState.IDLE);
        }
    }

    private void handleStunTick() {
        this.getNavigation().stop();
        stunTicks++;

        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld && stunTicks % 5 == 0) {
            // Critical posture break indicator sparks above head
            serverWorld.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.6, this.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
        }

        if (stunTicks >= MargitAttackState.STUNNED.getTotalDuration()) {
            stunTicks = 0;
            setPoise(MAX_POISE);
            setAttackState(MargitAttackState.IDLE);
        }
    }

    private void handlePinnedTick() {
        this.getNavigation().stop();
        pinnedTicks++;

        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld && pinnedTicks % 4 == 0) {
            // Golden shackle chains holding boss to the ground
            for (int i = 0; i < 4; i++) {
                double rad = Math.toRadians(i * 90 + pinnedTicks * 3);
                double cx = this.getX() + Math.cos(rad) * 1.5;
                double cz = this.getZ() + Math.sin(rad) * 1.5;
                serverWorld.spawnParticles(ParticleTypes.END_ROD, cx, this.getY() + 0.2, cz, 2, 0.05, 0.4, 0.05, 0.02);
            }
        }

        if (pinnedTicks >= MargitAttackState.PINNED.getTotalDuration()) {
            pinnedTicks = 0;
            setAttackState(MargitAttackState.IDLE);
        }
    }

    // ==========================================
    // Damage, Poise Break & Riposte Logic
    // ==========================================

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Complete invulnerability during the Phase 2 transition scream
        if (isInPhaseTransition()) {
            return false;
        }

        boolean damaged = super.damage(source, amount);
        if (damaged) {
            timeSinceLastHit = 0;

            // Calculate poise damage based on weapon type
            float poiseDamage = 12.0f;
            Entity attacker = source.getAttacker();
            if (attacker instanceof LivingEntity livingAttacker) {
                ItemStack weapon = livingAttacker.getMainHandStack();
                if (weapon.getItem() instanceof AxeItem) {
                    poiseDamage = 35.0f; // Heavy axe hits heavily drain poise
                } else if (livingAttacker.isSneaking() || livingAttacker.fallDistance > 0.0f) {
                    poiseDamage = 28.0f; // Jump / heavy attacks
                }
            }

            // Deplete poise
            if (!isStunned() && !isPinned()) {
                float newPoise = getPoise() - poiseDamage;
                setPoise(newPoise);

                if (newPoise <= 0.0f) {
                    triggerStunBreak();
                }
            }
        }
        return damaged;
    }

    /**
     * Stance / Poise break: drops Margit to one knee, stops navigation,
     * disables AI for 4.5 seconds and opens window for critical riposte.
     */
    public void triggerStunBreak() {
        setAttackState(MargitAttackState.STUNNED);
        stunTicks = 0;
        this.getNavigation().stop();

        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 1.8f, 0.6f);
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 1.2f, 1.4f);
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5, this.getZ(), 1, 0, 0, 0, 0);
        }
    }

    /**
     * Critical Hit / Riposte: triggered when a player interacts with Margit from the front
     * while he is in the STUNNED state.
     */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (isStunned() && hand == Hand.MAIN_HAND) {
            // Check if player is facing and standing in front of Margit
            Vec3d bossFacing = this.getRotationVec(1.0f);
            Vec3d bossToPlayer = player.getPos().subtract(this.getPos()).normalize();
            double dot = bossFacing.x * bossToPlayer.x + bossFacing.z * bossToPlayer.z;

            // dot > 0.4 means player is within the frontal 120-degree cone
            if (dot > 0.4) {
                World world = this.getWorld();
                if (world instanceof ServerWorld serverWorld) {
                    float critDamage = (float) (this.getMaxHealth() * 0.18); // 18% max health critical strike
                    this.damage(world.getDamageSources().playerAttack(player), critDamage);

                    serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.0f, 0.7f);
                    serverWorld.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5, this.getZ(), 35, 0.5, 0.5, 0.5, 0.3);

                    // End stun and reset poise
                    stunTicks = 0;
                    setPoise(MAX_POISE);
                    setAttackState(MargitAttackState.IDLE);
                }
                return ActionResult.SUCCESS;
            }
        }
        return super.interactMob(player, hand);
    }

    // ==========================================
    // Margit's Shackle Application
    // ==========================================

    /**
     * Applies the effect of Margit's Shackle:
     * - Only works in Phase 1 (HP > 60%)
     * - Max 2 uses per fight
     * - Pins boss to the ground for 3 seconds (60 ticks)
     */
    public boolean applyShackle() {
        if (isPhaseTwo() || getShackleUses() >= 2 || isStunned() || isPinned() || isInPhaseTransition()) {
            return false;
        }

        incrementShackleUses();
        setAttackState(MargitAttackState.PINNED);
        pinnedTicks = 0;
        this.getNavigation().stop();

        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.HOSTILE, 2.0f, 0.6f);
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_ANVIL_USE, SoundCategory.HOSTILE, 1.5f, 0.8f);
            serverWorld.spawnParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + 0.5, this.getZ(), 50, 1.0, 0.5, 1.0, 0.2);
        }
        return true;
    }

    // ==========================================
    // Shockwave & Ground Slam Math
    // ==========================================

    /**
     * Radial ground shockwave used by HAMMER_SLAM.
     * Iterates outwards in concentric rings, deals damage, and knocks players upwards.
     */
    public void performRadialShockwave(double maxRadius, float damage) {
        World world = this.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 2.0f, 0.8f);

        // Visual ring of rupture and golden burst particles
        for (double r = 1.5; r <= maxRadius; r += 1.2) {
            int particleCount = (int) (r * 12);
            for (int i = 0; i < particleCount; i++) {
                double angle = (2 * Math.PI / particleCount) * i;
                double px = this.getX() + r * Math.cos(angle);
                double pz = this.getZ() + r * Math.sin(angle);
                serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, px, this.getY() + 0.1, pz, 1, 0, 0, 0, 0);
                serverWorld.spawnParticles(ParticleTypes.WAX_ON, px, this.getY() + 0.2, pz, 2, 0.1, 0.2, 0.1, 0.05);
            }
        }

        // Damage & knockup all living entities within radial distance
        Box shockwaveBox = Box.of(this.getPos(), maxRadius * 2, 4.0, maxRadius * 2);
        List<LivingEntity> targets = serverWorld.getEntitiesByClass(LivingEntity.class, shockwaveBox, e -> e != this && e.isAlive());

        for (LivingEntity target : targets) {
            double dist = Math.sqrt(this.squaredDistanceTo(target));
            if (dist <= maxRadius) {
                DamageSource damageSource = world.getDamageSources().mobAttack(this);
                // Damage falls off slightly at edge of shockwave
                float effectiveDamage = (float) (damage * (1.0 - (dist / (maxRadius * 2.0))));
                target.damage(damageSource, effectiveDamage);

                // Vertical launch + horizontal outward blast
                Vec3d outward = target.getPos().subtract(this.getPos()).normalize().multiply(0.65);
                target.setVelocity(outward.x, 0.52, outward.z);
                target.velocityModified = true;
            }
        }
    }

    // ==========================================
    // Boss Bar Player Tracking
    // ==========================================

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }
}
