package net.claimworld.solidborder;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

import static org.bukkit.Bukkit.*;

public final class Solidborder extends JavaPlugin implements Listener {

    private String penalty;
    private boolean broadcast;

    private void updateData(CommandSender sender) {
        getScheduler().runTaskAsynchronously(this, () -> {
            reloadConfig();
            FileConfiguration config = getConfig();

            penalty = config.getString("settings.penalty");
            broadcast = config.getBoolean("settings.broadcast");

            getLogger().log(Level.INFO, "Data has been updated to values: [penalty: " + penalty + "], [broadcast: " + broadcast + "].");
            if (sender instanceof Player) sender.sendMessage("Data bas been successfully reloaded. Check console for details.");
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        updateData(sender);
        return true;
    }

    public void logEvents(Player player, World world, Location location) {
        String message = "Player " + player.getName() + " has tried to cross world border at " + world.getName() + " (" + Math.round(location.getX()) + "x, " + Math.round(location.getY()) + "y, " + Math.round(location.getZ()) + "z";
        getLogger().log(Level.WARNING, message);

        if (!broadcast) return;

        for (Player onlinePlayer : getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission("claimworld.solidborder.broadcast")) continue;
            onlinePlayer.sendMessage(ChatColor.GRAY + message);
        }
    }

    public void executePenalty(Player player) {
        if (penalty == null || penalty.equals("none")) return;
        if (penalty.equals("strike")) getScheduler().runTask(this, () -> player.getWorld().strikeLightning(player.getLocation()));
        if (penalty.equals("kill")) getScheduler().runTask(this, () -> player.setHealth(0));
    }

    @EventHandler
    public void event(EntityDamageEvent event) {
        getScheduler().runTaskAsynchronously(this, () -> {
            if (event.isCancelled()) return;
            if (event.getEntity().getType() != EntityType.PLAYER) return;
            if (event.getCause() != EntityDamageEvent.DamageCause.SUFFOCATION) return;

            Player player = (Player) event.getEntity();
            Location location = player.getLocation();
            World world = player.getWorld();
            if (world.getWorldBorder().isInside(location)) return;

            executePenalty(player);

            logEvents(player, world, location);
        });
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("reload-solidborder").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        updateData(getConsoleSender());
    }
}