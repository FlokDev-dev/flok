# Control Flow

## If / Else If / Else

```fk
if %player-health% <= 5:
    send "&cYou're nearly dead!"
else if %player-health% <= 10:
    send "&eWatch your health."
else:
    send "&aYou're healthy."
```

`elseif`, `else if`, and `elif` are all accepted.

---

## While Loop

```fk
%count% = 0
while %count% < 5:
    send "Count: %count%"
    %count% += 1
```

> **Safety:** A running op-limit counter will halt any while loop that runs for too long. See [Configuration](configuration) for the `max-ops` setting.

---

## For Each Loop

Iterate over a list:

```fk
%items% = ["sword", "shield", "potion"]
for item in %items%:
    send "Item: %item%"
```

Iterate over string characters:

```fk
for char in "hello":
    send "%char%"
```

---

## Repeat

```fk
repeat 5 times:
    broadcast "This repeats 5 times!"
```

The `times` keyword is optional.

---

## Break and Continue

```fk
for item in %list%:
    if %item% == "stop":
        break
    if %item% == "skip":
        continue
    send "%item%"
```

`stop` is an alias for `break`.

---

## Return

Use `return` to exit a function early and optionally pass back a value:

```fk
function clamp(value, min, max):
    if %value% < %min%:
        return %min%
    if %value% > %max%:
        return %max%
    return %value%
```

A function with no `return` statement returns `null`.
