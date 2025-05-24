package com.mochiserver.minecraftShopLands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CreateShopCommand implements CommandExecutor {@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        // 権限チェック
        if (!player.hasPermission("shoplands.create")) {
            player.sendMessage("§c土地を作成する権限がありません。");
            return true;
        }

        if (!RegionSelector.hasCompleteSelection(player)) {
            player.sendMessage("§c範囲が選択されていません。木の斧で範囲を選択してください。");
            return true;
        }

        Location pos1 = RegionSelector.getFirstPosition(player);
        Location pos2 = RegionSelector.getSecondPosition(player);

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage("§c異なるワールドの位置が選択されています。");
            return true;
        }

        // 地域名を生成（プレイヤー名 + タイムスタンプ）
        String regionName = "shop_" + player.getName() + "_" + System.currentTimeMillis();try {
            // デバッグ情報を表示
            player.sendMessage("§7選択範囲: " + formatLocation(pos1) + " - " + formatLocation(pos2));
              // WorldGuardの地域を作成
            createWorldGuardRegion(pos1, pos2, regionName, player.getWorld());

            // 看板アイテムをプレイヤーに配布
            giveShopSignItem(player, regionName);
            player.sendMessage("§a土地 §e" + regionName + " §aを作成しました！");
            player.sendMessage("§6看板アイテムをインベントリに追加しました。");
            player.sendMessage("§7適切な場所に看板を設置してください。");

            // 選択をクリア
            RegionSelector.clearSelection(player);

        } catch (Exception e) {
            player.sendMessage("§c土地の作成に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private void createWorldGuardRegion(Location pos1, Location pos2, String regionName, World world) {
        // 座標を整理
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // WorldGuardの地域を作成
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world));

        if (regions != null) {            // 保護範囲を作成
            BlockVector3 min = BlockVector3.at(minX, minY, minZ);
            BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
            ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, min, max);

            // WorldGuardのデフォルト設定を使用
            // - 地域にオーナー・メンバーが設定されていない場合：誰も建築不可
            // - オーナー・メンバーが設定されている場合：その人のみ建築可能
            // フラグを明示的に設定しないことで、WorldGuardのデフォルト動作を利用
            
            // 優先度を設定（他の地域より優先）
            region.setPriority(10);

            // 地域を登録
            regions.addRegion(region);
        }
    }

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private void giveShopSignItem(Player player, String regionName) {
        ItemStack signItem = ShopSignItem.createShopSign(regionName, player.getUniqueId());
        
        // インベントリに空きがあるかチェック
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(signItem);
        } else {
            // インベントリが満杯の場合は地面に落とす
            player.getWorld().dropItem(player.getLocation(), signItem);
            player.sendMessage("§7インベントリが満杯のため、看板を地面に落としました。");
        }
    }
}
