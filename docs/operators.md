# Operators & Expressions

## Arithmetic

| Operator | Meaning |
|---|---|
| `+` | Addition (also string concatenation) |
| `-` | Subtraction |
| `*` | Multiplication |
| `/` | Division |
| `%` | Modulo |
| `^` | Power |

## Comparison

| Operator | Alternatives | Meaning |
|---|---|---|
| `==` | `is` | Equal |
| `!=` | `isnt`, `isn't` | Not equal |
| `<` | | Less than |
| `<=` | | Less than or equal |
| `>` | | Greater than |
| `>=` | | Greater than or equal |
| `contains` | | List or string contains value |

## Logical

```fk
if %health% > 5 and %food% > 3:
    send "You're doing fine."

if %level% == 0 or %coins% < 10:
    send "You need resources!"

if not %player-gamemode% is "creative":
    send "Survival only!"
```

## String Concatenation

The `+` operator concatenates strings:

```fk
%greeting% = "Hello, " + %player-name% + "!"
```

For most cases, [string templates](string-templates) are cleaner:

```fk
send "Hello, %player-name%!"
```

## List and Index Access

```fk
%my-list% = [1, 2, 3, "four"]
%first% = %my-list%[0]

%data% = {"name": "Steve", "score": 42}
%score% = %data%["score"]
```

## Operator Precedence

Standard math precedence applies: `^` before `* / %` before `+ -`. Use parentheses to be explicit:

```fk
%result% = (2 + 3) * 4    # 20
%result% = 2 + 3 * 4      # 14
```
