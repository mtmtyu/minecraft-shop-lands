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
                // プレイヤーをメンバーに追加
                DefaultDomain members = region.getMembers();
                members.addPlayer(player.getUniqueId());
                region.setMembers(members);

                // WorldGuardでは、メンバーは自動的に地域内でビルド権限を得る
                // 地域の設定では build, block-place, block-break が DENY に設定されているが
                // メンバーに対してはこれらの制限が適用されない
            }
        }
    }
}
