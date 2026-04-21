package dev.ryanhcode.sable.mixin.entity.teleport_players;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Handles Mekanism's same-dimension non-player entity teleport path, which calls
 * entity.teleportTo(x, y, z) directly. Transforms sub-level local coordinates to
 * global world coordinates so entities appear at the correct visual position.
 */
@Mixin(Entity.class)
public abstract class EntityTeleportMixin {

    @WrapMethod(method = "teleportTo(DDD)V")
    private void sable$teleportTo(final double x, final double y, final double z, final Operation<Void> original) {
        final Entity self = (Entity) (Object) this;

        // Players are handled separately via connection.teleport() (see ServerGamePacketListenerImplMixin)
        // or via the ServerPlayer.teleportTo() mixin. Skip to avoid double transformation.
        if (self instanceof ServerPlayer || !(self.level() instanceof final ServerLevel serverLevel)) {
            original.call(x, y, z);
            return;
        }

        final Vector3d localPos = new Vector3d(x, y, z);
        final SubLevel subLevel = Sable.HELPER.getContaining(serverLevel, localPos);

        if (subLevel != null) {
            final Vector3d globalPos = Sable.HELPER.projectOutOfSubLevel(serverLevel, localPos, new Vector3d());
            original.call(globalPos.x, globalPos.y, globalPos.z);
        } else {
            original.call(x, y, z);
        }
    }
}
