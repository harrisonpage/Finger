package love.toad;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import love.toad.PlayerActivityListener;

public class Finger extends JavaPlugin implements CommandExecutor {
    private final HashMap<UUID, Long> lastActiveTime = new HashMap<>();
    private final HashMap<UUID, Double> distanceWalked = new HashMap<>();
    private JSONObject playerData;
    private final File dataFile = new File(getDataFolder(), "player_data.json");

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                try (FileWriter writer = new FileWriter(dataFile)) {
                    writer.write("{}\n"); // Write empty JSON object
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.getCommand("finger").setExecutor(this);
        loadPlayerData();
        getServer().getPluginManager().registerEvents(new PlayerActivityListener(lastActiveTime, distanceWalked, playerData, dataFile), this);
    }

    @Override
    public void onDisable() {
        savePlayerData();
    }

    private void loadPlayerData() {
        if (!dataFile.exists()) {
            playerData = new JSONObject();
            return;
        }
        try (FileReader reader = new FileReader(dataFile)) {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            if (obj instanceof JSONObject) {
                playerData = (JSONObject) obj;
                for (Object key : playerData.keySet()) {
                    JSONObject playerEntry = (JSONObject) playerData.get(key);
                    double distance = ((Number) playerEntry.getOrDefault("distance_walked", 0.0)).doubleValue();
                    distanceWalked.put(UUID.fromString((String) key), distance);
                }
            } else {
                playerData = new JSONObject();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            playerData = new JSONObject();
        }
    }

    private void savePlayerData() {
        for (UUID uuid : distanceWalked.keySet()) {
            JSONObject playerEntry = (JSONObject) playerData.getOrDefault(uuid.toString(), new JSONObject());
            playerEntry.put("distance_walked", distanceWalked.get(uuid));
            playerData.put(uuid.toString(), playerEntry);
        }
        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(playerData.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player requester = (Player) sender;
        Player target;

        if (args.length == 0) {
            target = requester;
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                requester.sendMessage(ChatColor.RED + "Player " + args[0] + " is not online.");
                return true;
            }
        }

        sendFingerReport(requester, target);
        return true;
    }

    private void sendFingerReport(Player requester, Player target) {
        long idleTime = System.currentTimeMillis() - lastActiveTime.getOrDefault(target.getUniqueId(), System.currentTimeMillis());
        String idleStatus = idleTime > 0 ? (idleTime / 60000) + "m" : "Active";

        double walked = distanceWalked.getOrDefault(target.getUniqueId(), 0.0);

        requester.sendMessage(ChatColor.GOLD + "[Finger] " + ChatColor.YELLOW + target.getName() + " (Level " + target.getLevel() + ")");
        requester.sendMessage("- Distance Walked: " + String.format("%.2f", walked) + " meters");

        Location loc = target.getLocation();
        requester.sendMessage("- Location: " + loc.getWorld().getName() + " (XYZ: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
        requester.sendMessage("- Ping: " + target.getPing() + "ms Idle: " + idleStatus);
        requester.sendMessage("- Mode: " + target.getGameMode() + " Env: " + target.getWorld().getEnvironment());
        requester.sendMessage("- Health: " + Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth()) + " Hunger: " + target.getFoodLevel() + "/20");

        ItemStack heldItem = target.getInventory().getItemInMainHand();
        requester.sendMessage("- Holding: " + getItemName(heldItem));

        sendArmorInfo(requester, target, "Helmet", target.getInventory().getHelmet());
        sendArmorInfo(requester, target, "Chestplate", target.getInventory().getChestplate());
        sendArmorInfo(requester, target, "Leggings", target.getInventory().getLeggings());
        sendArmorInfo(requester, target, "Boots", target.getInventory().getBoots());
    }

    private void sendArmorInfo(Player requester, Player target, String piece, ItemStack item) {
        String itemName = getItemName(item);
        requester.sendMessage("- " + piece + ": " + itemName);
    }

    private String getItemName(ItemStack item) {
        return (item != null && item.getType() != Material.AIR) ? item.getType().name().replace("_", " ") : "None";
    }
}
