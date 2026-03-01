# Flok vs Skript

Flok is designed as a performance-first alternative to Skript. Here's an honest comparison.

## Performance

| | Flok | Skript |
|---|---|---|
| Event dispatch | O(1) index lookup | Linear scan of all scripts |
| Context allocation | Pooled — zero allocation per event | New object(s) per event |
| Player variables | Lazy — injected only if accessed | Eager — always resolved |
| `player-move` safety | Hard 1s throttle + block-change filter built in | No built-in throttle; trivial to lag the server |
| Infinite loop protection | Op-limit halts runaway scripts safely | Can freeze the server |
| Async safety | Chat event marshalled to main thread | Mixed |

**The key difference:** Flok will not lag your server from a badly-written script. Skript can.

---

## Syntax

Both use an indentation-based, English-like syntax. Flok is more explicit about variable scope.

**Skript:**
```
on join:
    send "Welcome %player%!" to player
    add 100 to {coins::%player%}
```

**Flok:**
```fk
on player-join:
    send "Welcome %player-name%!"
    __coins-%player-name%__ += 100
```

Flok uses `%var%` for local variables and `__key__` for persistent storage — a clear visual distinction. In Skript, `{var}` and `{var::player}` look similar and are easy to confuse.

---

## What Flok Has That Skript Doesn't (Out of the Box)

- **Op-limit safety** — scripts can never freeze the server
- **Pooled execution contexts** — no GC pressure at high event rates
- **Clean addon API** — structured integration interface, not reflection hacks
- **Per-player move throttling** — built in, unconditional, can't be bypassed
- **Human-readable storage** — `flok_data.yml` is editable by admins directly

---

## What Skript Has That Flok Doesn't (Yet)

- Coverage for many more Bukkit entity and world events out of the box (Flok's addon API fills this gap for other plugins — adding new events requires extending `EventAdapter`)
- A larger community and addon ecosystem

---

## Verdict

If you need the widest possible event coverage and can manage the performance risk, Skript's ecosystem is larger. If you want a server that stays fast no matter what's in your scripts, and you're willing to write a small addon for unusual events, Flok is the better foundation.
