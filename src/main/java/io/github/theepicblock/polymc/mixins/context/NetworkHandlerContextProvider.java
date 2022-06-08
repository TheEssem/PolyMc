/*
 * PolyMc
 * Copyright (C) 2020-2020 TheEpicBlock_TEB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package io.github.theepicblock.polymc.mixins.context;

import io.github.theepicblock.polymc.impl.mixin.PlayerContextContainer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class NetworkHandlerContextProvider {
    @Shadow public ServerPlayerEntity player;


    /**
     * Provides any packets which implement PlayerContextContainer with the context of the player this network handler is attached to
     * @see PacketPlayerContextContainer
     */
    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V",
            at = @At("HEAD"))
    public void packetSendInject(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener, CallbackInfo ci) {
        if (packet instanceof PlayerContextContainer) {
            ((PlayerContextContainer)packet).setPolyMcProvidedPlayer(this.player);
        }
    }

    /*@Inject(method = "onHandSwing", at = @At("HEAD"))
    public void onHandSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        HitResult blockHit = player.raycast(4, 0, false);
        System.out.println(player.world.getBlockState(((BlockHitResult)blockHit).getBlockPos()));
        //player.interactionManager.tryBreakBlock(((BlockHitResult)blockHit).getBlockPos());
        player.interactionManager.processBlockBreakingAction(((BlockHitResult)blockHit).getBlockPos(), PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, player.getHorizontalFacing(), player.world.getTopY());
    }*/
}
