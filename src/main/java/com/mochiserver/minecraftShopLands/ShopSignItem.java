package com.mochiserver.minecraftShopLands;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.UUID;

public class ShopSignItem {
    
    public static ItemStack createShopSign(String regionName, UUID ownerUUID) {
        ItemStack signItem = new ItemStack(Material.OAK_SIGN, 1);
        ItemMeta meta = signItem.getItemMeta();
        
        if (meta != null) {
            // 看板の名前を設定
            meta.displayName(Component.text("§6[土地販売看板]"));
            
            // 看板の説明を設定
            meta.lore(Arrays.asList(
                Component.text("§7土地名: §e" + regionName),
                Component.text("§7所有者: §a" + ownerUUID.toString().substring(0, 8) + "..."),
                Component.text("§7"),
                Component.text("§7この看板を設置すると"),
                Component.text("§7土地を購入できるようになります"),
                Component.text("§7"),
                Component.text("§c※ 正しい位置に設置してください")));
            
            signItem.setItemMeta(meta);
            
            // NBTタグで地域情報を保存（カスタムデータ）
            // 注意: 実際のサーバーではPersistentDataContainerを使用することを推奨
        }
        
        return signItem;
    }
    
    public static boolean isShopSignItem(ItemStack item) {
        if (item == null || item.getType() != Material.OAK_SIGN) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        // 表示名で判定
        return meta.displayName().equals(Component.text("§6[土地販売看板]"));
    }
}
