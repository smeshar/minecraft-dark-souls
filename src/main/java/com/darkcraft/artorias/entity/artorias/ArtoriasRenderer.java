package com.darkcraft.artorias.entity.artorias;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ArtoriasRenderer extends BipedEntityRenderer<ArtoriasEntity, ArtoriasRenderState, ArtoriasModel> {
    public static final EntityModelLayer ARTORIAS_LAYER = new EntityModelLayer(ArtoriasMod.id("knight_artorias"), "main");
    private static final Identifier TEXTURE = ArtoriasMod.id("textures/entity/artorias.png");

    public ArtoriasRenderer(EntityRendererFactory.Context context) {
        super(context, new ArtoriasModel(context.getPart(ARTORIAS_LAYER)), 0.6f);
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
        state.attackTicks = entity.getAttackTicks();
        state.ageInTicks = entity.age + tickDelta;
    }

    @Override
    protected void scale(ArtoriasRenderState state, MatrixStack matrices) {
        // Lean, tall, menacing Dark Souls boss silhouette instead of chunky golem
        matrices.scale(1.15f, 1.45f, 1.15f);
    }

    @Override
    public Identifier getTexture(ArtoriasRenderState state) {
        return TEXTURE;
    }
}
