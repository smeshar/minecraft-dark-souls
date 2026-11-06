package com.darkcraft.artorias.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class SoulRationItem extends Item {
    public SoulRationItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (user.canConsume(false)) {
            if (!world.isClient()) {
                user.getHungerManager().add(6, 1.2f);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 0.9f, 1.0f);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.6f, 1.0f);

                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                            new ItemStackParticleEffect(ParticleTypes.ITEM, itemStack),
                            user.getX(), user.getEyeY() - 0.1, user.getZ(),
                            14, 0.15, 0.15, 0.15, 0.05
                    );
                }
            }
            if (!user.isCreative()) {
                itemStack.decrement(1);
            }
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.translatable("item.artorias.soul_ration.desc").formatted(Formatting.GOLD));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
