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
package io.github.theepicblock.polymc.api.block;

import io.github.theepicblock.polymc.api.PolyRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Manages which blockstates are allocated to which polys.
 */
public class BlockStateManager {
    private final Object2IntMap<Block> blockStateUsageCounter = new Object2IntOpenHashMap<>();
    private final PolyRegistry polyRegistry;

    public BlockStateManager(PolyRegistry polyRegistry) {
        this.polyRegistry = polyRegistry;
    }

    /**
     * Request a blockstate value to be allocated in a profile.
     * @param stateProfile the profile to use.
     * @return The blockstate you can now use.
     * @throws StateLimitReachedException if the limit of blockstates is reached.
     */
    public BlockState requestBlockState(BlockStateProfile stateProfile) throws StateLimitReachedException {
        for (Block block : stateProfile.blocks) {
            try {
                return requestBlockState(block, stateProfile.filter, stateProfile.onFirstRegister);
            } catch (StateLimitReachedException ignored) {}
        }
        throw new StateLimitReachedException("Tried to access more BlockStates then block has. Profile: '" + stateProfile.name + "'");
    }

    /**
     * Request a certain amount blockstate values to be allocated in a profile.
     * @param stateProfile the profile to use.
     * @param amount       how many blockstates you need.
     * @return The blockstates you can now use.
     * @throws StateLimitReachedException if the limit of BlockStates is reached.
     */
    public List<BlockState> requestBlockStates(BlockStateProfile stateProfile, int amount) throws StateLimitReachedException {
        Block[] blocks = stateProfile.blocks;
        List<BlockState> ret = new ArrayList<>(amount);
        int left = amount;
        for (Block block : blocks) {
            while (left != 0) {
                try {
                    ret.add(requestBlockState(block, stateProfile.filter, stateProfile.onFirstRegister));
                    left--;
                } catch (StateLimitReachedException e) {
                    break;
                }
            }
        }
        if (left != 0) {
            //We didn't reach the needed amount. We need to hack this in to un register the blockstates.
            for (BlockState state : ret) {
                blockStateUsageCounter.put(state.getBlock(), blockStateUsageCounter.getInt(state.getBlock()) - 1);
            }
            throw new StateLimitReachedException("Tried to access more BlockStates then block has. Profile: '" + stateProfile.name + "'");
        }
        return ret;
    }

    /**
     * Checks how many blockstates are available for a profile and compares that with the amount specified.
     * @param stateProfile the profile to use.
     * @param amount       how many blockstates you need.
     * @return True if that amount of blockstates are available.
     */
    public boolean isAvailable(BlockStateProfile stateProfile, int amount) {
        if (amount == 0) return true;
        int goodBlocks = 0;
        for (Block block : stateProfile.blocks) {
            int current = blockStateUsageCounter.getOrDefault(block, 0); //this is the current blockstateId that we're at for this item/

            while (true) {
                current++;
                try {
                    BlockState t = block.getStateManager().getStates().get(current);
                    if (stateProfile.filter.test(t)) {
                        goodBlocks++;
                        if (goodBlocks == amount) return true;
                    }
                } catch (IndexOutOfBoundsException ignored) {
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Request a blockstate value to be allocated for a specific block.
     * @param block           the block you need a BlockState for.
     * @param filter          limits the blockstates that this function can return. A blockstate can only be used if {@link Predicate#test(Object)} returns true.
     *                        A blockstate that was rejected can't be used anymore, even when using a different filter. It is advised to use the same filter per block.
     * @param onFirstRegister this will be called if this block is first used. Useful for registering a poly for it.
     * @return The value you can use.
     * @throws StateLimitReachedException if the limit of BlockStates is reached
     */
    private BlockState requestBlockState(Block block, Predicate<BlockState> filter, BiConsumer<Block,PolyRegistry> onFirstRegister) throws StateLimitReachedException {
        while (true) {
            int current = getBlockStateUsage(block, onFirstRegister);
            try {
                BlockState t = block.getStateManager().getStates().get(current);
                blockStateUsageCounter.put(block, current + 1);
                if (filter.test(t)) {
                    return t;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new StateLimitReachedException("Tried to access more BlockStates then block has: " + block.getTranslationKey());
            }
        }
    }

    /**
     * Gets the usage index of the block.
     * @param block           block to check usage of.
     * @param onFirstRegister method to use if this is the first time this block is used.
     * @return The usage index of the block
     */
    private int getBlockStateUsage(Block block, BiConsumer<Block,PolyRegistry> onFirstRegister) {
        if (!blockStateUsageCounter.containsKey(block)) {
            onFirstRegister.accept(block, polyRegistry);
            blockStateUsageCounter.put(block, 0);
        }
        return blockStateUsageCounter.getInt(block);
    }

    public static class StateLimitReachedException extends Exception {
        public StateLimitReachedException(String s) {
            super(s);
        }
    }
}
