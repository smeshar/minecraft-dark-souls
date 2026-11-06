package com.darkcraft.artorias.entity.goblin;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GoblinRenderer extends BipedEntityRenderer<GoblinEntity, GoblinModel> {
    public static final EntityModelLayer GOBLIN_LAYER = new EntityModelLayer(ArtoriasMod.id("goblin"), "main");
    private static final Identifier GOBLIN_TEXTURE = ArtoriasMod.id("textures/entity/goblin.png");
    private static final Identifier GOBLIN_KNIGHT_TEXTURE = ArtoriasMod.id("textures/entity/goblin_knight.png");

    public GoblinRenderer(EntityRendererFactory.Context context) {
        super(context, new GoblinModel(context.getPart(GOBLIN_LAYER)), 0.45f);
    }

    @Override
    protected void scale(GoblinEntity entity, MatrixStack matrices, float amount) {
        if (entity instanceof GoblinKnightEntity) {
            matrices.scale(0.85f, 0.85f, 0.85f);
        } else {
            matrices.scale(0.75f, 0.75f, 0.75f);
        }
    }

    @Override
    public Identifier getTexture(GoblinEntity entity) {
        return (entity instanceof GoblinKnightEntity) ? GOBLIN_KNIGHT_TEXTURE : GOBLIN_TEXTURE;
    }
}
