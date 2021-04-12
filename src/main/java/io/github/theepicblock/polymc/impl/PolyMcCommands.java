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
package io.github.theepicblock.polymc.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.theepicblock.polymc.PolyMc;
import io.github.theepicblock.polymc.api.DebugInfoProvider;
import io.github.theepicblock.polymc.api.PolyMap;
import io.github.theepicblock.polymc.impl.misc.PolyDumper;
import io.github.theepicblock.polymc.impl.misc.logging.CommandSourceLogger;
import io.github.theepicblock.polymc.impl.misc.logging.ErrorTrackerWrapper;
import io.github.theepicblock.polymc.impl.misc.logging.SimpleLogger;
import io.github.theepicblock.polymc.impl.resource.ResourcePackGenerator;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Registers the polymc commands.
 */
public class PolyMcCommands {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("polymc").requires(source -> source.hasPermissionLevel(2))
                    .then(literal("debug")
                        .then(literal("clientItem")
                            .executes((context) -> {
                                ItemStack heldItem = context.getSource().getPlayer().inventory.getMainHandStack();
                                context.getSource().sendFeedback(PolyMc.getMainMap().getClientItem(heldItem).toTag(new CompoundTag()).toText(), false);
                                return Command.SINGLE_SUCCESS;
                            }))
                        .then(literal("replaceInventoryWithDebug")
                            .executes((context) -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (!player.isCreative()) {
                                    throw new SimpleCommandExceptionType(new LiteralText("You must be in creative mode to execute this command. Keep in mind that this will wipe your inventory.")).create();
                                }
                                for (int i = 0; i < player.inventory.size(); i++){
                                    if (i == 0) {
                                        player.inventory.setStack(i, new ItemStack(Items.GREEN_STAINED_GLASS_PANE));
                                    } else {
                                        player.inventory.setStack(i, new ItemStack(Items.RED_STAINED_GLASS_PANE, i));
                                    }
                                }
                                return Command.SINGLE_SUCCESS;
                            })))
                    .then(literal("generate")
                        .then(literal("resources")
                            .executes((context -> {
                                SimpleLogger commandSource = new CommandSourceLogger(context.getSource(), true);
                                ErrorTrackerWrapper logger = new ErrorTrackerWrapper(PolyMc.LOGGER);
                                try {
                                    ResourcePackGenerator.generate(PolyMc.getMainMap(), "resource", logger);
                                } catch (Exception e) {
                                    commandSource.info("An error occurred whilst trying to generate the resource pack! Please check the console.");
                                    e.printStackTrace();
                                    return 0;
                                }
                                if (logger.errors != 0) {
                                    commandSource.error("There have been errors whilst generating the resource pack. These are usually completely normal. It only means that PolyMc couldn't find some of the textures or models. See the console for more info.");
                                }
                                commandSource.info("Finished generating resource pack");
                                return Command.SINGLE_SUCCESS;
                            })))
                        .then(literal("polyDump")
                            .executes((context) -> {
                                SimpleLogger logger = new CommandSourceLogger(context.getSource(), true);
                                try {
                                    PolyDumper.dumpPolyMap(PolyMc.getMainMap(), "PolyDump.txt", logger);
                                } catch (IOException e) {
                                    logger.error(e.getMessage());
                                    return 0;
                                } catch (Exception e) {
                                    logger.info("An error occurred whilst trying to generate the poly dump! Please check the console.");
                                    e.printStackTrace();
                                    return 0;
                                }
                                logger.info("Finished generating poly dump");
                                return Command.SINGLE_SUCCESS;
                    }))));
        });
    }
}
