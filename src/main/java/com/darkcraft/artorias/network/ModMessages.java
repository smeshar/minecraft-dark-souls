package com.darkcraft.artorias.network;

import com.darkcraft.artorias.ArtoriasMod;
import com.darkcraft.artorias.combat.DodgeRollManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ModMessages {
    public static final Identifier ROLL_ID = ArtoriasMod.id("dodge_roll");

    public record DodgeRollPayload(double inputX, double inputZ) implements CustomPayload {
        public static final Id<DodgeRollPayload> ID = new Id<>(ROLL_ID);
        public static final PacketCodec<PacketByteBuf, DodgeRollPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeDouble(value.inputX);
                    buf.writeDouble(value.inputZ);
                },
                buf -> new DodgeRollPayload(buf.readDouble(), buf.readDouble())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(DodgeRollPayload.ID, DodgeRollPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DodgeRollPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                DodgeRollManager.performRoll(context.player(), payload.inputX(), payload.inputZ());
            });
        });
    }
}
