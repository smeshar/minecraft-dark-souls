package com.darkcraft.artorias.entity.margit;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Spectral golden dagger thrown by Margit, the Fell Omen.
 * Flies with a golden trail, deals magic/pierce damage, and shatters on impact.
 */
public class MargitDaggerEntity extends ProjectileEntity {
    private static final float DAMAGE = 8.0f;
    private int lifeTicks = 0;

    public MargitDaggerEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = false;
    }

    public MargitDaggerEntity(World world, LivingEntity owner) {
        this(ArtoriasMod.MARGIT_DAGGER_ENTITY, world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onCollision(hitResult);
        }

        Vec3d velocity = this.getVelocity();
        this.setPosition(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
        ProjectileUtil.setRotationFromVelocity(this, 0.5f);

        // Spawn glowing golden trail particles
        if (this.getEntityWorld().isClient()) {
            this.getEntityWorld().addParticleClient(ParticleTypes.WAX_ON, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.getEntityWorld().addParticleClient(ParticleTypes.GLOW, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }

        lifeTicks++;
        if (lifeTicks > 100 && !this.getEntityWorld().isClient()) {
            this.discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity target = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (target != owner && target instanceof LivingEntity livingTarget) {
            World world = this.getEntityWorld();
            DamageSource damageSource = world.getDamageSources().indirectMagic(this, owner);
            livingTarget.damage((ServerWorld) world, damageSource, DAMAGE);

            // Stagger target slightly
            Vec3d knockback = this.getVelocity().normalize().multiply(0.4);
            livingTarget.addVelocity(knockback.x, 0.1, knockback.z);
            livingTarget.velocityDirty = true;

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY(), this.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.0f, 1.8f);
            }
            this.discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        World world = this.getEntityWorld();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY(), this.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1.0f, 1.6f);
        }
        this.discard();
    }
}
