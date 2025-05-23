package com.mochiserver.minecraftShopLands;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
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
    }

    public static boolean isShopSign(Location location) {
        return shopSigns.containsKey(location);
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
