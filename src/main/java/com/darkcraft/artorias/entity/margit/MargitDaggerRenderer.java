package com.darkcraft.artorias.entity.margit;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;

/**
 * Renderer for Margit's spectral golden dagger projectile.
 * Visual flight is represented by client-side luminous golden particle trails.
 */
public class MargitDaggerRenderer extends EntityRenderer<MargitDaggerEntity, EntityRenderState> {

    public MargitDaggerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
