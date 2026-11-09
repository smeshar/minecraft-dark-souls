package com.darkcraft.artorias.entity.margit;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

/**
 * Renderer for Margit's spectral golden dagger projectile.
 * Visual flight is represented by client-side luminous golden particle trails.
 */
public class MargitDaggerRenderer extends EntityRenderer<MargitDaggerEntity> {
    private static final Identifier TEXTURE = ArtoriasMod.id("textures/entity/margit_dagger.png");

    public MargitDaggerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(MargitDaggerEntity entity) {
        return TEXTURE;
    }
}
