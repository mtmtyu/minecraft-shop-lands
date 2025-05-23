package com.mochiserver.minecraftShopLands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ShopSignPlaceListener implements Listener {    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = event.getItemInHand();

        // 看板が設置されたかチェック
        if (block.getType() != Material.OAK_SIGN) {
            return;
        }

        // ショップ看板アイテムかチェック
        if (!ShopSignItem.isShopSignItem(item)) {
            return;
        }

        // 土地作成権限をチェック（手動看板設置は土地作成者のみ可能）
        if (!player.hasPermission("shoplands.create")) {
            player.sendMessage("§c土地販売看板を設置する権限がありません。");
            event.setCancelled(true);
            return;
        }

        // アイテムのメタデータから地域名を取得
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return;
        }

        // 地域名を抽出
        String regionName = null;
        for (Component line : lore) {
            String lineText = line.toString();
            if (lineText.contains("土地名: ")) {
                regionName = lineText.replace("土地名: §e", "").replace("§e", "");
                break;
            }
        }

        if (regionName == null) {
            player.sendMessage("§c看板から地域名を読み取れませんでした。");
            return;
        }

        // 看板にテキストを設定
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            
            try {
                sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text("§6[土地販売]"));
                sign.getSide(org.bukkit.block.sign.Side.FRONT).line(1, Component.text("§e" + regionName));
                sign.getSide(org.bukkit.block.sign.Side.FRONT).line(2, Component.text("§7右クリックで"));
                sign.getSide(org.bukkit.block.sign.Side.FRONT).line(3, Component.text("§7土地を購入"));
                sign.update();

                // 看板データを保存
                ShopSignManager.addShopSign(block.getLocation(), regionName, player.getUniqueId());

                player.sendMessage("§a土地販売看板を設置しました！");
                player.sendMessage("§7地域名: §e" + regionName);
                
            } catch (Exception e) {
                player.sendMessage("§c看板の設定中にエラーが発生しました: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
