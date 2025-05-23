package com.mochiserver.minecraftShopLands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;

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
            createWorldGuardRegion(pos1, pos2, regionName, player.getWorld());            // 看板を設置
            Location signLocation = findFloorCenter(pos1, pos2);
            boolean signPlaced = false;
            
            if (signLocation != null) {
                signPlaced = placeSign(signLocation, regionName, player);
                if (signPlaced) {
                    player.sendMessage("§a土地 §e" + regionName + " §aを作成し、看板を設置しました！");
                    player.sendMessage("§7看板位置: " + formatLocation(signLocation));
                }
            }
            
            if (!signPlaced) {
                // 床が見つからない、または設置に失敗した場合は、フォールバック位置を試す
                Location fallbackLocation = getFallbackSignLocation(pos1, pos2);
                if (fallbackLocation != null) {
                    signPlaced = placeSign(fallbackLocation, regionName, player);
                    if (signPlaced) {
                        player.sendMessage("§a土地 §e" + regionName + " §aを作成し、看板を設置しました！");
                        player.sendMessage("§7看板位置: " + formatLocation(fallbackLocation));
                    }
                }
            }
            
            if (!signPlaced) {
                // 看板の自動設置に失敗した場合、プレイヤーに看板アイテムを配布
                giveShopSignItem(player, regionName);
                player.sendMessage("§e土地 §e" + regionName + " §eを作成しました。");
                player.sendMessage("§6看板の自動設置に失敗したため、看板アイテムをインベントリに追加しました。");
                player.sendMessage("§7適切な場所に看板を設置してください。");
            }

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

        if (regions != null) {
            // 保護範囲を作成
            BlockVector3 min = BlockVector3.at(minX, minY, minZ);
            BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
            ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, min, max);

            // フラグを設定（build, block-place, block-break を DENY、優先度 10）
            region.setFlag(Flags.BUILD, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
            region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
            region.setPriority(10);

            // 地域を登録
            regions.addRegion(region);
        }
    }    private Location findFloorCenter(Location pos1, Location pos2) {
        int centerX = (pos1.getBlockX() + pos2.getBlockX()) / 2;
        int centerZ = (pos1.getBlockZ() + pos2.getBlockZ()) / 2;
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());

        World world = pos1.getWorld();

        // 下から上に向かって床を探す
        for (int y = minY; y <= maxY; y++) {
            Block block = world.getBlockAt(centerX, y, centerZ);
            Block above = world.getBlockAt(centerX, y + 1, centerZ);

            // 固体ブロックがあり、その上が空気の場合
            if (block.getType().isSolid() && above.getType() == Material.AIR) {
                return new Location(world, centerX, y + 1, centerZ);
            }
        }
        
        // 固体ブロックが見つからない場合、空気ブロックを探す
        for (int y = minY; y <= maxY; y++) {
            Block block = world.getBlockAt(centerX, y, centerZ);
            if (block.getType() == Material.AIR) {
                return new Location(world, centerX, y, centerZ);
            }
        }

        return null;
    }    private boolean placeSign(Location location, String regionName, Player owner) {
        // 非同期でブロックを空気に設定してから、同期的に看板を設置
        try {
            Block block = location.getBlock();
            
            // 既存のブロックを空気に変更
            block.setType(Material.AIR);
            
            // 同期処理で看板を設置
            block.setType(Material.OAK_SIGN);
            
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                
                // 新しいAPIを使用（Component API）
                sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text("§6[土地販売]"));
                sign.getSide(org.bukkit.block.sign.Side.FRONT).line(1, Component.text("§e" + regionName));
                sign.getSide(org.bukkit.block.sign.Side.FRONT).line(2, Component.text("§7右クリックで"));
                sign.getSide(org.bukkit.block.sign.Side.FRONT).line(3, Component.text("§7土地を購入"));
                sign.update();

                // 看板データを保存
                ShopSignManager.addShopSign(location, regionName, owner.getUniqueId());
                
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            owner.sendMessage("§c看板の設置中にエラーが発生しました: " + e.getMessage());
            return false;
        }
    }

    private Location getFallbackSignLocation(Location pos1, Location pos2) {
        int centerX = (pos1.getBlockX() + pos2.getBlockX()) / 2;
        int centerZ = (pos1.getBlockZ() + pos2.getBlockZ()) / 2;
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        
        World world = pos1.getWorld();
        
        // 最下層から少し上の位置に設置を試みる
        for (int y = minY; y <= minY + 5; y++) {
            Location testLoc = new Location(world, centerX, y, centerZ);
            Block block = testLoc.getBlock();
            
            // 空気ブロックの場所に設置
            if (block.getType() == Material.AIR) {
                return testLoc;
            }
        }
        
        // それでも見つからない場合は、強制的に最下層に設置
        return new Location(world, centerX, minY, centerZ);
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
