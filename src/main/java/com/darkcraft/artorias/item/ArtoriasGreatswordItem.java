package com.darkcraft.artorias.item;

import com.darkcraft.artorias.entity.artorias.ArtoriasEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ArtoriasGreatswordItem extends SwordItem {
    public ArtoriasGreatswordItem(Settings settings) {
        super(ToolMaterials.NETHERITE, settings.attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 12, -2.4f)));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!player.getItemCooldownManager().isCoolingDown(this)) {
            player.getItemCooldownManager().set(this, 80); // 4 seconds cooldown

            // Artorias Leaping Somersault Flip Slam
            Vec3d look = player.getRotationVec(1.0f);
            Vec3d leap = new Vec3d(look.x, 0, look.z).normalize().multiply(1.25).add(0, 0.72, 0);

            player.setVelocity(leap);
            player.velocityModified = true;

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 0.7f);

            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                serverWorld.getServer().execute(() -> {
                    triggerSlamDelayed(serverWorld, player);
                });
            }

            return TypedActionResult.success(stack, world.isClient());
        }

        return TypedActionResult.pass(stack);
    }

    private void triggerSlamDelayed(ServerWorld world, PlayerEntity player) {
        new Thread(() -> {
            try {
                Thread.sleep(600); // 0.6s apex timing
            } catch (InterruptedException ignored) {}

            world.getServer().execute(() -> {
                if (!player.isAlive()) return;
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();

                // Shockwave VFX
                world.spawnParticles(ParticleTypes.EXPLOSION, px, py, pz, 2, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py + 0.2, pz, 30, 1.5, 0.2, 1.5, 0.08);

                world.playSound(null, px, py, pz,
                        SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                world.playSound(null, px, py, pz,
                        SoundEvents.BLOCK_HEAVY_CORE_STEP, SoundCategory.PLAYERS, 1.5f, 0.6f);

                // AOE damage to enemies
                Box damageBox = new Box(px - 4.5, py - 2.0, pz - 4.5, px + 4.5, py + 3.0, pz + 4.5);
                for (Entity entity : world.getOtherEntities(player, damageBox)) {
                    if (entity instanceof LivingEntity target && !(entity instanceof PlayerEntity)) {
                        target.damage(world.getDamageSources().playerAttack(player), 24.0f);
                        target.takeKnockback(0.9, px - target.getX(), pz - target.getZ());

                        // Heavy poise damage if target is Artorias
                        if (target instanceof ArtoriasEntity boss) {
                            boss.applyPoiseDamage(45.0f, player);
                        }
                    }
                }
            });
        }).start();
    }
}
