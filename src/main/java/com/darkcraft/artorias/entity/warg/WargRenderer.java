package com.darkcraft.artorias.entity.warg;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class WargRenderer extends MobEntityRenderer<WargEntity, WargRenderState, WargModel> {
    public static final EntityModelLayer WARG_LAYER = new EntityModelLayer(ArtoriasMod.id("warg"), "main");
    private static final Identifier GIANT_TEXTURE = ArtoriasMod.id("textures/entity/giant_warg.png");
    private static final Identifier LESSER_TEXTURE = ArtoriasMod.id("textures/entity/lesser_warg.png");

    public WargRenderer(EntityRendererFactory.Context context) {
        super(context, new WargModel(context.getPart(WARG_LAYER)), 0.6f);
    }

    @Override
    public WargRenderState createRenderState() {
        return new WargRenderState();
    }

    @Override
    public void updateRenderState(WargEntity entity, WargRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.attackState = entity.getAttackState();
        state.attackTicks = entity.getAttackTicks();
        state.isGiant = entity.isGiant();
        state.ageInTicks = entity.age + tickDelta;
    }

    @Override
    protected void scale(WargRenderState state, MatrixStack matrices) {
        if (state.isGiant) {
            matrices.scale(1.6f, 1.6f, 1.6f);
        } else {
            matrices.scale(0.95f, 0.95f, 0.95f);
        }
    }

    @Override
    public Identifier getTexture(WargRenderState state) {
        return state.isGiant ? GIANT_TEXTURE : LESSER_TEXTURE;
    }
}
