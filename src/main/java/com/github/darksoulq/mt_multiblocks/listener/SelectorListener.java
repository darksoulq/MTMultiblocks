package com.github.darksoulq.mt_multiblocks.listener;

import com.github.darksoulq.mt_multiblocks.command.MultiblockCommand;
import com.github.darksoulq.mt_multiblocks.command.SelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SelectorListener implements Listener {
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(MultiblockCommand.SELECTOR_KEY, PersistentDataType.BYTE)) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (event.getClickedBlock() != null) {
                SelectionManager.pos1.put(p.getUniqueId(), event.getClickedBlock().getLocation());
                p.sendMessage(Component.text("Position 1 set.", NamedTextColor.GREEN));
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (event.getClickedBlock() != null) {
                SelectionManager.pos2.put(p.getUniqueId(), event.getClickedBlock().getLocation());
                p.sendMessage(Component.text("Position 2 set.", NamedTextColor.GREEN));
            }
        }
    }
}