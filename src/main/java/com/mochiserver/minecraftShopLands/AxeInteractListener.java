package com.mochiserver.minecraftShopLands;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class AxeInteractListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // 木の斧を持っているかチェック
        if (player.getInventory().getItemInMainHand().getType() != Material.WOODEN_AXE) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // 左クリック（攻撃）で1つ目の位置を設定
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            RegionSelector.setFirstPosition(player, block.getLocation());
        }
        // 右クリックで2つ目の位置を設定
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            RegionSelector.setSecondPosition(player, block.getLocation());
        }
    }
}
