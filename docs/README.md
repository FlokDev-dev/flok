# What is Flok?

Flok is a fast, lightweight scripting plugin for Paper/Spigot servers. You write plain `.fk` files — no Java, no compile step, no restarts for most changes. Drop a script in, run `/flok reload`, and it's live.

> **Requires:** Paper or Spigot 1.21+, Java 21+

## Design Goals

**Zero server lag from scripts** — op-limit, throttling, and pooled contexts mean a badly-written script can't freeze your server.

**Syntax that feels natural** — indentation-based, English-like, no boilerplate.

**A clean addon API** — other plugins can extend Flok without reflection hacks.

## Quick Example

```fk
on player-join:
    send "Welcome back, %player-name%!"
    __visits-%player-name%__ += 1
    send "This is visit number __visits-%player-name__%."
```

That's it. Drop it in `plugins/Flok/scripts/`, run `/flok reload`, and it's live.

## How It Works

Flok parses `.fk` files into an AST at load time. When an event fires, Flok dispatches to registered handlers via an O(1) index — there's no linear scan of scripts. Execution contexts are pooled, so there's zero allocation overhead per event at high throughput.

An operation counter is enforced on every execution. If a script hits the limit (default: 50,000 ops), it's halted and logged — the server thread is never blocked.
