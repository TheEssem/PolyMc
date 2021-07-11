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
import io.github.theepicblock.polymc.api.resource.ResourcePackMaker;
import io.github.theepicblock.polymc.impl.poly.block.ConditionalSimpleBlockPoly;
import io.github.theepicblock.polymc.impl.poly.block.PropertyRetainingReplacementPoly;
import io.github.theepicblock.polymc.impl.poly.block.SimpleReplacementPoly;
import net.minecraft.block.*;
import net.minecraft.block.enums.SculkSensorPhase;
import net.minecraft.state.property.Properties;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Defines a group of blocks and blockstates.
 * Is used by {@link BlockStateManager} to define which blockstates can be used and which not.
 * Also includes info on how to handle these blockstate {@link #onFirstRegister}.
 */
@SuppressWarnings("PointlessBooleanExpression")
public class BlockStateProfile {
    public final Block[] blocks;
    public final Predicate<BlockState> filter;
    public final BiConsumer<Block,PolyRegistry> onFirstRegister;
    public final String name;

    public BlockStateProfile(String name, Block[] blocks, Predicate<BlockState> filter, BiConsumer<Block,PolyRegistry> onFirstRegister) {
        this.blocks = blocks;
        this.filter = filter;
        this.onFirstRegister = onFirstRegister;
        this.name = name;
    }
    public BlockStateProfile(String name, Block block, Predicate<BlockState> filter, BiConsumer<Block,PolyRegistry> onFirstRegister) {
        this.blocks = new Block[]{block};
        this.filter = filter;
        this.onFirstRegister = onFirstRegister;
        this.name = name;
    }


    //BLOCK LISTS
    private static final Block[] LEAVES_BLOCKS = {Blocks.ACACIA_LEAVES,Blocks.BIRCH_LEAVES,Blocks.DARK_OAK_LEAVES,Blocks.JUNGLE_LEAVES,Blocks.OAK_LEAVES,Blocks.SPRUCE_LEAVES};
    private static final Block[] NO_COLLISION_BLOCKS = {Blocks.SUGAR_CANE,
            Blocks.ACACIA_SAPLING, Blocks.BIRCH_SAPLING, Blocks.DARK_OAK_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING,
            Blocks.TRIPWIRE, Blocks.POTATOES, Blocks.CARROTS, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE};
    private static final Block[] DOOR_BLOCKS = {Blocks.ACACIA_DOOR,Blocks.BIRCH_DOOR,Blocks.DARK_OAK_DOOR,Blocks.JUNGLE_DOOR,Blocks.OAK_DOOR,Blocks.SPRUCE_DOOR,Blocks.CRIMSON_DOOR,Blocks.WARPED_DOOR};
    private static final Block[] TRAPDOOR_BLOCKS = {Blocks.ACACIA_TRAPDOOR,Blocks.BIRCH_TRAPDOOR,Blocks.DARK_OAK_TRAPDOOR,Blocks.JUNGLE_TRAPDOOR,Blocks.OAK_TRAPDOOR,Blocks.SPRUCE_TRAPDOOR,Blocks.CRIMSON_TRAPDOOR,Blocks.WARPED_TRAPDOOR};

    //FILTERS
    private static final Predicate<BlockState> DEFAULT_FILTER = (blockState) -> blockState != blockState.getBlock().getDefaultState();
    private static final Predicate<BlockState> NO_COLLISION_FILTER = (blockState) -> {
        Block block = blockState.getBlock();
        if (block == Blocks.TRIPWIRE) {
            return isStringUseable(blockState);
        } else if (block == Blocks.POTATOES || block == Blocks.CARROTS) {
            int age = blockState.get(CropBlock.AGE);
            return age != 0 && age != 2 && age != 4 && age != 7;
        } else if (block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE || block == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            int power = blockState.get(Properties.POWER);
            return power != 0 && power != 1;
        } else {
            return DEFAULT_FILTER.test(blockState);
        }
    };
    private static final Predicate<BlockState> ALWAYS_TRUE_FILTER = (blockState) -> true;
    private static final Predicate<BlockState> FARMLAND_FILTER = (blockState) -> {
        int moisture = blockState.get(FarmlandBlock.MOISTURE);
        return moisture != 0 && moisture != 7;
    };
    private static final Predicate<BlockState> POWERED_FILTER = (blockState) -> blockState.get(Properties.POWERED) == true;
    private static final Predicate<BlockState> SCULK_FILTER = (blockState) -> {
        int power = blockState.get(SculkSensorBlock.POWER);
        SculkSensorPhase phase = blockState.get(SculkSensorBlock.SCULK_SENSOR_PHASE);
        return power != 0 || phase == SculkSensorPhase.COOLDOWN;
    };

    //ON FIRST REGISTERS
    private static final BiConsumer<Block,PolyRegistry> DEFAULT_ON_FIRST_REGISTER = (block, polyRegistry) -> polyRegistry.registerBlockPoly(block, new SimpleReplacementPoly(block.getDefaultState()));
    private static final BiConsumer<Block,PolyRegistry> NO_COLLISION_ON_FIRST_REGISTER = (block, polyRegistry) -> {
        if (block == Blocks.TRIPWIRE) {
            polyRegistry.registerBlockPoly(block, new BlockPoly() {
                @Override
                public BlockState getClientBlock(BlockState input) {
                    return input.with(Properties.POWERED, false).with(Properties.DISARMED,false);
                }
                @Override public void addToResourcePack(Block block, ResourcePackMaker pack) {}
            });
        } else if (block == Blocks.POTATOES || block == Blocks.CARROTS) {
            polyRegistry.registerBlockPoly(block, new BlockPoly() {
                @Override
                public BlockState getClientBlock(BlockState input) {
                    int age = input.get(CropBlock.AGE);
                    if (age == 7) {
                        return input.with(CropBlock.AGE, 7);
                    } else if (age >= 4) {
                        return input.with(CropBlock.AGE, 4);
                    } else if (age >= 2) {
                        return input.with(CropBlock.AGE, 2);
                    } else if (age >= 0) {
                        return input.with(CropBlock.AGE, 0);
                    } else {
                        return input.with(CropBlock.AGE, 0);
                    }
                }
                @Override public void addToResourcePack(Block block, ResourcePackMaker pack) {}
            });
        } else if (block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE || block == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            polyRegistry.registerBlockPoly(block, new BlockPoly() {
                @Override
                public BlockState getClientBlock(BlockState input) {
                    if (input.get(Properties.POWER) == 0) {
                        return input.with(Properties.POWER, 0);
                    } else {
                        return input.with(Properties.POWER, 1);
                    }
                }
                @Override public void addToResourcePack(Block block, ResourcePackMaker pack) {}
            });
        } else {
            polyRegistry.registerBlockPoly(block, new SimpleReplacementPoly(block.getDefaultState()));
        }
    };
    private static final BiConsumer<Block,PolyRegistry> SCULK_ON_FIRST_REGISTER = (block, polyRegistry) -> {
        polyRegistry.registerBlockPoly(block, new BlockPoly() {
            @Override
            public BlockState getClientBlock(BlockState input) {
                SculkSensorPhase phase = input.get(SculkSensorBlock.SCULK_SENSOR_PHASE);
                if (phase == SculkSensorPhase.INACTIVE || phase == SculkSensorPhase.COOLDOWN) {
                    return input.with(SculkSensorBlock.SCULK_SENSOR_PHASE, SculkSensorPhase.INACTIVE).with(SculkSensorBlock.POWER, 0);
                } else {
                    return input.with(SculkSensorBlock.SCULK_SENSOR_PHASE, SculkSensorPhase.ACTIVE).with(SculkSensorBlock.POWER, 0);
                }
            }
            @Override public void addToResourcePack(Block block, ResourcePackMaker pack) {}
        });
    };
    private static final BiConsumer<Block,PolyRegistry> PETRIFIED_OAK_SLAB_ON_FIRST_REGISTER = (block, polyRegistry) -> polyRegistry.registerBlockPoly(block, new PropertyRetainingReplacementPoly(Blocks.OAK_SLAB));
    private static final BiConsumer<Block,PolyRegistry> FARMLAND_ON_FIRST_REGISTER = (block, polyRegistry) -> polyRegistry.registerBlockPoly(block, new ConditionalSimpleBlockPoly(Blocks.FARMLAND.getDefaultState(), FARMLAND_FILTER));
    private static final BiConsumer<Block,PolyRegistry> POWERED_BLOCK_ON_FIRST_REGISTER = (block, polyRegistry) -> polyRegistry.registerBlockPoly(block, (BlockPolyPredicate)(block2) -> block2.with(Properties.POWERED, false));

    //PROFILES
    public static final BlockStateProfile NOTE_BLOCK_PROFILE = getProfileWithDefaultFilter("note block", Blocks.NOTE_BLOCK);
    public static final BlockStateProfile LEAVES_PROFILE = getProfileWithDefaultFilter("leaves", LEAVES_BLOCKS);
    public static final BlockStateProfile NO_COLLISION_PROFILE = new BlockStateProfile("blocks without collisions", NO_COLLISION_BLOCKS, NO_COLLISION_FILTER, NO_COLLISION_ON_FIRST_REGISTER);
    public static final BlockStateProfile PETRIFIED_OAK_SLAB_PROFILE = new BlockStateProfile("petrified oak slab", Blocks.PETRIFIED_OAK_SLAB, ALWAYS_TRUE_FILTER, PETRIFIED_OAK_SLAB_ON_FIRST_REGISTER);
    public static final BlockStateProfile LOWER_SLAB_PROFILE = new BlockStateProfile("lower slab", Blocks.SCULK_SENSOR, SCULK_FILTER, SCULK_ON_FIRST_REGISTER);
    public static final BlockStateProfile FARMLAND_PROFILE = new BlockStateProfile("farmland", Blocks.FARMLAND, FARMLAND_FILTER, FARMLAND_ON_FIRST_REGISTER);
    public static final BlockStateProfile CACTUS_PROFILE = getProfileWithDefaultFilter("cactus", Blocks.CACTUS);
    public static final BlockStateProfile KELP_PROFILE = getProfileWithDefaultFilter("kelp", Blocks.KELP);
    public static final BlockStateProfile DOOR_PROFILE = new BlockStateProfile("door", DOOR_BLOCKS, POWERED_FILTER, POWERED_BLOCK_ON_FIRST_REGISTER);
    public static final BlockStateProfile TRAPDOOR_PROFILE = new BlockStateProfile("trapdoor", TRAPDOOR_BLOCKS, POWERED_FILTER, POWERED_BLOCK_ON_FIRST_REGISTER);
    public static final BlockStateProfile METAL_DOOR_PROFILE = new BlockStateProfile("metal_door", Blocks.IRON_DOOR, POWERED_FILTER, POWERED_BLOCK_ON_FIRST_REGISTER);
    public static final BlockStateProfile METAL_TRAPDOOR_PROFILE = new BlockStateProfile("metal_trapdoor", Blocks.IRON_TRAPDOOR, POWERED_FILTER, POWERED_BLOCK_ON_FIRST_REGISTER);

    //OTHER CODE
    public static BlockStateProfile getProfileWithDefaultFilter(String name, Block[] blocks) {
        return new BlockStateProfile(name, blocks, DEFAULT_FILTER, DEFAULT_ON_FIRST_REGISTER);
    }

    public static BlockStateProfile getProfileWithDefaultFilter(String name, Block block) {
        return new BlockStateProfile(name, block, DEFAULT_FILTER, DEFAULT_ON_FIRST_REGISTER);
    }

    private static boolean isStringUseable(BlockState state) {
        return  state.get(Properties.POWERED) == true ||
                state.get(TripwireBlock.DISARMED) == true;
    }
}
