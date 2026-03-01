# Built-in Functions

These are available everywhere in scripts — no import needed.

## Math

| Function | Description |
|---|---|
| `abs(n)` | Absolute value |
| `ceil(n)` | Round up |
| `floor(n)` | Round down |
| `round(n)` | Round to nearest integer |
| `sqrt(n)` | Square root |
| `pow(base, exp)` | Exponentiation |
| `min(a, b)` | Minimum of two values |
| `max(a, b)` | Maximum of two values |
| `clamp(value, min, max)` | Clamp value between min and max |
| `log(n)` | Natural logarithm |
| `log10(n)` | Base-10 logarithm |
| `sin(deg)` | Sine (degrees) |
| `cos(deg)` | Cosine (degrees) |
| `tan(deg)` | Tangent (degrees) |
| `random()` | Random float between 0.0 and 1.0 |
| `random-int(min, max)` | Random integer between min and max (inclusive) |

---

## String

| Function | Description |
|---|---|
| `upper(s)` | Uppercase |
| `lower(s)` | Lowercase |
| `length(s)` | String length (or list size) |
| `trim(s)` | Remove whitespace from both ends |
| `starts-with(s, prefix)` | True if `s` starts with `prefix` |
| `ends-with(s, suffix)` | True if `s` ends with `suffix` |
| `contains(s, sub)` | True if `s` contains `sub` (also works on lists) |
| `replace(s, old, new)` | Replace all occurrences of `old` with `new` |
| `split(s, delimiter)` | Split string into a list |
| `substring(s, start, end)` | Substring (`end` optional) |
| `index-of(s, sub)` | Index of first occurrence (-1 if not found) |
| `repeat-str(s, n)` | Repeat string `n` times |
| `str(value)` | Convert any value to string |
| `num(value)` | Convert to number |
| `bool(value)` | Convert to boolean |

---

## List

| Function | Description |
|---|---|
| `range(start, end)` | List of integers from `start` (inclusive) to `end` (exclusive) |
| `push(list, value)` | Append value to list (modifies in place) |
| `pop(list)` | Remove and return the last item |
| `remove(list, index)` | Remove item at index |
| `join(list, sep)` | Join list into string (`sep` defaults to `, `) |
| `sum(list)` | Sum all numeric values |
| `sort(list)` | Return a sorted copy |
| `shuffle(list)` | Return a shuffled copy |
| `reverse(list)` | Return a reversed copy |
| `size(list)` / `count(list)` | Number of items (also works on maps and strings) |

---

## Type Checking

| Function | Description |
|---|---|
| `is-null(v)` | True if value is null |
| `is-number(v)` | True if value is a number |
| `is-string(v)` | True if value is a string |
| `is-list(v)` | True if value is a list |

---

## Utility

| Function | Description |
|---|---|
| `format-time(seconds)` | Format seconds as `HH:MM:SS` |

---

## Examples

```fk
# Pick a random item from a list
%loot% = ["diamond", "gold_ingot", "emerald", "iron_ingot"]
%drop% = %loot%[random-int(0, size(%loot%) - 1)]
give %drop% 1

# Parse command args safely
%raw% = %args%[0]
%amount% = num(%raw%)
if is-null(%amount%):
    send "&cInvalid amount."
else:
    __coins-%player-name%__ += %amount%

# Build a display string from a list
%online% = ["Alice", "Bob", "Charlie"]
send "Online: " + join(%online%, ", ")
```
