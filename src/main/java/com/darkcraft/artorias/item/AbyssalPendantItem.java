package com.darkcraft.artorias.item;

import com.darkcraft.artorias.ArtoriasMod;
import com.darkcraft.artorias.entity.artorias.ArtoriasEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class AbyssalPendantItem extends Item {
    public AbyssalPendantItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) context.getWorld();
            BlockPos spawnPos = context.getBlockPos().offset(context.getSide());

            ArtoriasEntity boss = ArtoriasMod.ARTORIAS_ENTITY.create(world);
            if (boss != null) {
                boss.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, context.getPlayerYaw(), 0.0f);
                world.spawnEntity(boss);
                if (context.getPlayer() != null) {
                    boss.setTarget(context.getPlayer());
                }

                // Dramatic awakening visual and sound effects
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, 3, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, 40, 1.0, 1.5, 1.0, 0.08);
                world.spawnParticles(ParticleTypes.WITCH, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, 30, 0.8, 1.2, 0.8, 0.05);

                world.playSound(null, spawnPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1.8f, 0.7f);
                world.playSound(null, spawnPos, SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1.5f, 0.8f);

                world.getServer().getPlayerManager().broadcast(
                        Text.translatable("message.artorias.boss_spawn"),
                        false
                );

                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    context.getStack().decrement(1);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.CONSUME;
    }
}
