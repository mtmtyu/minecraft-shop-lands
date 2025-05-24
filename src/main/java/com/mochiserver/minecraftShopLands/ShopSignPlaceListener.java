package com.mochiserver.minecraftShopLands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class ShopSignPlaceListener implements Listener {
    
    // 自動設定される看板の位置を追跡
    private static final Set<Location> autoSettingSigns = new HashSet<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        Location signLocation = event.getBlock().getLocation();
        
        // 自動設定される看板の場合、編集を防ぐ
        if (autoSettingSigns.contains(signLocation)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
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
        }        if (regionName == null) {
            player.sendMessage("§c看板から地域名を読み取れませんでした。");
            return;
        }

        final String finalRegionName = regionName;
        final Location signLocation = block.getLocation();
        
        // この看板を自動設定リストに追加
        autoSettingSigns.add(signLocation);
        
        // 看板にテキストを設定（遅延実行で編集画面を回避）
        if (block.getState() instanceof Sign) {
            // 5ティック後に看板テキストを設定（編集画面が完全に閉じた後）
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (block.getState() instanceof Sign) {
                            Sign delayedSign = (Sign) block.getState();
                            delayedSign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text("§6[土地販売]"));
                            delayedSign.getSide(org.bukkit.block.sign.Side.FRONT).line(1, Component.text("§e" + finalRegionName));
                            delayedSign.getSide(org.bukkit.block.sign.Side.FRONT).line(2, Component.text("§7右クリックで"));
                            delayedSign.getSide(org.bukkit.block.sign.Side.FRONT).line(3, Component.text("§7土地を購入"));
                            delayedSign.update();

                            // 看板データを保存
                            ShopSignManager.addShopSign(signLocation, finalRegionName, player.getUniqueId());

                            player.sendMessage("§a土地販売看板を設置しました！");
                            player.sendMessage("§7地域名: §e" + finalRegionName);
                            
                            // 10ティック後に自動設定リストから削除（編集画面が完全に処理された後）
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    autoSettingSigns.remove(signLocation);
                                }
                            }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("minecraft-shop-lands"), 10L);
                        }
                    } catch (Exception e) {
                        player.sendMessage("§c看板の設定中にエラーが発生しました: " + e.getMessage());
                        e.printStackTrace();
                        // エラーが発生した場合もリストから削除
                        autoSettingSigns.remove(signLocation);
                    }
                }
            }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("minecraft-shop-lands"), 5L);
        }
    }
}
