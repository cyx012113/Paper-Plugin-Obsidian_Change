# Obsidian Change Powder Plugin

[MIT License](https://mit-license.org/)

Starting from version `2.0.0 + MIT`, the source code license has been changed from CC BY-SA 4.0 to the MIT license.

**Version 2.0.0 + MIT** – A lightweight Paper/Spigot plugin that lets players convert obsidian into wooden planks using a special powder.

## Features

- Right-click obsidian with **Obsidian Change Powder** to start conversion.
- Configurable conversion delay (default 3 seconds) and cooldown (default 5 seconds).
- Fully customizable messages and result block material.
- Tab completion for the `/obsidian give` command.
- Permission system (`obsidian.use` to use the item, `obsidian.admin` to give items).

## Installation

1. Download the latest `obsidian_change-2.0.0.jar`.
2. Place it in your server's `plugins/` folder.
3. Restart the server (or reload plugins).
4. The configuration file `plugins/obsidian_change/config.yml` will be generated automatically.
5. Edit the config to your liking and reload with `/obsidian reload` (optional, requires plugin restart for current version).

## Commands & Permissions

| Command                                                   | Permission       | Description                                                                |
| --------------------------------------------------------- | ---------------- | -------------------------------------------------------------------------- |
| `/obsidian give [player] obsidian_change_powder [amount]` | `obsidian.admin` | Give the powder to a player. If no player is specified, gives to yourself. |
| N/A                                                       | `obsidian.use`   | Allows players to right-click with the powder. (Default: true)             |

## Configuration (`config.yml`)

```yaml
# Delay before conversion (ticks, 20 ticks = 1 sec)
conversion-time-ticks: 60

# Cooldown after using (seconds)
cooldown-seconds: 5

# Result block material (any valid Bukkit Material)
result-block: OAK_PLANKS

# All message strings – use & for color codes, {placeholder} for dynamic values
messages:
  must-look-obsidian: "&cYou must look at obsidian to use!"
  cooldown: "&cSkill on cooldown, remaining {seconds} seconds"
  already-converting: "&cYou already have an ongoing conversion!"
  start-conversion: "&eStarting obsidian conversion, please wait {seconds} seconds..."
  success: "&aObsidian successfully converted to {result}!"
  failed: "&cConversion failed: target block has been changed or destroyed."
  usage-give: "&cUsage: /obsidian give [player] obsidian_change_powder [amount]"
  unknown-item: "&cUnknown item: {item}"
  player-not-online: "&cPlayer {player} is not online!"
  given: "&aGiven {player} {amount} Obsidian Change Powder"
  received: "&aYou have received {amount} Obsidian Change Powder"
  console-must-specify: "&cConsole must specify player name: /obsidian give <player> obsidian_change_powder [amount]"
  amount-integer: "&cAmount must be an integer!"
```

## How to use in-game

1. Obtain the powder: `/obsidian give obsidian_change_powder 1` (requires `obsidian.admin`).
2. Hold it in your hand.
3. Right-click on any obsidian block.
4. Wait the configured delay (default 3 seconds) – the block will turn into wooden planks.
5. After a successful conversion, you must wait the cooldown period before using another powder.

## Developer Information

- **API version**: 1.21
- **Dependencies**: None (pure Paper/Spigot API)
- **Source code**: Fully open under [insert license]
- **Author**: cyx012113

## Building from source

1. Clone the repository.
2. Use Maven: `mvn clean package`
3. The output JAR will be in `target/obsidian_change-2.0.0.jar`.

## License

This plugin is licensed under CC BY-SA 4.0. You are free to share and adapt it, but must give appropriate credit and share under the same license.

## Support

For issues or suggestions, contact the author via Discord or GitHub.
