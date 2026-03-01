# Your First Script

Create `plugins/Flok/scripts/hello.fk`:

```fk
on player-join:
    send "Welcome to the server, %player-name%!"
    send "&aYou joined for the first time!" if %first-join% is true
```

Run `/flok reload`. Every player who joins now gets a welcome message.

## Let's Go Further

Here's a more complete starter script showing variables, storage, and a custom command:

```fk
on player-join:
    __visits-%player-name%__ += 1
    if %first-join% is true:
        send "&aWelcome to the server for the first time!"
        give bread 5
    else:
        send "&7Welcome back! This is visit #__visits-%player-name__%."

on player-death:
    __deaths-%player-name%__ += 1

command stats:
    send "&6=== Your Stats ==="
    send "&fVisits: &e__visits-%player-name%__"
    send "&fDeaths: &c__deaths-%player-name%__"
```

## What's Happening Here

- `on player-join:` — an **event block**. The indented body runs when a player joins.
- `%player-name%` — a **local variable** auto-populated by the event.
- `__visits-%player-name%__` — a **persistent variable** saved to disk. It survives restarts.
- `command stats:` — registers `/stats` as a real server command.

Next: read [Script Structure](script-structure) for the full picture.
