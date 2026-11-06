package com.darkcraft.artorias.entity.margit;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renderer for Margit, the Fell Omen.
 * Configures boss scale (1.35x), texture binding, and animation render states.
 */
public class MargitRenderer extends BipedEntityRenderer<MargitEntity, MargitRenderState, MargitModel> {
    public static final EntityModelLayer MARGIT_LAYER = new EntityModelLayer(ArtoriasMod.id("margit_the_fell_omen"), "main");
    private static final Identifier TEXTURE = ArtoriasMod.id("textures/entity/margit.png");

    public MargitRenderer(EntityRendererFactory.Context context) {
        super(context, new MargitModel(context.getPart(MARGIT_LAYER)), 0.75f);
    }

    @Override
    public MargitRenderState createRenderState() {
        return new MargitRenderState();
    }

    @Override
    public void updateRenderState(MargitEntity entity, MargitRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.attackState = entity.getAttackState();
        state.isStunned = entity.isStunned();
        state.isPinned = entity.isPinned();
        state.isPhaseTwo = entity.isPhaseTwo();
        state.attackTicks = entity.getAttackTicks();
        state.ageInTicks = entity.age + tickDelta;
    }

    @Override
    protected void scale(MargitRenderState state, MatrixStack matrices) {
        // Imposing Elden Ring Boss silhouette: tall, menacing, broad-shouldered
        matrices.scale(1.35f, 1.35f, 1.35f);
    }

    @Override
    public Identifier getTexture(MargitRenderState state) {
        return TEXTURE;
    }
}
