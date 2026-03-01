# Effects (Actions)

Effects are the actions scripts can take. They're called without parentheses, with arguments separated by spaces or commas.

## Messaging

```fk
# Send a message to the triggering player
send "Hello, %player-name%!"

# Broadcast to all online players
broadcast "&aServer announcement!"

# Action bar (appears above the hotbar)
actionbar "&6Balance: __coins-%player-name%__"

# Title and subtitle (both arguments required)
title "&lWelcome!" "&7Enjoy your stay"
```

Color codes use `&` + a standard Minecraft format code. See [String Templates](string-templates) for the full reference.

---

## Player State

```fk
# Fully restore health
heal

# Set food level (0–20)
set-food 20

# Enable or disable flight
set-fly true
set-fly false

# Change game mode
set-gamemode creative
set-gamemode survival

# Teleport to coordinates
teleport 100 64 -200
```

---

## Inventory

```fk
# Give an item (material name, amount optional — defaults to 1)
give diamond 5
give iron-sword 1
give oak-log 64
```

Material names follow Minecraft's internal names. You can use underscores or hyphens interchangeably (`oak_log` and `oak-log` both work).

---

## Sound

```fk
# Play a sound at the player's location
# Arguments: sound-name [volume] [pitch]
sound entity-player-levelup
sound block-note-block-pling 1.0 1.5
```

Sound names follow Bukkit's `Sound` enum — case-insensitive, hyphens accepted.

---

## Potion Effects

```fk
# Apply a potion effect
# Arguments: type duration-in-seconds [amplifier]
potion speed 30 2
potion regeneration 10 1
potion night-vision 60
```

---

## Scoreboard

```fk
# Set the sidebar title
scoreboard-title "&6&lMy Server"

# Set a line — higher score value = higher on the board
scoreboard-line 10 "&fCoins: &e__coins-%player-name%__"
scoreboard-line 9 "&fKills: &c__kills-%player-name%__"
scoreboard-line 8 " "
scoreboard-line 7 "&7%player-world%"
```

---

## Custom Effects (Addons)

Plugins using the [Addon API](addon-api) can register additional effects that behave identically to built-ins:

```fk
on player-join:
    send-discord "**%player-name%** joined the server!"
```

If an effect name isn't recognized, Flok will log an "Unknown effect" error. See [Troubleshooting](troubleshooting).
