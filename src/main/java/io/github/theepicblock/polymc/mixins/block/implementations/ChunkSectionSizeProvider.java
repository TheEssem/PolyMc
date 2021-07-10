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
package io.github.theepicblock.polymc.mixins.block.implementations;

import io.github.theepicblock.polymc.impl.mixin.PacketSizeProvider;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Provides the correct packet size for this {@link ChunkSection} via the {@link PacketSizeProvider} interface.
 *  * This allows the packet size to take into account the player and their PolyMap when calculating
 */
@Mixin(ChunkSection.class)
public class ChunkSectionSizeProvider implements PacketSizeProvider {
    @Shadow @Final private PalettedContainer<BlockState> container;

    @Override
    public int getPacketSize(ServerPlayerEntity playerEntity) {
        return 2 + ((PacketSizeProvider)this.container).getPacketSize(playerEntity);
    }
}
