package dev.ryanhcode.sable.mixin.entity.teleport_players;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFreezePlayerPacket;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Handles Mekanism's cross-dimension teleport path, which calls
 * entity.changeDimension(DimensionTransition). Clears any stale player freeze
 * and, for sub-level destinations, sets a new freeze so the client repositions
 * correctly once the destination sub-level finalizes.
 */
@Mixin(Entity.class)
public abstract class EntityChangeDimensionMixin {

    @WrapMethod(method = "changeDimension")
    private Entity sable$changeDimension(final DimensionTransition transition, final Operation<Entity> original) {
        final ServerLevel targetLevel = transition.newLevel();
        final Vector3d localPos = JOMLConversion.toJOML(transition.pos());
        final SubLevel subLevel = Sable.HELPER.getContaining(targetLevel, localPos);

        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final Entity result = original.call(transition);

            if (result instanceof final ServerPlayer resultPlayer) {
                final Vector3d localAnchor = serverSubLevel.logicalPose().transformPositionInverse(localPos, new Vector3d());
                ((PlayerFreezeExtension) resultPlayer).sable$freezeTo(serverSubLevel.getUniqueId(), localAnchor);
                resultPlayer.connection.send(new ClientboundCustomPayloadPacket(
                        new ClientboundFreezePlayerPacket(serverSubLevel.getUniqueId(), localAnchor)
                ));
            }

            return result;
        }

        // Clear any stale player freeze when teleporting to a non-sub-level position
        final Entity self = (Entity) (Object) this;
        if (self instanceof final ServerPlayer player) {
            ((PlayerFreezeExtension) player).sable$freezeTo(null, null);
            player.connection.send(new ClientboundCustomPayloadPacket(ClientboundFreezePlayerPacket.unfreeze()));
        }

        return original.call(transition);
    }
}
