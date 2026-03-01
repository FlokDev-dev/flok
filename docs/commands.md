# Commands

Define custom commands directly in your scripts. Flok registers them as real server commands — they appear in tab-complete and `/help`.

## Basic Command

```fk
command hello:
    send "Hello, %player-name%!"
```

Players run it as `/hello`.

---

## Command with Arguments

Named parameters are accessible as local variables inside the command body. If a player doesn't provide enough arguments, missing parameters default to an empty string.

```fk
command greet(target, message):
    send "%player-name% says to %target%: %message%"
```

Players run it as: `/greet Steve Hello there!`

---

## Command Metadata

Attach metadata on the header line before the colon:

```fk
command fly permission: server.fly description: Toggle your flight:
    if %player-gamemode% == "creative":
        send "&cAlready in creative mode."
    else:
        set-fly true
        send "&aFlight enabled!"
```

```fk
command heal aliases: h, restore permission: staff.heal description: Heal yourself:
    heal
    set-food 20
    send "&aYou have been healed."
```

**Supported metadata fields:**

| Field | Example | Description |
|---|---|---|
| `permission:` | `permission: server.fly` | Required permission node |
| `description:` | `description: Toggle flight` | Shown in tab-complete and `/help` |
| `aliases:` | `aliases: h, restore` | Alternative command names |

---

## Raw Argument Access

Inside any command, you always have:

| Variable | Value |
|---|---|
| `%args%` | List of all arguments as strings |
| `%args-count%` | Number of arguments provided |

```fk
command info:
    if %args-count% == 0:
        send "Usage: /info <topic>"
    else:
        %topic% = %args%[0]
        send "Looking up: %topic%"
```

---

## Full Example

```fk
command pay aliases: give-coins permission: economy.pay description: Pay another player:
    if %args-count% < 2:
        send "&cUsage: /pay <player> <amount>"
    else:
        %target% = %args%[0]
        %amount% = num(%args%[1])
        if is-null(%amount%) or %amount% <= 0:
            send "&cInvalid amount."
        else if __coins-%player-name%__ < %amount%:
            send "&cYou don't have enough coins."
        else:
            __coins-%player-name%__ -= %amount%
            __coins-%target%__ += %amount%
            send "&aYou paid &e%target% &a%amount% coins."
```
