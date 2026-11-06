package com.darkcraft.artorias.entity.goblin;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
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

import java.util.EnumSet;

public class GoblinEntity extends HostileEntity {
    public static final int STATE_IDLE = 0;
    public static final int STATE_STAB = 1;
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
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.36)
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.ARMOR, 2.0)
                .add(EntityAttributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_STATE, STATE_IDLE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new GoblinCombatGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.9));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
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
        return this.attackTicks;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Agile dodge roll chance on hit
        if (dodgeCooldown <= 0 && this.getRandom().nextFloat() < 0.45f && source.getAttacker() instanceof LivingEntity attacker) {
            this.setAttackState(STATE_DODGE);
            this.attackTicks = 14;
            this.dodgeCooldown = 60; // 3 seconds cooldown

            Vec3d away = this.getEntityPos().subtract(attacker.getEntityPos()).normalize();
            // Evade diagonally backward
            double lateral = this.getRandom().nextBoolean() ? 0.6 : -0.6;
            Vec3d leap = new Vec3d(away.x + away.z * lateral, 0.28, away.z - away.x * lateral).normalize().multiply(0.85);

            this.setVelocity(leap);
            this.velocityDirty = true;

            world.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.0f, 1.5f);
            world.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.2, 0.1, 0.2, 0.05);

            return false; // Evaded damage through agile backstep!
        }

        return super.damage(world, source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (dodgeCooldown > 0) dodgeCooldown--;

        if (this.attackTicks > 0) {
            this.attackTicks--;
            if (this.attackTicks == 0 && !this.getEntityWorld().isClient()) {
                setAttackState(STATE_IDLE);
            }
        }
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        int count = 1 + world.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) {
            this.dropItem(world, ArtoriasMod.SOUL_RATION);
        }
    }

    public void triggerFlurry(LivingEntity target) {
        if (!this.getEntityWorld().isClient()) {
            this.setAttackState(STATE_FLURRY);
            this.attackTicks = 16;

            if (this.getEntityWorld() instanceof ServerWorld sw) {
                this.tryAttack(sw, target);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.HOSTILE, 1.2f, 1.4f);
                sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    static class GoblinCombatGoal extends Goal {
        protected final GoblinEntity goblin;
        protected int attackCooldown = 0;

        public GoblinCombatGoal(GoblinEntity goblin) {
            this.goblin = goblin;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = goblin.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = goblin.getTarget();
            if (target == null) return;

            goblin.getLookControl().lookAt(target, 30.0f, 30.0f);
            double distSq = goblin.squaredDistanceTo(target);

            if (attackCooldown > 0) attackCooldown--;

            if (distSq <= 5.0 && attackCooldown <= 0 && goblin.getAttackState() == STATE_IDLE) {
                goblin.triggerFlurry(target);
                attackCooldown = 30;
                return;
            }

            if (goblin.getAttackState() == STATE_IDLE) {
                goblin.getNavigation().startMovingTo(target, 1.25);
            }
        }
    }
}
