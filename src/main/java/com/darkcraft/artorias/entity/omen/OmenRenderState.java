package com.darkcraft.artorias.entity.omen;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;

public class OmenRenderState extends BipedEntityRenderState {
    public int attackState = 0;
    public int attackTicks = 0;
    public boolean isStaggered = false;
    public OmenEntity.Variant variant = OmenEntity.Variant.CLEAVER;
    public float ageInTicks = 0.0f;
}
