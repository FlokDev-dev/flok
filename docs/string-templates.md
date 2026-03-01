# String Templates

Any string in Flok can embed variables and persistent values directly using the same `%var%` and `__key__` syntax.

```fk
send "Hello %player-name%, you have __coins-%player-name%__ coins!"
```

This is equivalent to:

```fk
send "Hello " + %player-name% + ", you have " + str(__coins-%player-name%__) + " coins!"
```

Templates are almost always the cleaner choice.

## Rules

- `%var-name%` inside a string interpolates a local variable.
- `__key__` inside a string interpolates a persistent value.
- Nested interpolation inside persistent keys works: `__coins-%player-name%__` resolves `%player-name%` first, then looks up the resulting key.
- The string is evaluated left-to-right at runtime.
- If a variable doesn't exist, it interpolates as an empty string.

## Color Codes

Minecraft color codes use `&` followed by a code:

```fk
send "&aGreen text &cred text &eand yellow!"
send "&l&6Bold gold title"
```

| Code | Color/Format |
|---|---|
| `&0`–`&9`, `&a`–`&f` | Colors (black → white) |
| `&l` | Bold |
| `&o` | Italic |
| `&n` | Underline |
| `&m` | Strikethrough |
| `&k` | Obfuscated |
| `&r` | Reset |

## Scoreboard Example

```fk
scoreboard-line 10 "&6Coins: &e__coins-%player-name%__"
scoreboard-line 9 "&fKills: &c__kills-%player-name%__"
scoreboard-line 7 "&7World: %player-world%"
```
