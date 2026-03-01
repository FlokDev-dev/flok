# Functions

Define reusable functions in any script file. Functions are available **within their own script only** — they don't cross file boundaries.

```fk
function greet(name):
    send "Hello, %name%!"

function add(a, b):
    return %a% + %b%

on player-join:
    greet(%player-name%)
    %result% = add(10, 5)
    send "10 + 5 = %result%"
```

## Rules

- Functions can call other functions, up to **64 levels deep** (stack overflow protection is built in).
- Recursion works but has the same depth limit.
- A function with no `return` statement returns `null`.
- Parameters are local variables inside the function body — they use `%param%` syntax.

## Recursive Example

```fk
function factorial(n):
    if %n% <= 1:
        return 1
    return %n% * factorial(%n% - 1)

on player-join:
    send "5! = " + str(factorial(5))
```

## No Global Functions

Functions defined in one file can't be called from another. If you need shared logic, either duplicate it or have a single script handle that responsibility entirely.

> The [Addon API](addon-api) lets other Java plugins register custom **effects** that behave like built-in effects — that's the extension point for cross-script shared functionality.
