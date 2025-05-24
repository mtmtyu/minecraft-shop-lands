package com.mochiserver.minecraftShopLands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignInteractListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) {
            return;
        }

        // ショップの看板かチェック
        if (!ShopSignManager.isShopSign(block.getLocation())) {
            return;
        }        Player player = event.getPlayer();
        
        // 土地購入権限をチェック
        if (!player.hasPermission("shoplands.buy")) {
            player.sendMessage("§c土地を購入する権限がありません。");
            return;
        }
        
        // 既に土地を所有しているかチェック
        if (hasPlayerOwnedLand(player)) {
            player.sendMessage("§c既に土地を所有しています。一人につき一つの土地のみ購入可能です。");
            return;
        }
        
        ShopSignManager.ShopSignData signData = ShopSignManager.getShopSign(block.getLocation());

        if (signData == null) {
            return;
        }

        // 看板の所有者が購入しようとした場合は無視
        if (signData.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c自分の土地は購入できません。");
            return;
        }

        try {
            // プレイヤーを地域に追加
            addPlayerToRegion(signData.getRegionName(), player);

            // 看板を削除
            block.setType(Material.AIR);
            ShopSignManager.removeShopSign(block.getLocation());

            player.sendMessage("§a土地 §e" + signData.getRegionName() + " §aを購入しました！");
            player.sendMessage("§6この土地では建築・破壊が可能になりました。");

        } catch (Exception e) {
            player.sendMessage("§c土地の購入に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }    private void addPlayerToRegion(String regionName, Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regions != null) {
            ProtectedRegion region = regions.getRegion(regionName);
            if (region != null) {
                // デバッグ情報を表示
                player.sendMessage("§7[デバッグ] 地域発見: " + regionName);
                player.sendMessage("§7[デバッグ] 優先度: " + region.getPriority());
                
                // プレイヤーをオーナーに追加（より高い権限）
                DefaultDomain owners = region.getOwners();
                owners.addPlayer(player.getUniqueId());
                region.setOwners(owners);
                
                // メンバーからも追加（予備として）
                DefaultDomain members = region.getMembers();
                members.addPlayer(player.getUniqueId());
                region.setMembers(members);

                // 変更を保存
                try {
                    regions.save();
                    player.sendMessage("§7[デバッグ] 地域権限の保存完了");
                } catch (Exception e) {
                    player.sendMessage("§c[デバッグ] 地域権限の保存に失敗: " + e.getMessage());
                }

                player.sendMessage("§a地域 §e" + regionName + " §aのオーナーになりました！");
                player.sendMessage("§7この土地では自由に建築・破壊ができます。");
                player.sendMessage("§7[デバッグ] オーナーID: " + player.getUniqueId());
            } else {
                player.sendMessage("§c地域が見つかりませんでした: " + regionName);
            }
        } else {
            player.sendMessage("§c地域管理システムにアクセスできませんでした。");
        }
    }/**
     * プレイヤーが既に土地を所有しているかチェック
     * ショップ土地の地域のみをチェック対象とする
     */
    private boolean hasPlayerOwnedLand(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
          if (regions != null) {
            // すべての地域をチェック
            for (ProtectedRegion region : regions.getRegions().values()) {
                // ショップ土地の地域かチェック（優先度10の地域）
                if (region.getPriority() == 10) {
                    
                    // プレイヤーがオーナーまたはメンバーの地域があるかチェック
                    if (region.getOwners().contains(player.getUniqueId()) || 
                        region.getMembers().contains(player.getUniqueId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
