package com.mochiserver.minecraftShopLands;

import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftShopLands extends JavaPlugin {

    @Override
    public void onEnable() {
        // WorldGuardプラグインが有効かチェック
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuardが見つかりません。このプラグインを無効にします。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // イベントリスナーを登録
        getServer().getPluginManager().registerEvents(new AxeInteractListener(), this);
        getServer().getPluginManager().registerEvents(new SignInteractListener(), this);
        getServer().getPluginManager().registerEvents(new ShopSignPlaceListener(), this);

        // コマンドを登録
        getCommand("createshop").setExecutor(new CreateShopCommand());
        getCommand("renameland").setExecutor(new RenameLandCommand());
        getCommand("landinfo").setExecutor(new LandInfoCommand());
        getCommand("resetland").setExecutor(new LandInfoCommand());
        getCommand("debugregion").setExecutor(new DebugRegionCommand());

        getLogger().info("MinecraftShopLands プラグインが有効になりました！");
        getLogger().info("木の斧で範囲選択し、/createshop コマンドで土地を作成できます。");
    }

    @Override
    public void onDisable() {
        getLogger().info("MinecraftShopLands プラグインが無効になりました。");
    }
}
