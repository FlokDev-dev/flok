# Troubleshooting

## Script won't load

Run `/flok reload` and check the output. Errors show the filename and line number:

```
✗ myscript.fk: [line 5] Expected INDENT but got NEWLINE
```

**Common causes:**

**Inconsistent indentation** — don't mix tabs and spaces, and don't randomly change indent depth within a file.

**Missing colon** — block headers like `on player-join`, `if`, `while` all need a `:` before the indented body (or at least a newline).

**Unterminated variable** — `%var` without a closing `%`, or `__key` without a closing `__`.

---

## Script loads but does nothing

Check the event name. `on join:` won't work — it must be `on player-join:`. See the full [Events Reference](events) for exact names.

Run `/flok list` to confirm the script registered the event at all.

Enable debug mode with `/flok debug` to see more output when the event fires.

---

## "Script exceeded op limit"

Your script has a runaway loop or is doing too much in one execution. Common causes:

- A `while` loop with a condition that never becomes `false`.
- A recursive function with no base case.

If your script genuinely needs more operations (e.g. processing a very large list), increase `safety.max-ops` in `config.yml`.

---

## Persistent data disappeared

Check `/flok storage list`. If it's empty after a crash, the last auto-save may not have run. The auto-save interval is 5 minutes. For critical data, run `/flok storage save` after important writes.

---

## Command not appearing in tab-complete

After `/flok reload`, Flok calls `syncCommands()` automatically. If the command still doesn't appear for a client, have that player relog — some clients cache the command list aggressively. If it still doesn't appear after a relog, restart the server.

---

## Effects do nothing / "Unknown effect"

Effect names are case-insensitive but must match exactly. Check the [Effects Reference](effects).

If you're using a custom effect from an addon, make sure the addon plugin is loaded and has registered the effect *before* your script fires.

---

## Double `%` in messages

When you want to display a literal `%` character in a message (e.g. "You have 50% HP"), escape it by doubling it:

```fk
send "You have 50%% HP remaining."
```

This also applies inside persistent key interpolation — `__visits-%player-name__%% times` will render the number followed by a literal `%`.
