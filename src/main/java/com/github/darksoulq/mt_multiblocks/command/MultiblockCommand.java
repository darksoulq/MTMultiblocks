package com.github.darksoulq.mt_multiblocks.command;

import com.github.darksoulq.mt_multiblocks.MTMultiblocks;
import com.github.darksoulq.mt_multiblocks.core.AddonDeviceDatabase;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MultiblockCommand {

    public static final NamespacedKey SELECTOR_KEY = new NamespacedKey("mt_multiblocks", "selector");

    public static void register(Commands commands) {
        commands.register(Commands.literal("mtb")
                .requires(source -> source.getSender().hasPermission("mt_multiblocks.admin"))
                .then(Commands.literal("pos1")
                    .executes(ctx -> executePos1(ctx.getSource()))
                )
                .then(Commands.literal("pos2")
                    .executes(ctx -> executePos2(ctx.getSource()))
                )
                .then(Commands.literal("save")
                    .then(Commands.argument("filename", StringArgumentType.word())
                        .executes(ctx -> executeSave(ctx.getSource(), StringArgumentType.getString(ctx, "filename")))
                    )
                ).build(),
            "Multiblock management command"
        );
    }

    private static int executePos1(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) return 0;
        SelectionManager.pos1.put(player.getUniqueId(), player.getLocation().getBlock().getLocation());
        player.sendMessage("Position 1 set.");
        return Command.SINGLE_SUCCESS;
    }

    private static int executePos2(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) return 0;
        SelectionManager.pos2.put(player.getUniqueId(), player.getLocation().getBlock().getLocation());
        player.sendMessage("Position 2 set.");
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSave(CommandSourceStack source, String filename) {
        if (!(source.getSender() instanceof Player player)) return 0;
        UUID uuid = player.getUniqueId();
        Location p1 = SelectionManager.pos1.get(uuid);
        Location p2 = SelectionManager.pos2.get(uuid);

        if (p1 == null || p2 == null || !p1.getWorld().equals(p2.getWorld())) {
            player.sendMessage("You must set pos1 and pos2 in the same world.");
            return 0;
        }

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());

        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        Location coreLoc = null;
        int bedrockCount = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = p1.getWorld().getBlockAt(x, y, z);
                    if (block.getType() == Material.BEDROCK) {
                        coreLoc = block.getLocation();
                        bedrockCount++;
                    }
                }
            }
        }

        if (bedrockCount == 0) {
            player.sendMessage("No bedrock found in selection. Multiblocks require exactly 1 bedrock block as the core placeholder.");
            return 0;
        }

        if (bedrockCount > 1) {
            player.sendMessage("More than 1 bedrock found in selection. Only 1 bedrock can exist to mark the core.");
            return 0;
        }

        YamlConfiguration config = new YamlConfiguration();
        Map<String, String> blockDataToPaletteKey = new HashMap<>();
        int paletteCounter = 1;
        int blockIndex = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = p1.getWorld().getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR || block.getType() == Material.BEDROCK) continue;

                    BlockData data = block.getBlockData();
                    String dataString = data.getAsString();

                    String paletteKey = blockDataToPaletteKey.get(dataString);
                    if (paletteKey == null) {
                        paletteKey = "p" + paletteCounter++;
                        blockDataToPaletteKey.put(dataString, paletteKey);

                        config.set("palette." + paletteKey + ".block_data", dataString);
                        config.set("palette." + paletteKey + ".port.type", "mt_multiblocks:structural_port");

                        config.setComments("palette." + paletteKey, List.of(
                            "To make this optional with component choices, either:",
                            "1) Change 'block_data' to a list of strings (for multiple specific blocks)",
                            "2) Remove 'block_data' entirely and instead use 'tag' and 'default_block_data'"
                        ));
                    }

                    Vector offset = block.getLocation().toVector().subtract(coreLoc.toVector());

                    String path = "blocks.b" + blockIndex++;
                    config.set(path + ".x", offset.getBlockX());
                    config.set(path + ".y", offset.getBlockY());
                    config.set(path + ".z", offset.getBlockZ());
                    config.set(path + ".palette_key", paletteKey);
                    config.set(path + ".required", true);

                    if (blockIndex == 1) {
                        config.setComments(path, List.of(
                            "Set 'required: false' to make this an OptionalComponent instead of RequiredComponent"
                        ));
                    }
                }
            }
        }

        File file = new File(MTMultiblocks.getInstance().getDataFolder() + "/blueprints", filename + ".yml");
        file.getParentFile().mkdirs();
        try {
            config.save(file);
            player.sendMessage("Blueprint saved to " + file.getName());
        } catch (IOException e) {
            player.sendMessage("Failed to save blueprint: " + e.getMessage());
            e.printStackTrace();
        }

        return Command.SINGLE_SUCCESS;
    }
}