package com.darkcraft.artorias.entity.omen;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

public class OmenEntity extends HostileEntity {
    public enum Variant {
        CLEAVER,
        BRAWLER
    }

    public static final int STATE_IDLE = 0;
    public static final int STATE_OVERHEAD_SLAM = 1;
    public static final int STATE_KICK = 2;
    public static final int STATE_STOMP = 3;
    public static final int STATE_CURSED_BREATH = 4;
    public static final int STATE_STAGGERED = 5;

    public static final float MAX_POISE = 90.0f;

    private static final TrackedData<Integer> ATTACK_STATE = DataTracker.registerData(OmenEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(OmenEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> POISE = DataTracker.registerData(OmenEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private int attackTicks = 0;
    private int specialCooldown = 40;

    public OmenEntity(EntityType<? extends OmenEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 35;
    }

    public OmenEntity(EntityType<? extends OmenEntity> entityType, World world, Variant variant) {
        this(entityType, world);
        setVariant(variant);
    }

    public static DefaultAttributeContainer.Builder createCleaverAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 85.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 11.0)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 4.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    public static DefaultAttributeContainer.Builder createBrawlerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 75.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 2.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_STATE, STATE_IDLE);
        builder.add(VARIANT, Variant.CLEAVER.ordinal());
        builder.add(POISE, MAX_POISE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new OmenCombatGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.85));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public Variant getVariant() {
        return Variant.values()[this.dataTracker.get(VARIANT)];
    }

    public void setVariant(Variant variant) {
        this.dataTracker.set(VARIANT, variant.ordinal());
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

    public boolean isStaggered() {
        return this.getAttackState() == STATE_STAGGERED;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Poise damage calculation
        if (!isStaggered()) {
            float currentPoise = this.dataTracker.get(POISE);
            float poiseDmg = amount * 1.5f;
            currentPoise -= poiseDmg;

            if (currentPoise <= 0) {
                // Poise Break!
                this.setAttackState(STATE_STAGGERED);
                this.attackTicks = 65; // ~3.25 seconds
                this.dataTracker.set(POISE, MAX_POISE);

                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 1.8f, 0.6f);
                if (this.getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5, this.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                }
            } else {
                this.dataTracker.set(POISE, currentPoise);
            }
        }

        return super.damage(source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (specialCooldown > 0) specialCooldown--;

        if (this.attackTicks > 0) {
            this.attackTicks--;

            // Ongoing effect handling for multi-tick attacks
            if (this.getAttackState() == STATE_CURSED_BREATH && this.getWorld() instanceof ServerWorld sw) {
                Vec3d look = this.getRotationVec(1.0f);
                Vec3d mouth = this.getPos().add(0, 2.0, 0).add(look.multiply(0.8));
                sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, mouth.x, mouth.y, mouth.z, 6, 0.3, 0.3, 0.3, 0.08);
                sw.spawnParticles(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, 3, 0.2, 0.2, 0.2, 0.04);

                Box cone = new Box(this.getBlockPos()).expand(4.0);
                for (LivingEntity target : sw.getEntitiesByClass(LivingEntity.class, cone, e -> e != this && !(e instanceof OmenEntity))) {
                    if (this.canSee(target)) {
                        target.damage(this.getWorld().getDamageSources().mobAttack(this), 2.5f);
                        target.setOnFireFor(3.0f);
                    }
                }
            }

            if (this.attackTicks == 0 && !this.getWorld().isClient()) {
                setAttackState(STATE_IDLE);
            }
        }
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        int count = 2 + world.getRandom().nextInt(4); // 2 to 5 rations
        for (int i = 0; i < count; i++) {
            this.dropItem(ArtoriasMod.SOUL_RATION);
        }
    }

