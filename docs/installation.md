# Installation

## Setup

1. Drop `Flok.jar` into your `plugins/` folder.
2. Start or restart the server.
3. Flok creates `plugins/Flok/scripts/` automatically.
4. Put your `.fk` files in that folder.
5. Scripts load on startup. Use `/flok reload` to reload without restarting.

> **Requirements:** Paper or Spigot 1.21+, Java 21+

## File Layout

After first run, your plugin folder will look like this:

```
plugins/
└── Flok/
    ├── config.yml
    ├── flok_data.yml     ← persistent storage lives here
    └── scripts/
        ├── hello.fk
        └── your-script.fk
```

## Verifying It Works

Run `/flok info` in-game or in the console. You should see engine stats including loaded script count, registered events, and JVM memory usage.

If you see an error on startup, check the server log — Flok prints the filename and line number of any parse error.
