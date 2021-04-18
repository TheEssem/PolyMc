package io.github.theepicblock.polymc.impl.poly.block;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

import io.github.theepicblock.polymc.api.PolyRegistry;
import io.github.theepicblock.polymc.api.block.BlockPoly;
import io.github.theepicblock.polymc.api.block.BlockStateManager;
import io.github.theepicblock.polymc.api.block.BlockStateProfile;
import io.github.theepicblock.polymc.api.resource.JsonBlockState;
import io.github.theepicblock.polymc.api.resource.ResourcePackMaker;
import io.github.theepicblock.polymc.impl.Util;
import it.unimi.dsi.fastutil.Function;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * Chooses whether to use a {@link io.github.theepicblock.polymc.api.block.BlockStateProfile} for each state or a default {@link io.github.theepicblock.polymc.impl.poly.block.SimpleReplacementPoly} based on a boolean
 * @see UnusedBlockStatePoly
 */
public class ReplacementFallbackUnusedBlockStatePoly implements BlockPoly {
    private final ImmutableMap<BlockState,BlockState> states;

    /**
     * @param moddedBlock     the block this poly represents
     * @param falseProfile    the profile to use.
     * @param registry        registry used to register this poly
     * @throws BlockStateManager.StateLimitReachedException when the clientSideBlock doesn't have any more BlockStates left.
     */
    public ReplacementFallbackUnusedBlockStatePoly(Block moddedBlock, PolyRegistry registry, BlockStateProfile profile, Function<BlockState, Boolean> function) throws BlockStateManager.StateLimitReachedException {
        BlockStateManager manager = registry.getBlockStateManager();

        // Sort states into true <-> false
        ImmutableList<BlockState> moddedStates = moddedBlock.getStateManager().getStates();
        List<BlockState> falseStates = new ArrayList<>();
        List<BlockState> trueStates = new ArrayList<>();
        for (BlockState moddedState : moddedStates) {
            if (function.apply(moddedState)) {
                trueStates.add(moddedState);
            } else {
                falseStates.add(moddedState);
            }
        }

        // Check if both profiles have enough states
        if (!manager.isAvailable(profile, trueStates.size())) {
            throw new BlockStateManager.StateLimitReachedException("Block doesn't have enough blockstates left for false profile: '"+ profile.name +"'");
        }

        // Register the blocks to the correct profiles
        HashMap<BlockState,BlockState> res = new HashMap<>();
        for (BlockState state : falseStates) {
            res.put(state, Blocks.STONE.getDefaultState());
        }
        for (BlockState state : trueStates) {
            res.put(state, manager.requestBlockState(profile));
        }
        states = ImmutableMap.copyOf(res);
    }

    @Override
    public BlockState getClientBlock(BlockState input) {
        return states.get(input);
    }

    @Override
    public void addToResourcePack(Block block, ResourcePackMaker pack) {
        Identifier moddedBlockId = Registry.BLOCK.getId(block);
        InputStreamReader blockStateReader = pack.getAsset(moddedBlockId.getNamespace(), ResourcePackMaker.BLOCKSTATES + moddedBlockId.getPath() + ".json");
        JsonBlockState moddedBlockStates = pack.getGson().fromJson(new JsonReader(blockStateReader), JsonBlockState.class);

        states.forEach((moddedState, clientState) -> {
            if (clientState != Blocks.STONE.getDefaultState()) {
                Identifier clientBlockId = Registry.BLOCK.getId(clientState.getBlock());
                JsonBlockState clientBlockStates = pack.getOrDefaultPendingBlockState(clientBlockId);
                String clientStateString = Util.getPropertiesFromBlockState(clientState);

                JsonElement moddedVariants = moddedBlockStates.getVariantBestMatching(moddedState);
                clientBlockStates.variants.put(clientStateString, moddedVariants);

                for (JsonBlockState.Variant v : JsonBlockState.getVariantsFromJsonElement(moddedVariants)) {
                    Identifier vId = Identifier.tryParse(v.model);
                    if (vId != null) pack.copyModel(new Identifier(v.model));
                }
            }
        });
    }

    @Override
    public String getDebugInfo(Block obj) {
        StringBuilder out = new StringBuilder();
        out.append(states.size()).append(" states");
        states.forEach((moddedState, clientState) -> {
            out.append("\n");
            out.append("    #");
            out.append(moddedState);
            out.append(" -> ");
            out.append(clientState);
        });
        return out.toString();
    }
    
}
