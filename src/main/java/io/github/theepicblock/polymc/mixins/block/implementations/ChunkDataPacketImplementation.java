/*
 * PolyMc
 * Copyright (C) 2020-2021 TheEpicBlock_TEB
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
package io.github.theepicblock.polymc.mixins.block.implementations;

import io.github.theepicblock.polymc.impl.mixin.ChunkPacketStaticHack;
import io.github.theepicblock.polymc.impl.mixin.PacketSizeProvider;
import io.github.theepicblock.polymc.impl.mixin.PlayerContextContainer;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkDataS2CPacket.class)
public class ChunkDataPacketImplementation implements PlayerContextContainer {
    @Unique private ServerPlayerEntity player;

    @Override
    public ServerPlayerEntity getPolyMcProvidedPlayer() {
        return player;
    }

    @Override
    public void setPolyMcProvidedPlayer(ServerPlayerEntity v) {
        player = v;
    }

    /**
     * Redirects the calculation of the packet size to a custom method. This allows it to take the player into account.
     * @see ChunkSectionSizeProvider
     * @see PacketSizeProvider
     */
    @Redirect(method = "getDataSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;getPacketSize()I"))
    public int redirectGetSize(ChunkSection chunkSection) {
        return ((PacketSizeProvider)chunkSection).getPacketSize(ChunkPacketStaticHack.player);
    }
}
