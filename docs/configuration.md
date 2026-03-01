# Configuration

`plugins/Flok/config.yml` is created automatically on first run.

```yaml
# Enable verbose debug logging (shows script errors with more detail)
debug: false

# Scripts folder (relative to plugin data folder)
scripts:
  folder: scripts

# Persistent storage
storage:
  data-file: flok_data.yml

# Safety limits
safety:
  # Max AST operations per script execution before halting
  # Prevents infinite loops from freezing the server
  # Default 50000 is generous for any normal script
  max-ops: 50000
```

---

## Options

### `debug`

Enables verbose logging to the server console. Shows detailed error context, script execution traces, and more. Toggle this live with `/flok debug` instead of editing the file.

**Default:** `false`

---

### `scripts.folder`

The folder Flok looks in for `.fk` files, relative to `plugins/Flok/`. You can change this if you want to organize scripts in a subdirectory.

**Default:** `scripts`

---

### `storage.data-file`

The filename for persistent storage, relative to `plugins/Flok/`. This is plain YAML — editable by hand when the server is stopped.

**Default:** `flok_data.yml`

---

### `safety.max-ops`

The maximum number of AST operations a single script execution can perform before being forcibly halted. Every expression, statement, and loop iteration counts as one operation.

A script that hits this limit is stopped and a warning is logged. The server thread is never blocked — this is a hard safety guarantee.

**Default:** `50000`

The default is intentionally conservative. If you have a legitimate script that processes large datasets and hits the limit, increase this value. Typical event handlers use far fewer than 1,000 ops.
