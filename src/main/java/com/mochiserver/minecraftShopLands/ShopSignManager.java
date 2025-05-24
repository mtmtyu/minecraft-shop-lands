package com.mochiserver.minecraftShopLands;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ShopSignManager {
    private static final Map<Location, ShopSignData> shopSigns = new HashMap<>();

    public static void addShopSign(Location location, String regionName, UUID ownerUUID) {
        shopSigns.put(location, new ShopSignData(regionName, ownerUUID));
    }

    public static ShopSignData getShopSign(Location location) {
        return shopSigns.get(location);
    }

    public static void removeShopSign(Location location) {
        shopSigns.remove(location);
    }    public static boolean isShopSign(Location location) {
        return shopSigns.containsKey(location);
    }

    public static Set<Location> getAllShopSignLocations() {
        return shopSigns.keySet();
    }

    public static void updateRegionName(String oldName, String newName) {
        // すべての看板データの地域名を更新
        for (Map.Entry<Location, ShopSignData> entry : shopSigns.entrySet()) {
            ShopSignData data = entry.getValue();
            if (data.getRegionName().equals(oldName)) {
                // 古いデータを削除して新しいデータを追加
                shopSigns.put(entry.getKey(), new ShopSignData(newName, data.getOwnerUUID()));
            }
        }
    }

    public static class ShopSignData {
        private final String regionName;
        private final UUID ownerUUID;

        public ShopSignData(String regionName, UUID ownerUUID) {
            this.regionName = regionName;
            this.ownerUUID = ownerUUID;
        }

        public String getRegionName() {
            return regionName;
        }

        public UUID getOwnerUUID() {
            return ownerUUID;
        }
    }
}
