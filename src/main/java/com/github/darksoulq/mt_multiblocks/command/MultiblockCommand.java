package com.github.darksoulq.mt_multiblocks.command;

import com.github.darksoulq.mt_multiblocks.MTMultiblocks;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
                .then(Commands.literal("selector")
                    .executes(ctx -> executeSelector(ctx.getSource()))
                )
                .then(Commands.literal("save")
                    .then(Commands.argument("filename", StringArgumentType.word())
                        .executes(ctx -> executeSave(ctx.getSource(), StringArgumentType.getString(ctx, "filename")))
                    )
                ).build(),
            "Multiblock management command"
        );
    }

    private static int executeSelector(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) return 0;

        ItemStack selector = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = selector.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.text("Multiblock Selector").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                Component.text("Left-Click: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Pos 1").color(NamedTextColor.WHITE)),
                Component.text("Right-Click: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Pos 2").color(NamedTextColor.WHITE))
            ));

            meta.getPersistentDataContainer().set(SELECTOR_KEY, PersistentDataType.BYTE, (byte) 1);
            selector.setItemMeta(meta);
        }

        player.getInventory().addItem(selector);
        player.sendMessage(Component.text("Given Multiblock Selector.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int executePos1(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) return 0;
        SelectionManager.pos1.put(player.getUniqueId(), player.getLocation().getBlock().getLocation());
        player.sendMessage(Component.text("Position 1 set.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int executePos2(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) return 0;
        SelectionManager.pos2.put(player.getUniqueId(), player.getLocation().getBlock().getLocation());
        player.sendMessage(Component.text("Position 2 set.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSave(CommandSourceStack source, String filename) {
        if (!(source.getSender() instanceof Player player)) return 0;
        UUID uuid = player.getUniqueId();
        Location p1 = SelectionManager.pos1.get(uuid);
        Location p2 = SelectionManager.pos2.get(uuid);

        if (p1 == null || p2 == null || !p1.getWorld().equals(p2.getWorld())) {
            player.sendMessage(Component.text("You must set pos1 and pos2 in the same world.", NamedTextColor.RED));
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
            player.sendMessage(Component.text("No bedrock found in selection. Multiblocks require exactly 1 bedrock block as the core placeholder.", NamedTextColor.RED));
            return 0;
        }

        if (bedrockCount > 1) {
            player.sendMessage(Component.text("More than 1 bedrock found in selection. Only 1 bedrock can exist to mark the core.", NamedTextColor.RED));
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
                    }

                    Vector offset = block.getLocation().toVector().subtract(coreLoc.toVector());

                    String path = "blocks.b" + blockIndex++;
                    config.set(path + ".x", offset.getBlockX());
                    config.set(path + ".y", offset.getBlockY());
                    config.set(path + ".z", offset.getBlockZ());
                    config.set(path + ".palette_key", paletteKey);
                    config.set(path + ".required", true);
                }
            }
        }

        File file = new File(MTMultiblocks.getInstance().getDataFolder() + "/blueprints", filename + ".yml");
        file.getParentFile().mkdirs();
        try {
            config.save(file);
            player.sendMessage(Component.text("Blueprint saved to " + file.getName(), NamedTextColor.GREEN));
        } catch (IOException e) {
            player.sendMessage(Component.text("Failed to save blueprint: " + e.getMessage(), NamedTextColor.RED));
            e.printStackTrace();
        }

        return Command.SINGLE_SUCCESS;
    }
}