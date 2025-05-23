package com.mochiserver.minecraftShopLands;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionSelector {
    private static final Map<UUID, Location> firstPositions = new HashMap<>();
    private static final Map<UUID, Location> secondPositions = new HashMap<>();

    public static void setFirstPosition(Player player, Location location) {
        firstPositions.put(player.getUniqueId(), location);
        player.sendMessage("§a1つ目の位置を選択しました: §7" + formatLocation(location));
    }

    public static void setSecondPosition(Player player, Location location) {
        secondPositions.put(player.getUniqueId(), location);
        player.sendMessage("§a2つ目の位置を選択しました: §7" + formatLocation(location));
        
        if (hasCompleteSelection(player)) {
            player.sendMessage("§6範囲選択完了！ §a/createshop §6コマンドで土地を作成できます。");
        }
    }

    public static boolean hasCompleteSelection(Player player) {
        UUID uuid = player.getUniqueId();
        return firstPositions.containsKey(uuid) && secondPositions.containsKey(uuid);
    }

    public static Location getFirstPosition(Player player) {
        return firstPositions.get(player.getUniqueId());
    }

    public static Location getSecondPosition(Player player) {
        return secondPositions.get(player.getUniqueId());
    }

    public static void clearSelection(Player player) {
        UUID uuid = player.getUniqueId();
        firstPositions.remove(uuid);
        secondPositions.remove(uuid);
    }

    private static String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
