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
import net.minecraft.world.entity.RelativeMovement;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Shadow
    public ServerGamePacketListenerImpl connection;

    @WrapMethod(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z")
    public boolean sable$teleportTo(final ServerLevel serverLevel, final double x, final double y, final double z, final Set<RelativeMovement> set, final float g, final float h, final Operation<Boolean> original) {
        final Vector3d localPos = new Vector3d(x, y, z);
        final SubLevel subLevel = Sable.HELPER.getContaining(serverLevel, localPos);

        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            // Teleport directly to the chunk-grid position — entities in sub-levels live at chunk-grid coords.
            // Do NOT use projectOutOfSubLevel here: that converts to visual coords, but sable$teleport()
            // on the client calls lastPose().transformPosition(anchor) which expects chunk-grid input,
            // causing a ~20M desync if we pre-transform.
            final boolean result = original.call(serverLevel, x, y, z, set, g, h);

            if (result) {
                ((PlayerFreezeExtension) this).sable$freezeTo(serverSubLevel.getUniqueId(), localPos);
                this.connection.send(new ClientboundCustomPayloadPacket(
                        new ClientboundFreezePlayerPacket(serverSubLevel.getUniqueId(), localPos)
                ));
            }

            return result;
        }

        // Teleporting to a non-sub-level position: clear any stale freeze so the client
        // doesn't get warped back to a previously-frozen sub-level after this teleport
        ((PlayerFreezeExtension) this).sable$freezeTo(null, null);
        this.connection.send(new ClientboundCustomPayloadPacket(ClientboundFreezePlayerPacket.unfreeze()));

        return original.call(serverLevel, x, y, z, set, g, h);
    }
}
