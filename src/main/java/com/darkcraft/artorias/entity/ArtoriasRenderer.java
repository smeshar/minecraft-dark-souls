package com.darkcraft.artorias.entity;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ArtoriasRenderer extends BipedEntityRenderer<ArtoriasEntity, ArtoriasRenderState, ArtoriasModel> {
    private static final Identifier TEXTURE = ArtoriasMod.id("textures/entity/artorias.png");

    public ArtoriasRenderer(EntityRendererFactory.Context context) {
        super(context, new ArtoriasModel(context.getPart(EntityModelLayers.PLAYER)), 0.7f);
        this.addFeature(new HeldItemFeatureRenderer<>(this));
    }

    @Override
    public ArtoriasRenderState createRenderState() {
        return new ArtoriasRenderState();
    }

    @Override
    public void updateRenderState(ArtoriasEntity entity, ArtoriasRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.attackState = entity.getAttackState();
        state.isStaggered = entity.isStaggered();
        state.isPhaseTwo = entity.isPhaseTwo();
    }

    @Override
    protected void scale(ArtoriasRenderState state, MatrixStack matrices) {
        float scale = 1.38f;
        matrices.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTexture(ArtoriasRenderState state) {
        return TEXTURE;
    }
}
