package com.darkcraft.artorias.entity.omen;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class OmenRenderer extends BipedEntityRenderer<OmenEntity, OmenRenderState, OmenModel> {
    public static final EntityModelLayer OMEN_LAYER = new EntityModelLayer(ArtoriasMod.id("omen"), "main");
    private static final Identifier CLEAVER_TEXTURE = ArtoriasMod.id("textures/entity/omen_cleaver.png");
    private static final Identifier BRAWLER_TEXTURE = ArtoriasMod.id("textures/entity/omen_brawler.png");

    public OmenRenderer(EntityRendererFactory.Context context) {
        super(context, new OmenModel(context.getPart(OMEN_LAYER)), 0.75f);
    }

    @Override
    public OmenRenderState createRenderState() {
        return new OmenRenderState();
    }

    @Override
    public void updateRenderState(OmenEntity entity, OmenRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.attackState = entity.getAttackState();
        state.attackTicks = entity.getAttackTicks();
        state.isStaggered = entity.isStaggered();
        state.variant = entity.getVariant();
        state.ageInTicks = entity.age + tickDelta;
    }

    @Override
    protected void scale(OmenRenderState state, MatrixStack matrices) {
        // Massive, broad, heavy Leyndell sewer omen proportions
        matrices.scale(1.4f, 1.45f, 1.4f);
    }

    @Override
    public Identifier getTexture(OmenRenderState state) {
        return state.variant == OmenEntity.Variant.CLEAVER ? CLEAVER_TEXTURE : BRAWLER_TEXTURE;
    }
}

