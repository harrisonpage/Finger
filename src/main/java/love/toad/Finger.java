package love.toad;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

public class Finger extends JavaPlugin {
    private static String PLUGIN_NAME = "Finger";
    private static String VERSION = "1.0.0";
    private static String VERSION_STRING = PLUGIN_NAME + '/' + VERSION;
    private static String GITHUB_URL = "https://github.com/harrisonpage/Finger";
    private final File dataFile = new File(getDataFolder(), "player_data.json");
    private PlayerActivityListener playerActivityListener;
    private File configFile;
    private FileConfiguration config;
    private String htmlPlayerReport;
    private String serverName;
    Logger log = Logger.getLogger("Minecraft");
    long startTime;

    @Override
    public void onEnable() {
        startTime = System.currentTimeMillis();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        loadConfig();
        playerActivityListener = new PlayerActivityListener(dataFile);
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
        playerActivityListener.loadPlayerData();
        getServer().getPluginManager().registerEvents(playerActivityListener, this);
        log.info("[" + VERSION_STRING + "] Enabled: " + GITHUB_URL);

        if (isHtmlReportEnabled()) {
            // initial first write
            writeReport();
            // then write HTML report every 60s
            Bukkit.getScheduler().runTaskTimerAsynchronously(Bukkit.getPluginManager().getPlugin("Finger"), this::writeReport, 1200L, 1200L);
            log.info("[" + VERSION_STRING + "] Periodically writing " + htmlPlayerReport + " file");
        }
    }

    @Override
    public void onDisable() {
        playerActivityListener.savePlayerData();
        log.info("[" + VERSION_STRING + "] Disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players");
            return true;
        }

        Player requester = (Player) sender;
        Player target;

