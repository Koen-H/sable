package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.util.SableBufferUtils;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

public record ClientboundFreezePlayerPacket(UUID subLevelID, Vector3dc localPosition) implements SableTCPPacket {

    private static final UUID UNFREEZE_UUID = new UUID(0, 0);

    public static final Type<ClientboundFreezePlayerPacket> TYPE = new Type<>(Sable.sablePath("freeze_player"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundFreezePlayerPacket> CODEC = StreamCodec.of((buf, value) -> value.write(buf), ClientboundFreezePlayerPacket::read);

    public static ClientboundFreezePlayerPacket unfreeze() {
        return new ClientboundFreezePlayerPacket(UNFREEZE_UUID, new Vector3d());
    }

    private static ClientboundFreezePlayerPacket read(final FriendlyByteBuf buf) {
        return new ClientboundFreezePlayerPacket(buf.readUUID(), SableBufferUtils.read(buf, new Vector3d()));
    }

    private void write(final FriendlyByteBuf buf) {
        buf.writeUUID(this.subLevelID);
        SableBufferUtils.write(buf, this.localPosition);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(final PacketContext context) {
        final Player player = context.player();
        assert player != null;

        if (UNFREEZE_UUID.equals(this.subLevelID)) {
            ((PlayerFreezeExtension) player).sable$freezeTo(null, null);
        } else {
            ((PlayerFreezeExtension) player).sable$freezeTo(this.subLevelID, this.localPosition);
        }
    }
}