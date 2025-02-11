# Finger

Have you ever wanted to finger your friends? This Minecraft plugin will display information about another player similar to [finger(1)](https://manpage.me/?finger) in [UNIX](https://www.youtube.com/watch?v=dFUlAQZB9Ng).

## Usage

```
/finger [USER]
```

![Screenshot](finger.png)

Output includes:

* Username
* Level
* World Location
* Ping
* Idle Time
* Health
* Hunger
* Meters Walked
* Blocks Mined
* Kills
* Damage Dealt
* Deaths
* Item Held
* Armor Stats

## Building

```
mvn package
```

## Installation

```
cp target/Finger-1.0.1.jar ~/minecraft/plugins
```

## Setup

When the server starts, these files will be written to the `plugins/Finger` folder if they don't exist:

* `config.yml`
* `template.html`

If you are using a permissions plugin, you can allow access to `/finger` using the `finger.use` permission. By default, only operators can use this command. The server must be restarted after changes are made.

## 🕷️ Web Page

This is an optional feature that will write a flat HTML file to disk every minute.

![Screenshot](www.png)

You can configure the plugin to regularly write player information to an HTML page by adding a path to `config.yml` under the `html_player_report` option e.g.

```
html_player_report: /var/www/html/players.html
```

The process running Minecraft must have write access to the specified directory. If you have enabled this feature and the HTML file never appears, check your console for an error like this:

```
[11:32:49 WARN]: java.io.FileNotFoundException: /var/www/html/players.html (Permission denied)
```

You can also optionally set the `server_name` option e.g.

```
server_name: My Minecraft Server
```

This maps to the `{SERVER_NAME}` placeholder in the template.

## 📝 Template Placeholders

The HTML template can be modified as you like. All placeholders are optional, removing them will not cause problems.

## **🔹 Player-Specific Placeholders**
These placeholders are used **inside** the **player-card template** and contain **per-player** details.

| Placeholder       | Description |
|------------------|-------------|
| `{BLOCKS_MINED}` | Number of blocks mined by the player |
| `{BOOTS}` | Player’s equipped boots |
| `{CHESTPLATE}` | Player’s equipped chestplate |
| `{DAMAGE_DEALT}` | Total damage dealt by the player |
| `{DEATHS}` | Number of times the player has died |
| `{GAMEMODE}` | The player’s current game mode (`Survival`, `Creative`, etc.) |
| `{HEALTH}` | Player's current health |
| `{HELD_ITEM}` | The item currently held in the player's main hand |
| `{HELMET}` | Player’s equipped helmet |
| `{HUNGER}` | Player's hunger level |
| `{IDLE}` | How long the player has been idle |
| `{LEGGINGS}` | Player’s equipped leggings |
| `{LEVEL}` | The player's experience level |
| `{MOBS_KILLED}` | Total number of mobs the player has killed |
| `{PING}` | The player's network ping in milliseconds |
| `{PLAYER_NAME}` | The player's in-game username |
| `{UUID}` | The player's unique identifier (UUID) |
| `{WALKED}` | Total distance walked by the player (in meters) |
| `{WORLD}` | The world the player is currently in |
| `{X}` | Player’s X coordinate |
| `{Y}` | Player’s Y coordinate |
| `{Z}` | Player’s Z coordinate |

---

## **🌍 Global Server Placeholders**
These placeholders apply **to the entire server** and are not player-specific.

| Placeholder       | Description |
|------------------|-------------|
| `{BUKKIT_VERSION}` | The Bukkit/Spigot/Paper version of the server |
| `{GITHUB_URL}` | Link to the GitHub repository for the plugin |
| `{PLAYER_ENTRIES}` | Placeholder for all player cards (used in the main template) |
| `{SERVER_MEMORY_ALLOCATED}` | Amount of memory allocated to the Minecraft server (MB) |
| `{SERVER_MEMORY_FREE}` | Free memory available in the Minecraft server (MB) |
| `{SERVER_MEMORY_MAX}` | Maximum memory limit for the Minecraft server (MB) |
| `{SERVER_MEMORY_USED}` | Memory currently used by the Minecraft server (MB) |
| `{SERVER_NAME}` | The server’s name (configured in `server.properties` or `bukkit.yml`) |
| `{SERVER_UPTIME}` | How long the server has been running since startup |
| `{SERVER_VERSION}` | The version of Minecraft the server is running |
| `{TIMESTAMP}` | The current time when the report was generated |
| `{VERSION_STRING}` | The version of the `Finger` plugin |

## 🚙 Under the Hood

Every minute, the JSON file `plugins/Finger/player_data.json` is written. The format of this file is:

```
{
    "MINECRAFT_USER_ID": {
        "damage_dealt": 440.398380279541,
        "distance_walked": 38062.187168638084,
        "mobs_killed": 18,
        "blocks_mined": 1980,
        "deaths": 3
    }
}
```

Do not edit this file while the server is running or your changes will be lost. These counts are running totals since the plugin was installed.

# Etc

Created by [harrison.page](https://harrison.page) on 9-Feb-2025.
