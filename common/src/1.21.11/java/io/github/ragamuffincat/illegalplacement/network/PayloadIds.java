package io.github.ragamuffincat.illegalplacement.network;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

final class PayloadIds {

    private PayloadIds() {
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(IllegalPlacement.MOD_ID, path));
    }
}
