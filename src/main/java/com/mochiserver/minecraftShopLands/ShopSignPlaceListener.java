package com.mochiserver.minecraftShopLands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
        
        // 既存のショップ看板の編集を防ぐ
        if (ShopSignManager.isShopSign(signLocation)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cショップ看板は編集できません。");
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        
        // ショップ看板の破壊を管理者権限でのみ許可
        if (ShopSignManager.isShopSign(location)) {
            Player player = event.getPlayer();
            if (!player.hasPermission("shoplands.admin")) {
                event.setCancelled(true);
                player.sendMessage("§cショップ看板を破壊する権限がありません。");
                return;
            }
            
            // 管理者による破壊の場合、データから削除
            ShopSignManager.removeShopSign(location);
            player.sendMessage("§eショップ看板を削除しました。");
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
        }        final String finalRegionName = regionName;
        
        // 看板の置き換えシステム：古い看板を破壊し、新しい看板を設置
        replaceSignWithProperSign(block, finalRegionName, player);
    }

    /**
     * 看板を破壊し、正しい情報を持つ新しい看板に置き換える
     */
    private void replaceSignWithProperSign(Block originalBlock, String regionName, Player player) {
        final Location signLocation = originalBlock.getLocation();
        
        // 方向と種類を保存
        org.bukkit.block.data.BlockData originalBlockData = originalBlock.getBlockData();
        
        // この看板を自動設定リストに追加（編集画面の干渉を防ぐ）
        autoSettingSigns.add(signLocation);
        
        // 遅延実行で看板を破壊・再設置
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // 元の看板を破壊
                    originalBlock.setType(Material.AIR);
                    
                    // 1ティック後に新しい看板を設置
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                // 新しい看板を設置
                                originalBlock.setBlockData(originalBlockData);
                                
                                if (originalBlock.getState() instanceof Sign) {
                                    Sign newSign = (Sign) originalBlock.getState();
                                    
                                    // 看板テキストを設定
                                    newSign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text("§6[土地販売]"));
                                    newSign.getSide(org.bukkit.block.sign.Side.FRONT).line(1, Component.text("§e" + regionName));
                                    newSign.getSide(org.bukkit.block.sign.Side.FRONT).line(2, Component.text("§7右クリックで"));
                                    newSign.getSide(org.bukkit.block.sign.Side.FRONT).line(3, Component.text("§7土地を購入"));
                                    newSign.update(true);
                                    
                                    // 看板データを保存
                                    ShopSignManager.addShopSign(signLocation, regionName, player.getUniqueId());
                                    
                                    player.sendMessage("§a土地販売看板を設置しました！");
                                    player.sendMessage("§7地域名: §e" + regionName);
                                    
                                    // 自動設定リストから削除
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            autoSettingSigns.remove(signLocation);
                                        }
                                    }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("minecraft-shop-lands"), 10L);
                                } else {
                                    player.sendMessage("§c看板の設置に失敗しました。");
                                    autoSettingSigns.remove(signLocation);
                                }
                            } catch (Exception e) {
                                player.sendMessage("§c看板の設置中にエラーが発生しました: " + e.getMessage());
                                e.printStackTrace();
                                autoSettingSigns.remove(signLocation);
                            }
                        }
                    }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("minecraft-shop-lands"), 1L);
                } catch (Exception e) {
                    player.sendMessage("§c看板の破壊中にエラーが発生しました: " + e.getMessage());
                    e.printStackTrace();
                    autoSettingSigns.remove(signLocation);
                }
            }
        }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("minecraft-shop-lands"), 3L);
    }
}
