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
package io.github.theepicblock.polymc.impl.misc;

import io.github.theepicblock.polymc.api.block.BlockPoly;
import io.github.theepicblock.polymc.impl.Util;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla clients do client-side prediction when placing and removing blocks.
 * These predictions are wrong.
 * These methods are called by {@link io.github.theepicblock.polymc.mixins.block.ResyncImplementation} to resync the blocks with the server's state.
 */
public class BlockResyncManager {
    public static boolean shouldForceSync(BlockState sourceState, BlockState clientState, Direction direction) {
        Block block = clientState.getBlock();
        if (block == Blocks.NOTE_BLOCK) {
            return direction == Direction.UP;
        } else if (block == Blocks.MYCELIUM || block == Blocks.PODZOL) {
            return direction == Direction.DOWN;
        } else if (block == Blocks.TRIPWIRE) {
            if (sourceState == null) return direction.getAxis().isHorizontal();

            //Checks if the connected property for the block isn't what it should be
            //If the source block in that direction is string, it should be true. Otherwise false
            return direction.getAxis().isHorizontal() &&
                    clientState.get(ConnectingBlock.FACING_PROPERTIES.get(direction.getOpposite())) != (sourceState.getBlock() instanceof TripwireBlock);
        }
        return false;
    }

    public static void onBlockUpdate(BlockState sourceState, BlockPos sourcePos, World world, ServerPlayerEntity player, List<BlockPos> checkedBlocks) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (Direction direction : Direction.values()) {
            pos.set(sourcePos.getX() + direction.getOffsetX(), sourcePos.getY() + direction.getOffsetY(), sourcePos.getZ() + direction.getOffsetZ());
            if (checkedBlocks != null && checkedBlocks.contains(pos)) continue;

            BlockState state = world.getBlockState(pos);
            BlockPoly poly = Util.tryGetPolyMap(player).getBlockPoly(state.getBlock());

            if (poly != null) {
                BlockState clientState = poly.getClientBlock(state);

                if (BlockResyncManager.shouldForceSync(sourceState, clientState, direction)) {
                    BlockPos newPos = pos.toImmutable();
                    player.networkHandler.sendPacket(new BlockUpdateS2CPacket(newPos, state));

                    if (checkedBlocks == null) checkedBlocks = new ArrayList<>();
                    checkedBlocks.add(sourcePos);

                    onBlockUpdate(clientState, newPos, world, player, checkedBlocks);
                }
            }

            // If the lower half of a door is interacted with, we should check the upper half as well
            boolean isUpperDoor = direction == Direction.UP && state.getBlock() instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
            if (isUpperDoor) {
                if (checkedBlocks == null) checkedBlocks = new ArrayList<>();
                checkedBlocks.add(sourcePos);
                onBlockUpdate(null, pos, world, player, checkedBlocks);
            }
        }
    }
}
