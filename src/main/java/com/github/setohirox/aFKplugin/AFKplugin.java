package com.github.setohirox.aFKplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class AFKplugin extends JavaPlugin implements CommandExecutor, Listener {

    private static final long AFK_TIMEOUT = 5 * 60 * 1000; // 5分（ミリ秒）

    private final Set<String> isAFK = new HashSet<>();
    private final Map<UUID, Long> lastAction = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("afk").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        // AFKチェックの定期タスク
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (isAFK.contains(player.getName())) continue; // すでにAFKなら無視

                    long last = lastAction.getOrDefault(uuid, now);
                    if (now - last >= AFK_TIMEOUT) {
                        isAFK.add(player.getName());
                        Bukkit.broadcast(createMessage(player.getName(), "さんが離席中です"));
                    }
                }
            }
        }.runTaskTimer(this, 20L, 60 * 20L); // 1秒後開始、60秒ごとに実行
    }

    @Override
    public void onDisable() {
    }

    // /afk コマンド
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("afk")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                String name = p.getName();
                if (isAFK.contains(name)) {
                    isAFK.remove(name);
                    Bukkit.broadcast(createMessage(name, "さんの離席が解除されました"));
                } else {
                    isAFK.add(name);
                    Bukkit.broadcast(createMessage(name, "さんが離席中です"));
                }
                lastAction.put(p.getUniqueId(), System.currentTimeMillis()); // 時刻を更新
                return true;
            }
        }
        return false;
    }

    // 移動によるAFK解除
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getFrom().distanceSquared(e.getTo()) == 0) return; // 移動してなければ無視

        Player p = e.getPlayer();
        String name = p.getName();
        lastAction.put(p.getUniqueId(), System.currentTimeMillis());
        if (isAFK.remove(name)) {
            Bukkit.broadcast(createMessage(name, "さんの離席が解除されました"));
        }
    }

    // チャットでAFK解除＋アクティブ記録
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String name = p.getName();
        lastAction.put(p.getUniqueId(), System.currentTimeMillis());
        if (isAFK.remove(name)) {
            Bukkit.getScheduler().runTask(this, () ->
                    Bukkit.broadcast(createMessage(name, "さんの離席が解除されました"))
            );
        }
    }

    // メッセージ組み立て
    private Component createMessage(String playerName, String suffix) {
        return Component.text("[")
                .color(NamedTextColor.GRAY)
                .append(Component.text("AFK").color(NamedTextColor.YELLOW))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Component.text(playerName + suffix).color(NamedTextColor.GRAY));
    }
}
