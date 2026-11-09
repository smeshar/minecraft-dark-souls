package com.darkcraft.artorias.entity.warg;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

public class WargEntity extends HostileEntity {
    public static final int STATE_IDLE = 0;
    public static final int STATE_POUNCE = 1;
    public static final int STATE_BITE = 2;
    public static final int STATE_HOWL = 3;

    private static final TrackedData<Integer> ATTACK_STATE = DataTracker.registerData(WargEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_GIANT = DataTracker.registerData(WargEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int attackTicks = 0;
    private int howlCooldown = 200;

    public WargEntity(EntityType<? extends WargEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 15;
    }

    public WargEntity(EntityType<? extends WargEntity> entityType, World world, boolean isGiant) {
        super(entityType, world);
        this.setGiant(isGiant);
        this.experiencePoints = isGiant ? 35 : 15;
    }

    public static DefaultAttributeContainer.Builder createGiantAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    public static DefaultAttributeContainer.Builder createLesserAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.38)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.2)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_STATE, STATE_IDLE);
        builder.add(IS_GIANT, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new WargCombatGoal(this));
        this.goalSelector.add(3, new WargCircleGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.9));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public boolean isGiant() {
        return this.dataTracker.get(IS_GIANT);
    }

    public void setGiant(boolean giant) {
        this.dataTracker.set(IS_GIANT, giant);
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
    public void tick() {
        super.tick();

        if (howlCooldown > 0) {
            howlCooldown--;
        }

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
        int count = isGiant() ? (2 + world.getRandom().nextInt(3)) : (1 + world.getRandom().nextInt(2));
        for (int i = 0; i < count; i++) {
            this.dropItem(ArtoriasMod.SOUL_RATION);
        }
    }

    public void triggerPounce(LivingEntity target) {
        if (!this.getWorld().isClient()) {
            this.setAttackState(STATE_POUNCE);
            this.attackTicks = 25;

            Vec3d dir = target.getPos().subtract(this.getPos());
            Vec3d leap = new Vec3d(dir.x, 0, dir.z).normalize().multiply(1.2).add(0, 0.42, 0);
            this.setVelocity(leap);
            this.velocityModified = true;

            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 1.4f, isGiant() ? 0.6f : 1.1f);
        }
    }

    public void triggerBite(LivingEntity target) {
        if (!this.getWorld().isClient()) {
            this.setAttackState(STATE_BITE);
            this.attackTicks = 12;

            this.tryAttack(target);
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_WOLF_SHAKE, SoundCategory.HOSTILE, 1.2f, isGiant() ? 0.7f : 1.2f);
        }
    }

    public void triggerHowl() {
        if (!this.getWorld().isClient() && isGiant() && howlCooldown <= 0) {
            this.setAttackState(STATE_HOWL);
            this.attackTicks = 35;
            this.howlCooldown = 300; // 15 seconds

            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.8f, 0.65f);

            if (this.getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.2, this.getZ(), 25, 0.8, 0.5, 0.8, 0.05);

                // Buff nearby wargs
                Box box = this.getBoundingBox().expand(14.0);
                for (WargEntity packMember : sw.getEntitiesByClass(WargEntity.class, box, e -> e.isAlive())) {
                    packMember.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
                    packMember.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
                }
            }
        }
    }

    // Elden Ring style wolf combat AI
    static class WargCombatGoal extends Goal {
        private final WargEntity warg;
        private int pounceCooldown = 0;

        public WargCombatGoal(WargEntity warg) {
            this.warg = warg;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = warg.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = warg.getTarget();
            if (target == null) return;

            warg.getLookControl().lookAt(target, 30.0f, 30.0f);
            double distSq = warg.squaredDistanceTo(target);

            if (pounceCooldown > 0) pounceCooldown--;

            if (warg.isGiant() && warg.howlCooldown <= 0 && distSq < 100.0) {
                warg.triggerHowl();
                return;
            }

            // Pounce attack from 4 to 10 blocks
            if (distSq >= 16.0 && distSq <= 100.0 && pounceCooldown <= 0 && warg.getAttackState() == STATE_IDLE) {
                warg.triggerPounce(target);
                pounceCooldown = 80;
                return;
            }

            // Close melee bite
            if (distSq < 6.0 && warg.getAttackState() == STATE_IDLE) {
                warg.triggerBite(target);
                return;
            }

            if (warg.getAttackState() == STATE_IDLE) {
                warg.getNavigation().startMovingTo(target, 1.25);
            }
        }
    }

    // Flanking & circling around player
    static class WargCircleGoal extends Goal {
        private final WargEntity warg;
        private int circleDir = 1;

        public WargCircleGoal(WargEntity warg) {
            this.warg = warg;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = warg.getTarget();
            if (target == null || !target.isAlive()) return false;
            double distSq = warg.squaredDistanceTo(target);
            return distSq > 16.0 && distSq < 120.0 && warg.getAttackState() == STATE_IDLE && warg.getRandom().nextFloat() < 0.25f;
        }

        @Override
        public void start() {
            circleDir = warg.getRandom().nextBoolean() ? 1 : -1;
        }

        @Override
        public void tick() {
            LivingEntity target = warg.getTarget();
            if (target == null) return;

            Vec3d toTarget = target.getPos().subtract(warg.getPos()).normalize();
            Vec3d lateral = new Vec3d(-toTarget.z * circleDir, 0, toTarget.x * circleDir).multiply(4.0);
            Vec3d circlePos = warg.getPos().add(lateral).add(toTarget.multiply(1.5));

            warg.getNavigation().startMovingTo(circlePos.x, circlePos.y, circlePos.z, 1.15);
        }
    }
}