    public void triggerOverheadSlam(LivingEntity target) {
        if (!this.getWorld().isClient()) {
            this.setAttackState(STATE_OVERHEAD_SLAM);
            this.attackTicks = 26;

            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.6f, 0.7f);

            // Trigger delayed impact damage at apex
            if (this.getWorld() instanceof ServerWorld sw) {
                sw.getServer().execute(() -> {
                    new Thread(() -> {
                        try {
                            Thread.sleep(650); // Windup duration
                        } catch (InterruptedException ignored) {}

                        sw.getServer().execute(() -> {
                            if (!this.isAlive() || this.isStaggered()) return;

                            Vec3d look = this.getRotationVec(1.0f);
                            Vec3d impact = this.getPos().add(look.multiply(2.2));

                            sw.playSound(null, impact.x, impact.y, impact.z,
                                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.4f, 0.8f);
                            sw.playSound(null, impact.x, impact.y, impact.z,
                                    SoundEvents.BLOCK_HEAVY_CORE_BREAK, SoundCategory.HOSTILE, 1.8f, 0.5f);

                            sw.spawnParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.2, impact.z, 2, 0.4, 0.2, 0.4, 0.05);
                            sw.spawnParticles(ParticleTypes.GUST_EMITTER_LARGE, impact.x, impact.y + 0.2, impact.z, 1, 0, 0, 0, 0);

                            Box aoe = new Box(impact.x - 2.5, impact.y - 1.0, impact.z - 2.5, impact.x + 2.5, impact.y + 2.5, impact.z + 2.5);
                            for (LivingEntity victim : sw.getEntitiesByClass(LivingEntity.class, aoe, e -> e != this && !(e instanceof OmenEntity))) {
                                victim.damage(this.getWorld().getDamageSources().mobAttack(this), 16.0f);
                                victim.takeKnockback(1.2, this.getX() - victim.getX(), this.getZ() - victim.getZ());
                            }
                        });
                    }).start();
                });
            }
        }
    }

    public void triggerStomp() {
        if (!this.getWorld().isClient()) {
            this.setAttackState(STATE_STOMP);
            this.attackTicks = 18;

            if (this.getWorld() instanceof ServerWorld sw) {
                sw.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.HOSTILE, 1.5f, 0.6f);
                sw.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 1, 0.3, 0.1, 0.3, 0.02);

                Box aoe = this.getBoundingBox().expand(3.0);
                for (LivingEntity victim : sw.getEntitiesByClass(LivingEntity.class, aoe, e -> e != this && !(e instanceof OmenEntity))) {
                    victim.damage(this.getWorld().getDamageSources().mobAttack(this), 9.0f);
                    victim.setVelocity(victim.getVelocity().add(0, 0.5, 0));
                    victim.velocityModified = true;
                }
            }
        }
    }

    public void triggerKick(LivingEntity target) {
        if (!this.getWorld().isClient()) {
            this.setAttackState(STATE_KICK);
            this.attackTicks = 16;

            if (this.getWorld() instanceof ServerWorld sw) {
                this.tryAttack(target);
                target.takeKnockback(1.8, this.getX() - target.getX(), this.getZ() - target.getZ());
                sw.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.HOSTILE, 1.4f, 0.7f);
            }
        }
    }

    public void triggerCursedBreath() {
        if (!this.getWorld().isClient()) {
            this.setAttackState(STATE_CURSED_BREATH);
            this.attackTicks = 30;

            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.4f, 0.6f);
        }
    }

    // Omen moveset AI
    static class OmenCombatGoal extends Goal {
        private final OmenEntity omen;

        public OmenCombatGoal(OmenEntity omen) {
            this.omen = omen;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = omen.getTarget();
            return target != null && target.isAlive() && !omen.isStaggered();
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = omen.getTarget();
            return target != null && target.isAlive() && !omen.isStaggered();
        }

        @Override
        public void tick() {
            LivingEntity target = omen.getTarget();
            if (target == null) return;

            omen.getLookControl().lookAt(target, 30.0f, 30.0f);
            double distSq = omen.squaredDistanceTo(target);

            if (omen.getAttackState() != STATE_IDLE) {
                // Currently executing an attack animation
                omen.getNavigation().stop();
                return;
            }

            if (omen.getVariant() == Variant.CLEAVER) {
                // Cleaver moveset
                if (distSq <= 16.0 && omen.specialCooldown <= 0 && omen.getRandom().nextFloat() < 0.35f) {
                    omen.triggerOverheadSlam(target);
                    omen.specialCooldown = 90;
                    return;
                }
                if (distSq > 16.0 && distSq < 64.0 && omen.specialCooldown <= 0 && omen.getRandom().nextFloat() < 0.3f) {
                    omen.triggerCursedBreath();
                    omen.specialCooldown = 120;
                    return;
                }
            } else {
                // Brawler moveset
                if (distSq <= 8.0 && omen.getRandom().nextFloat() < 0.4f) {
                    omen.triggerKick(target);
                    return;
                }
                if (distSq <= 16.0 && omen.specialCooldown <= 0 && omen.getRandom().nextFloat() < 0.35f) {
                    omen.triggerStomp();
                    omen.specialCooldown = 70;
                    return;
                }
            }

            // Normal approach and melee
            if (distSq <= 6.0) {
                omen.tryAttack(target);
            } else {
                omen.getNavigation().startMovingTo(target, 1.15);
            }
        }
    }
}
