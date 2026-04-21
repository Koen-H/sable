package dev.ryanhcode.sable.mixin.entity.teleport_players;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFreezePlayerPacket;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Handles Mekanism's same-dimension player teleport path, which calls
 * player.connection.teleport() directly instead of ServerPlayer.teleportTo().
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @WrapMethod(method = "teleport(DDDFF)V")
    private void sable$teleport(final double x, final double y, final double z, final float yRot, final float xRot, final Operation<Void> original) {
        final ServerLevel level = this.player.serverLevel();
        final Vector3d localPos = new Vector3d(x, y, z);
        final SubLevel subLevel = Sable.HELPER.getContaining(level, localPos);

        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final Vector3d globalPos = Sable.HELPER.projectOutOfSubLevel(level, localPos, new Vector3d());
            original.call(globalPos.x, globalPos.y, globalPos.z, yRot, xRot);
            ((PlayerFreezeExtension) this.player).sable$freezeTo(serverSubLevel.getUniqueId(), localPos);
            this.player.connection.send(new ClientboundCustomPayloadPacket(
                    new ClientboundFreezePlayerPacket(serverSubLevel.getUniqueId(), localPos)
            ));
        } else {
            ((PlayerFreezeExtension) this.player).sable$freezeTo(null, null);
            this.player.connection.send(new ClientboundCustomPayloadPacket(ClientboundFreezePlayerPacket.unfreeze()));
            original.call(x, y, z, yRot, xRot);
        }
    }
}
