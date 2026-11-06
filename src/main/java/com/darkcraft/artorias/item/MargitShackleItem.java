package com.darkcraft.artorias.item;

import com.darkcraft.artorias.entity.margit.MargitEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

/**
 * Margit's Shackle: A sacred fetish cursed with golden omen chains.
 * When used within 15 blocks of Margit, the Fell Omen during Phase 1 (HP > 60%),
 * slams him to the earth and pins him for 3 seconds.
 * Usable a maximum of 2 times per boss encounter.
 */
public class MargitShackleItem extends Item {
    public static final double MAX_DISTANCE = 15.0;
    private static final int COOLDOWN_TICKS = 300; // 15 seconds cooldown

    public MargitShackleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.FAIL;
        }

        Box searchBox = Box.of(player.getEntityPos(), MAX_DISTANCE * 2, MAX_DISTANCE * 2, MAX_DISTANCE * 2);
        List<MargitEntity> nearbyMargits = world.getEntitiesByClass(MargitEntity.class, searchBox, MargitEntity::isAlive);

        if (nearbyMargits.isEmpty()) {
            if (!world.isClient()) {
                player.sendMessage(Text.translatable("message.artorias.shackle_no_boss").formatted(Formatting.GRAY), true);
            }
            return ActionResult.PASS;
        }

        // Target the closest Margit within range
        MargitEntity targetBoss = nearbyMargits.stream()
                .min(Comparator.comparingDouble(player::squaredDistanceTo))
                .orElse(null);

        if (targetBoss == null) {
            return ActionResult.PASS;
        }

        // Phase 2 immunity check
        if (targetBoss.isPhaseTwo()) {
            if (!world.isClient()) {
                player.sendMessage(Text.translatable("message.artorias.shackle_phase_two_resist").formatted(Formatting.GOLD), true);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_ANVIL_HIT, SoundCategory.PLAYERS, 1.2f, 0.5f);
            }
            player.getItemCooldownManager().set(stack, 60);
            return ActionResult.FAIL;
        }

        // Maximum 2 uses per boss fight
        if (targetBoss.getShackleUses() >= 2) {
            if (!world.isClient()) {
                player.sendMessage(Text.translatable("message.artorias.shackle_exhausted").formatted(Formatting.RED), true);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.8f);
            }
            player.getItemCooldownManager().set(stack, 60);
            return ActionResult.FAIL;
        }

        // Apply Shackle pinning effect
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            boolean success = targetBoss.applyShackle();
            if (success) {
                // Golden beam and seal particles between player and Margit
                double dx = targetBoss.getX() - player.getX();
                double dy = (targetBoss.getY() + 1.0) - (player.getY() + 1.0);
                double dz = targetBoss.getZ() - player.getZ();
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

                int particleSteps = (int) (length * 4);
                for (int i = 0; i <= particleSteps; i++) {
                    double progress = (double) i / particleSteps;
                    double px = player.getX() + dx * progress;
                    double py = player.getY() + 1.0 + dy * progress;
                    double pz = player.getZ() + dz * progress;
                    serverWorld.spawnParticles(ParticleTypes.WAX_ON, px, py, pz, 1, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.GLOW, px, py, pz, 1, 0, 0, 0, 0);
                }

                serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 2.0f, 0.8f);

                player.sendMessage(Text.translatable("message.artorias.shackle_bound").formatted(Formatting.GOLD), true);
                player.getItemCooldownManager().set(stack, COOLDOWN_TICKS);
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.SUCCESS;
    }
}