        if (args.length == 0) {
            target = requester;
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                requester.sendMessage(ChatColor.RED + "Player " + args[0] + " is not online");
                return true;
            }
        }

        sendFingerReport(requester, target);
        return true;
    }

    private void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            log.info("[" + VERSION_STRING + "] Initial configuration file " + configFile + " created");
            saveDefaultConfig();
        }
        copyTemplateFile();

        config = YamlConfiguration.loadConfiguration(configFile);
        htmlPlayerReport = config.getString("html_player_report", "");
        serverName = config.getString("server_name", "My Server");

        if (htmlPlayerReport.isEmpty()) {
            htmlPlayerReport = null; // Disable feature if not set
        }
    }

    public void saveDefaultConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            config = new YamlConfiguration();
            config.set("html_player_report", "");
            config.set("server_name", "My Server");
            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyTemplateFile() {
        File templateFile = new File(getDataFolder(), "template.html");
        if (!templateFile.exists()) {
            log.info("[" + VERSION_STRING + "] Initial HTML template file created");
            getDataFolder().mkdirs();
            try (InputStream in = getResource("template.html");
                 FileOutputStream out = new FileOutputStream(templateFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getConfigData() {
        return config;
    }

    public String getHtmlPlayerReport() {
        return htmlPlayerReport;
    }

    public boolean isHtmlReportEnabled() {
        return htmlPlayerReport != null;
    }


    private String s(int i) {
        return i == 1 ? "" : "s";
    }

    private void sendFingerReport(Player requester, Player target) {
        long idleTime = System.currentTimeMillis() - playerActivityListener.lastActiveTime.getOrDefault(target.getUniqueId(), System.currentTimeMillis());
        String idleStatus = idleTime > 0 ? (idleTime / 60000) + "m" : "Active";

        double walked = playerActivityListener.distanceWalked.getOrDefault(target.getUniqueId(), 0.0);
        double damageDealt = playerActivityListener.damageDealt.getOrDefault(target.getUniqueId(), 0.0);
        int mobsKilled = playerActivityListener.mobsKilled.getOrDefault(target.getUniqueId(), 0);
        int blocksMined = playerActivityListener.blocksMined.getOrDefault(target.getUniqueId(), 0);
        int deaths = playerActivityListener.deaths.getOrDefault(target.getUniqueId(), 0);

        requester.sendMessage(ChatColor.GOLD + "[Finger] " + ChatColor.YELLOW + target.getName() + " (Level " + target.getLevel() + ")");
        Location loc = target.getLocation();
        requester.sendMessage("- Location: " + loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ") " + target.getGameMode());
        requester.sendMessage("- Ping: " + target.getPing() + "ms, Idle: " + idleStatus);
        requester.sendMessage("- Health: " + Math.round(target.getHealth()) + "/" + Math.round(target.getHealthScale()) + " Hunger: " + target.getFoodLevel() + "/20");
        requester.sendMessage("- Stats: Walked " + String.format("%.2f", walked) + " meters, mined " + blocksMined + " block" + s(blocksMined));
        requester.sendMessage("- Combat: " + mobsKilled + " kill" + s(mobsKilled) + ", " + Math.round(damageDealt) + " damage dealt, " + deaths + " death" + s(deaths));

        ItemStack heldItem = target.getInventory().getItemInMainHand();
        requester.sendMessage("- Holding: " + getItemDetails(heldItem));

        sendArmorInfo(requester, target, "Helmet", target.getInventory().getHelmet());
        sendArmorInfo(requester, target, "Chestplate", target.getInventory().getChestplate());
        sendArmorInfo(requester, target, "Leggings", target.getInventory().getLeggings());
        sendArmorInfo(requester, target, "Boots", target.getInventory().getBoots());
    }

    private void writeReport() {
        File templateFile = new File(getDataFolder(), "template.html");
        String template = loadTemplate(templateFile);

        if (template == null) {
            return; // Abort if template couldn't be loaded
        }

        // Extract player-card template
        String playerCardTemplate = extractPlayerCardTemplate(template);
        if (playerCardTemplate == null) {
            log.warning("[Finger] Player card template not found in template.html");
            return;
        }

        StringBuilder playerEntries = new StringBuilder();
        for (Player target : Bukkit.getOnlinePlayers()) {
            long idleTime = System.currentTimeMillis() - playerActivityListener.lastActiveTime.getOrDefault(target.getUniqueId(), System.currentTimeMillis());
            String idleStatus = idleTime > 0 ? (idleTime / 60000) + "m" : "Active";

            double walked = playerActivityListener.distanceWalked.getOrDefault(target.getUniqueId(), 0.0);
            double damageDealt = playerActivityListener.damageDealt.getOrDefault(target.getUniqueId(), 0.0);
            int mobsKilled = playerActivityListener.mobsKilled.getOrDefault(target.getUniqueId(), 0);
            int blocksMined = playerActivityListener.blocksMined.getOrDefault(target.getUniqueId(), 0);
            int deaths = playerActivityListener.deaths.getOrDefault(target.getUniqueId(), 0);

            Location loc = target.getLocation();
            String uuid = target.getUniqueId().toString();

            String entry = playerCardTemplate.replace("{PLAYER_NAME}", target.getName())
                                   .replace("{UUID}", uuid)
                                   .replace("{LEVEL}", String.valueOf(target.getLevel()))
                                   .replace("{WORLD}", loc.getWorld().getName())
                                   .replace("{X}", String.valueOf(loc.getBlockX()))
                                   .replace("{Y}", String.valueOf(loc.getBlockY()))
                                   .replace("{Z}", String.valueOf(loc.getBlockZ()))
                                   .replace("{GAMEMODE}", target.getGameMode().toString())
                                   .replace("{PING}", String.valueOf(target.getPing()))
                                   .replace("{IDLE}", idleStatus)
                                   .replace("{HEALTH}", Math.round(target.getHealth()) + "/" + Math.round(target.getHealthScale()))
                                   .replace("{HUNGER}", target.getFoodLevel() + "/20")
                                   .replace("{WALKED}", String.format("%.2f", walked))
                                   .replace("{BLOCKS_MINED}", String.valueOf(blocksMined))
                                   .replace("{MOBS_KILLED}", String.valueOf(mobsKilled))
                                   .replace("{DAMAGE_DEALT}", String.valueOf(Math.round(damageDealt)))
                                   .replace("{DEATHS}", String.valueOf(deaths))
                                   .replace("{HELD_ITEM}", getItemDetails(target.getInventory().getItemInMainHand()))
                                   .replace("{HELMET}", getItemDetails(target.getInventory().getHelmet()))
                                   .replace("{CHESTPLATE}", getItemDetails(target.getInventory().getChestplate()))
                                   .replace("{LEGGINGS}", getItemDetails(target.getInventory().getLeggings()))
                                   .replace("{BOOTS}", getItemDetails(target.getInventory().getBoots()));

            playerEntries.append(entry).append("\n");
        }

        String playerEntriesString = playerEntries.length() > 0 ? playerEntries.toString() : "<p style='text-align: center;'>Nobody Online</p>";
        String finalHtml = template.replace("{PLAYER_ENTRIES}", playerEntriesString);

        String serverVersion = Bukkit.getServer().getVersion();
        String bukkitVersion = Bukkit.getServer().getBukkitVersion();

        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        long uptimeMillis = System.currentTimeMillis() - startTime;
        long uptimeSeconds = uptimeMillis / 1000;
        long uptimeMinutes = uptimeSeconds / 60;
        long uptimeHours = uptimeMinutes / 60;
        String uptimeFormatted = uptimeHours + "h " + (uptimeMinutes % 60) + "m " + (uptimeSeconds % 60) + "s";

        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024; // MB
        long allocatedMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long usedMemory = allocatedMemory - freeMemory;


        finalHtml = finalHtml.replace("{VERSION_STRING}", VERSION_STRING)
            .replace("{TIMESTAMP}", timestamp)
            .replace("{SERVER_NAME}", serverName)
            .replace("{SERVER_VERSION}", serverVersion)
            .replace("{BUKKIT_VERSION}", bukkitVersion)
            .replace("{SERVER_UPTIME}", uptimeFormatted)
            .replace("{SERVER_MEMORY_MAX}", String.valueOf(maxMemory))
            .replace("{SERVER_MEMORY_ALLOCATED}", String.valueOf(allocatedMemory))
            .replace("{SERVER_MEMORY_FREE}", String.valueOf(freeMemory))
            .replace("{SERVER_MEMORY_USED}", String.valueOf(usedMemory))
            .replace("{GITHUB_URL}", GITHUB_URL);

        File file = new File(getHtmlPlayerReport());
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(finalHtml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractPlayerCardTemplate(String template) {
        String startTag = "<script id=\"player-card-template\" type=\"text/template\">";
        String endTag = "</script>";
        int startIndex = template.indexOf(startTag);
        int endIndex = template.indexOf(endTag, startIndex);
        if (startIndex == -1 || endIndex == -1) {
            return null;
        }
        return template.substring(startIndex + startTag.length(), endIndex).trim();
    }

    private String loadTemplate(File file) {
        if (!file.exists()) {
            log.warning("Template file not found: " + file.getPath());
            return null;
        }
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendArmorInfo(Player requester, Player target, String piece, ItemStack item) {
        requester.sendMessage("- " + piece + ": " + getItemDetails(item));
    }

    private String getItemDetails(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "None";
        StringBuilder details = new StringBuilder(item.getType().name().replace("_", " "));
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            details.append(" (Enchanted: ");
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                details.append(entry.getKey().getKey().getKey()).append(" ").append(entry.getValue()).append(", ");
            }
            details.setLength(details.length() - 2);
            details.append(")");
        }
        return details.toString();
    }
}
