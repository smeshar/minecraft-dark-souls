package com.darkcraft.artorias.entity.goblin;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GoblinEntity extends HostileEntity {
    public static final int STATE_IDLE = 0;
    public static final int STATE_STABBING = 1;
    public static final int STATE_FLURRY = 2;
    public static final int STATE_DODGE = 3;

    protected static final TrackedData<Integer> ATTACK_STATE = DataTracker.registerData(GoblinEntity.class, TrackedDataHandlerRegistry.INTEGER);
    protected int attackTicks = 0;
    protected int dodgeCooldown = 0;

    public GoblinEntity(EntityType<? extends GoblinEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createGoblinAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_STATE, STATE_IDLE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new GoblinAttackGoal(this, 1.25, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public int getAttackState() {
        return this.dataTracker.get(ATTACK_STATE);
    }

    public void setAttackState(int state) {
        this.dataTracker.set(ATTACK_STATE, state);
    }

    public int getAttackTicks() {
        return attackTicks;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Agile dodge roll chance on hit
        if (dodgeCooldown <= 0 && this.getRandom().nextFloat() < 0.45f && source.getAttacker() instanceof LivingEntity attacker) {
            this.setAttackState(STATE_DODGE);
            this.attackTicks = 14;
            this.dodgeCooldown = 60; // 3 seconds cooldown

            Vec3d away = this.getPos().subtract(attacker.getPos()).normalize();
            // Evade diagonally backward
            double lateral = this.getRandom().nextBoolean() ? 0.6 : -0.6;
            Vec3d leap = new Vec3d(away.x + away.z * lateral, 0.28, away.z - away.x * lateral).normalize().multiply(0.85);

            this.setVelocity(leap);
            this.velocityModified = true;

            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.0f, 1.5f);
            if (this.getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
            }

            return false; // Evaded damage through agile backstep!
        }

        return super.damage(source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (dodgeCooldown > 0) dodgeCooldown--;

        if (this.attackTicks > 0) {
            this.attackTicks--;
            if (this.attackTicks == 0 && !this.getWorld().isClient()) {
                setAttackState(STATE_IDLE);
            }
        }
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        int count = 1 + world.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) {
            this.dropItem(ArtoriasMod.SOUL_RATION);
        }
    }

    public void triggerFlurry(LivingEntity target) {
        if (!this.getWorld().isClient()) {
            this.setAttackState(STATE_FLURRY);
            this.attackTicks = 16;

            if (this.getWorld() instanceof ServerWorld sw) {
                this.tryAttack(target);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.HOSTILE, 1.2f, 1.4f);
                sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    static class GoblinAttackGoal extends MeleeAttackGoal {
        private final GoblinEntity goblin;

        public GoblinAttackGoal(GoblinEntity goblin, double speed, boolean pauseWhenMobIdle) {
            super(goblin, speed, pauseWhenMobIdle);
            this.goblin = goblin;
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = goblin.getTarget();
            if (target != null && target.isAlive() && goblin.getAttackState() == STATE_IDLE) {
                if (goblin.squaredDistanceTo(target) <= 4.0 && goblin.getRandom().nextFloat() < 0.2f) {
                    goblin.triggerFlurry(target);
                }
            }
        }
    }
}
