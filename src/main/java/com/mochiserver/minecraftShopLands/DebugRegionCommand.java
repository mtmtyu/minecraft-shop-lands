package com.mochiserver.minecraftShopLands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugRegionCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        // 権限チェック
        if (!player.hasPermission("shoplands.admin")) {
            player.sendMessage("§cこのコマンドを使用する権限がありません。");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§c使用方法: /debugregion <地域名>");
            return true;
        }

        String regionName = args[0];

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

            if (regions != null) {
                ProtectedRegion region = regions.getRegion(regionName);
                if (region != null) {
                    player.sendMessage("§e=== 地域情報: " + regionName + " ===");
                    player.sendMessage("§7優先度: " + region.getPriority());
                    player.sendMessage("§7タイプ: " + region.getType());
                    
                    // オーナー情報
                    player.sendMessage("§7オーナー: " + region.getOwners().getPlayers().size() + "人");
                    region.getOwners().getPlayers().forEach(uuid -> {
                        player.sendMessage("  §a- " + uuid.toString());
                    });
                    
                    // メンバー情報
                    player.sendMessage("§7メンバー: " + region.getMembers().getPlayers().size() + "人");
                    region.getMembers().getPlayers().forEach(uuid -> {
                        player.sendMessage("  §b- " + uuid.toString());
                    });
                    
                    // フラグ情報
                    player.sendMessage("§7フラグ:");
                    player.sendMessage("  BUILD: " + region.getFlag(Flags.BUILD));
                    player.sendMessage("  BLOCK_PLACE: " + region.getFlag(Flags.BLOCK_PLACE));
                    player.sendMessage("  BLOCK_BREAK: " + region.getFlag(Flags.BLOCK_BREAK));
                    player.sendMessage("  PASSTHROUGH: " + region.getFlag(Flags.PASSTHROUGH));
                    
                    // 現在のプレイヤーの権限チェック
                    boolean isOwner = region.getOwners().contains(player.getUniqueId());
                    boolean isMember = region.getMembers().contains(player.getUniqueId());
                    player.sendMessage("§7あなたの権限:");
                    player.sendMessage("  オーナー: " + (isOwner ? "§aあり" : "§cなし"));
                    player.sendMessage("  メンバー: " + (isMember ? "§aあり" : "§cなし"));
                    
                } else {
                    player.sendMessage("§c地域 '" + regionName + "' が見つかりませんでした。");
                }
            } else {
                player.sendMessage("§c地域管理システムにアクセスできませんでした。");
            }

        } catch (Exception e) {
            player.sendMessage("§cエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
