package com.mochiserver.minecraftShopLands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

public class RenameLandCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        // 権限チェック
        if (!player.hasPermission("shoplands.create")) {
            player.sendMessage("§c土地の名前を変更する権限がありません。");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage("§c使用方法: /renameland <現在の土地名> <新しい土地名>");
            return true;
        }

        String oldName = args[0];
        String newName = args[1];

        // 新しい名前のバリデーション
        if (newName.length() > 16) {
            player.sendMessage("§c土地名は16文字以下にしてください。");
            return true;
        }

        if (!newName.matches("^[a-zA-Z0-9_\\-]+$")) {
            player.sendMessage("§c土地名は英数字、アンダースコア、ハイフンのみ使用できます。");
            return true;
        }

        try {
            // WorldGuard地域が存在するかチェック
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

            if (regions == null) {
                player.sendMessage("§c地域管理が利用できません。");
                return true;
            }

            ProtectedRegion region = regions.getRegion(oldName);
            if (region == null) {
                player.sendMessage("§c土地 §e" + oldName + " §cが見つかりません。");
                return true;
            }

            // 新しい名前の地域がすでに存在するかチェック
            if (regions.getRegion(newName) != null) {
                player.sendMessage("§c土地名 §e" + newName + " §cはすでに使用されています。");
                return true;
            }

            // 地域を削除して新しい名前で再作成
            regions.removeRegion(oldName);
            
            // 新しい名前で地域を作成
            ProtectedRegion newRegion = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(
                newName, region.getMinimumPoint(), region.getMaximumPoint());
            
            // フラグと設定をコピー
            newRegion.copyFrom(region);
            
            // 地域を登録
            regions.addRegion(newRegion);

            // 関連する看板を更新
            updateRelatedSigns(oldName, newName, player);

            player.sendMessage("§a土地名を §e" + oldName + " §aから §e" + newName + " §aに変更しました！");

        } catch (Exception e) {
            player.sendMessage("§c土地名の変更に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private void updateRelatedSigns(String oldName, String newName, Player player) {
        int updatedSigns = 0;
        
        // ShopSignManagerの地域名を更新
        ShopSignManager.updateRegionName(oldName, newName);
        
        // すべてのショップ看板をチェックして更新
        for (Location signLocation : ShopSignManager.getAllShopSignLocations()) {
            ShopSignManager.ShopSignData signData = ShopSignManager.getShopSign(signLocation);
            
            if (signData != null && signData.getRegionName().equals(newName)) {
                // 看板のテキストを更新
                Block block = signLocation.getBlock();
                if (block.getType() == Material.OAK_SIGN && block.getState() instanceof Sign) {
                    Sign sign = (Sign) block.getState();
                    
                    sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text("§6[土地販売]"));
                    sign.getSide(org.bukkit.block.sign.Side.FRONT).line(1, Component.text("§e" + newName));
                    sign.getSide(org.bukkit.block.sign.Side.FRONT).line(2, Component.text("§7右クリックで"));
                    sign.getSide(org.bukkit.block.sign.Side.FRONT).line(3, Component.text("§7土地を購入"));
                    sign.update();
                    
                    updatedSigns++;
                    player.sendMessage("§7看板を更新しました: " + formatLocation(signLocation));
                }
            }
        }
        
        if (updatedSigns > 0) {
            player.sendMessage("§a合計 " + updatedSigns + " 個の看板を更新しました。");
        }
    }

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
