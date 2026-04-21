package dev.ryanhcode.sable.mixin.entity.teleport_players;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Handles same-dimension non-player entity teleport via entity.teleportTo(x, y, z).
 * Entities in sub-levels live at chunk-grid coordinates, so no coordinate transformation
 * is needed — the entities_stick_sublevels system handles tracking naturally.
 * Players are handled by ServerPlayerMixin / ServerGamePacketListenerImplMixin.
 */
@Mixin(Entity.class)
public abstract class EntityTeleportMixin {

    @WrapMethod(method = "teleportTo(DDD)V")
    private void sable$teleportTo(final double x, final double y, final double z, final Operation<Void> original) {
        original.call(x, y, z);
    }
}
