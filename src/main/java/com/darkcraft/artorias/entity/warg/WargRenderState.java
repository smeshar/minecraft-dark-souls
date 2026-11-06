package com.darkcraft.artorias.entity.warg;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

public class WargRenderState extends LivingEntityRenderState {
    public int attackState = 0;
    public int attackTicks = 0;
    public boolean isGiant = false;
    public float ageInTicks = 0.0f;
}

