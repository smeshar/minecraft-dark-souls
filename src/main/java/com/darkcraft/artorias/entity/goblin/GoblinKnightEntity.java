package com.darkcraft.artorias.entity.goblin;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GoblinKnightEntity extends GoblinEntity {
    public static final int STATE_GUARDING = 4;
    public static final int STATE_SHIELD_BASH = 5;
    public static final int STATE_COUNTER = 6;

    private int guardCooldown = 60;

    public GoblinKnightEntity(EntityType<? extends GoblinKnightEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 25;
    }

    public static DefaultAttributeContainer.Builder createGoblinKnightAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 45.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.ARMOR, 8.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(EntityAttributes.FOLLOW_RANGE, 26.0);
    }

    public boolean isGuarding() {
        return this.getAttackState() == STATE_GUARDING;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Shield parry & block when guarding against frontal melee
        if (isGuarding() && source.getAttacker() instanceof LivingEntity attacker) {
            Vec3d look = this.getRotationVec(1.0f);
            Vec3d toAttacker = attacker.getEntityPos().subtract(this.getEntityPos()).normalize();
            double dot = look.dotProduct(toAttacker);

            if (dot > 0.3) {
                // Frontal block successful!
                world.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_SHIELD_BLOCK.value(), SoundCategory.HOSTILE, 1.5f, 1.2f);
                world.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.8, this.getZ(), 10, 0.3, 0.3, 0.3, 0.1);

                // Immediate critical counter-thrust riposte!
                triggerRiposte(attacker);
                return false;
            }
        }

        return super.damage(world, source, amount);
    }

    public void triggerGuard() {
        if (!this.getEntityWorld().isClient() && guardCooldown <= 0) {
            this.setAttackState(STATE_GUARDING);
            this.attackTicks = 35; // 1.75 sec guard stance
            this.guardCooldown = 100;

            this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), SoundCategory.HOSTILE, 1.2f, 1.0f);
        }
    }

    public void triggerRiposte(LivingEntity target) {
        if (!this.getEntityWorld().isClient()) {
            this.setAttackState(STATE_COUNTER);
            this.attackTicks = 16;

            if (this.getEntityWorld() instanceof ServerWorld sw) {
                sw.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 1.4f, 1.1f);
                target.damage(sw, sw.getDamageSources().mobAttack(this), 10.0f);
                target.takeKnockback(0.8, this.getX() - target.getX(), this.getZ() - target.getZ());
            }
        }
    }

    public void triggerShieldBash(LivingEntity target) {
        if (!this.getEntityWorld().isClient()) {
            this.setAttackState(STATE_SHIELD_BASH);
            this.attackTicks = 18;

            if (this.getEntityWorld() instanceof ServerWorld sw) {
                this.tryAttack(sw, target);
                target.takeKnockback(1.4, this.getX() - target.getX(), this.getZ() - target.getZ());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));
                sw.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_SHIELD_BLOCK.value(), SoundCategory.HOSTILE, 1.4f, 0.8f);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (guardCooldown > 0) guardCooldown--;

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.getAttackState() == STATE_IDLE) {
            double distSq = this.squaredDistanceTo(target);
            if (distSq <= 12.0 && guardCooldown <= 0 && this.getRandom().nextFloat() < 0.2f) {
                triggerGuard();
            } else if (distSq <= 6.0 && this.getRandom().nextFloat() < 0.25f) {
                triggerShieldBash(target);
            }
        }
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        // Extra drops for goblin knight
        this.dropItem(world, ArtoriasMod.SOUL_RATION);
        if (world.getRandom().nextFloat() < 0.35f) {
            this.dropItem(world, Items.IRON_INGOT);
        }
    }
}
