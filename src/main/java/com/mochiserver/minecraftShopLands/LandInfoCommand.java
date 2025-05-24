package com.mochiserver.minecraftShopLands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LandInfoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("landinfo")) {
            showPlayerLandInfo(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("resetland")) {
            if (!player.hasPermission("shoplands.admin")) {
                player.sendMessage("§c管理者権限が必要です。");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage("§c使用方法: /resetland <プレイヤー名>");
                return true;
            }

            resetPlayerLand(player, args[0]);
            return true;
        }

        return false;
    }

    private void showPlayerLandInfo(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regions == null) {
            player.sendMessage("§c地域管理システムにアクセスできませんでした。");
            return;
        }

        boolean hasLand = false;
        player.sendMessage("§6=== あなたの所有土地 ===");

        for (ProtectedRegion region : regions.getRegions().values()) {
            // ショップ土地の地域かチェック（優先度10でbuild=DENYの地域）
            if (region.getPriority() == 10 && 
                region.getFlag(Flags.BUILD) == StateFlag.State.DENY) {
                
                // プレイヤーがオーナーまたはメンバーの地域があるかチェック
                if (region.getOwners().contains(player.getUniqueId()) || 
                    region.getMembers().contains(player.getUniqueId())) {
                    
                    hasLand = true;
                    String role = region.getOwners().contains(player.getUniqueId()) ? "オーナー" : "メンバー";
                    player.sendMessage("§e" + region.getId() + " §7(" + role + ")");
                }
            }
        }

        if (!hasLand) {
            player.sendMessage("§7所有している土地はありません。");
        }
    }

    private void resetPlayerLand(Player admin, String targetPlayerName) {
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
        
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            admin.sendMessage("§cプレイヤー " + targetPlayerName + " が見つかりません。");
            return;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(admin.getWorld()));

        if (regions == null) {
            admin.sendMessage("§c地域管理システムにアクセスできませんでした。");
            return;
        }

        UUID targetUUID = targetPlayer.getUniqueId();
        int resetCount = 0;

        for (ProtectedRegion region : regions.getRegions().values()) {
            // ショップ土地の地域かチェック（優先度10でbuild=DENYの地域）
            if (region.getPriority() == 10 && 
                region.getFlag(Flags.BUILD) == StateFlag.State.DENY) {
                
                // プレイヤーがオーナーまたはメンバーの場合、削除
                if (region.getOwners().contains(targetUUID) || 
                    region.getMembers().contains(targetUUID)) {
                    
                    region.getOwners().removePlayer(targetUUID);
                    region.getMembers().removePlayer(targetUUID);
                    resetCount++;
                    
                    admin.sendMessage("§e地域 " + region.getId() + " から " + targetPlayerName + " を削除しました。");
                }
            }
        }

        if (resetCount > 0) {
            admin.sendMessage("§a" + targetPlayerName + " の土地所有権を " + resetCount + " 件リセットしました。");
            if (targetPlayer.isOnline()) {
                ((Player) targetPlayer).sendMessage("§c管理者により土地所有権がリセットされました。");
            }
        } else {
            admin.sendMessage("§7" + targetPlayerName + " は土地を所有していません。");
        }
    }
}
