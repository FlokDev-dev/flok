# Event Cancellation

Scripts can cancel the event that triggered them using the `cancel` effect. This prevents the default server behavior from happening.

## Cancellable Events

`cancel` works on: `player-chat`, `player-command`, `player-move`, `block-break`, `block-place`

The lifecycle events (`player-join`, `player-quit`, `player-death`, `player-respawn`, `player-level-change`, `player-gamemode-change`) are **not cancellable** — `cancel` is silently ignored on them.

---

## Examples

```fk
# Prevent breaking bedrock
on block-break:
    if %block-type% == "bedrock":
        cancel
        send "&cYou can't break that!"

# Block profanity in chat
on player-chat:
    if %message% contains "badword":
        cancel
        send "&cWatch your language."

# Prevent falling into the void
on player-move:
    if %to-y% < 0:
        cancel

# Disable TNT placement in survival
on block-place:
    if %player-gamemode% == "survival" and %block-type% == "tnt":
        cancel
        send "&cTNT is disabled on this server."

# Block the /op command entirely
on player-command:
    if %command% == "/op":
        cancel
        send "&cNope."
```

---

## Uncancel

Use `uncancel` to reverse a cancellation — useful if you want to conditionally re-allow something that another script or plugin blocked:

```fk
on block-break:
    if %player-name% == "admin":
        uncancel
```

> **Note:** When multiple scripts handle the same event, they all run. If one script cancels and another uncancels, the final state depends on execution order, which is not guaranteed across files. Be deliberate about which script "owns" a given cancellation decision.
