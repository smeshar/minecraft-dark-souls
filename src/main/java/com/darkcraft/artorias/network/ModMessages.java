package com.darkcraft.artorias.network;

import com.darkcraft.artorias.ArtoriasMod;
import com.darkcraft.artorias.combat.DodgeRollManager;
import com.darkcraft.artorias.combat.GroundSlamManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ModMessages {
    public static final Identifier ROLL_ID = ArtoriasMod.id("dodge_roll");
    public static final Identifier GROUND_SLAM_C2S_ID = ArtoriasMod.id("ground_slam_c2s");
    public static final Identifier GROUND_SLAM_START_S2C_ID = ArtoriasMod.id("ground_slam_start_s2c");
    public static final Identifier GROUND_SLAM_IMPACT_S2C_ID = ArtoriasMod.id("ground_slam_impact_s2c");

    // 1. Dodge Roll C2S Payload
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

    // 2. Ground Slam Trigger C2S Payload
    public record GroundSlamPayload() implements CustomPayload {
        public static final Id<GroundSlamPayload> ID = new Id<>(GROUND_SLAM_C2S_ID);
        public static final PacketCodec<PacketByteBuf, GroundSlamPayload> CODEC = PacketCodec.of(
                (value, buf) -> {},
                buf -> new GroundSlamPayload()
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 3. Ground Slam Start S2C Payload
    public record GroundSlamStartPayload(int entityId) implements CustomPayload {
        public static final Id<GroundSlamStartPayload> ID = new Id<>(GROUND_SLAM_START_S2C_ID);
        public static final PacketCodec<PacketByteBuf, GroundSlamStartPayload> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeInt(value.entityId),
                buf -> new GroundSlamStartPayload(buf.readInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 4. Ground Slam Impact S2C Payload
    public record GroundSlamImpactPayload(int entityId, double x, double y, double z) implements CustomPayload {
        public static final Id<GroundSlamImpactPayload> ID = new Id<>(GROUND_SLAM_IMPACT_S2C_ID);
        public static final PacketCodec<PacketByteBuf, GroundSlamImpactPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeInt(value.entityId);
                    buf.writeDouble(value.x);
                    buf.writeDouble(value.y);
                    buf.writeDouble(value.z);
                },
                buf -> new GroundSlamImpactPayload(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void registerC2SPackets() {
        // Register Codecs
        PayloadTypeRegistry.playC2S().register(DodgeRollPayload.ID, DodgeRollPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GroundSlamPayload.ID, GroundSlamPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(GroundSlamStartPayload.ID, GroundSlamStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GroundSlamImpactPayload.ID, GroundSlamImpactPayload.CODEC);

        // Server Receivers
        ServerPlayNetworking.registerGlobalReceiver(DodgeRollPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                DodgeRollManager.performRoll(context.player(), payload.inputX(), payload.inputZ());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(GroundSlamPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                GroundSlamManager.initiateSlam(context.player());
            });
        });
    }
}
