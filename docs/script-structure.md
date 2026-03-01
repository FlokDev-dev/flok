# Script Structure

A `.fk` script is a flat file containing **event blocks**, **command blocks**, and **function definitions**. Indentation defines blocks — use 4 spaces (or 2, consistently).

```fk
# This is a comment

on player-join:
    send "Hello %player-name%!"

on player-quit:
    broadcast "&e%player-name% left the server."

command greet(target):
    send "Hey %target%, %player-name% says hello!"

function double(n):
    return %n% * 2
```

## Rules

**Block headers** end with an optional `:` — both `on player-join:` and `on player-join` work.

**Indentation** must be consistent within a file. Don't mix tabs and spaces, and don't randomly change depth. If you start with 4 spaces, use 4 spaces everywhere.

**Comments** start with `#` and can appear anywhere, including at the end of a line.

## Block Types

| Block | Syntax | Purpose |
|---|---|---|
| Event | `on player-join:` | Runs when a server event fires |
| Command | `command name(args):` | Registers a `/command` |
| Function | `function name(params):` | Reusable logic, callable from anywhere in the same file |

## Multiple Handlers

You can define multiple blocks for the same event or command across different files. All will fire. Order is not guaranteed across files.

```fk
# file: welcome.fk
on player-join:
    send "Welcome!"

# file: stats.fk  
on player-join:
    __joins-%player-name%__ += 1
```
