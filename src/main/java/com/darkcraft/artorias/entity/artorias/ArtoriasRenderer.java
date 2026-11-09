package com.darkcraft.artorias.entity.artorias;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ArtoriasRenderer extends BipedEntityRenderer<ArtoriasEntity, ArtoriasModel> {
    public static final EntityModelLayer ARTORIAS_LAYER = new EntityModelLayer(ArtoriasMod.id("knight_artorias"), "main");
    private static final Identifier TEXTURE = ArtoriasMod.id("textures/entity/artorias.png");

    public ArtoriasRenderer(EntityRendererFactory.Context context) {
        super(context, new ArtoriasModel(context.getPart(ARTORIAS_LAYER)), 0.6f);
        this.addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
    }

    @Override
    protected void scale(ArtoriasEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.15f, 1.45f, 1.15f);
    }

    @Override
    public Identifier getTexture(ArtoriasEntity entity) {
        return TEXTURE;
    }
}
