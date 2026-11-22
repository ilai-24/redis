# Redis

A small in-memory key-value server written in Java, inspired by Redis. It speaks a simple **line-based text protocol** over TCP and supports three commands: `SET`, `GET` and `DEL`.

It is a learning project, built to practice network programming, concurrency, layered design and testing.

## Features

- `SET`, `GET` and `DEL` over TCP
- Many clients at the same time (one thread per client), all sharing one store
- Input validation with clear `-ERR` messages; an invalid command gets an error and the connection stays open
- Protection against oversized lines (1 MiB limit)
- Unit tests for every layer, and integration tests that talk to the real server over sockets

## Quick start

Requires **Java 16 or newer**.

Compile (from the folder that contains the sources):

```
javac -d out Command.java CommandSpec.java CommandException.java CommandParser.java StorageEngine.java CommandExecutor.java LineTooLongException.java Server.java Client.java Repl.java
```

Start the server (it listens on port 6380 by default, to avoid colliding with a real Redis):

```
java -cp out Server
```

In another terminal, start the console client:

```
java -cp out Client
```

In IntelliJ, just run `Server.main` and then `Client.main`. Any TCP client also works, for example `telnet localhost 6380` or `ncat localhost 6380`.

### Example session

```
Connected to localhost:6380. Type 'quit' to leave.
> SET name Ilai Amrami
OK
> GET name
Ilai Amrami
> GET Name
(nil)
> DEL name
1
> DEL name
0
> GET
-ERR wrong number of arguments for 'get' command
> PING
-ERR unknown command 'PING'
> quit
```

## Commands

| Command | Reply | Notes |
|---|---|---|
| `SET key value` | `OK` | Overwrites an existing value. The value is the rest of the line, so it may contain spaces. |
| `GET key` | the value, or `(nil)` | `(nil)` means the key does not exist. |
| `DEL key` | `1` or `0` | `1` if the key existed and was deleted, `0` if it did not exist. |

- Command names are case-insensitive (`set` is the same as `SET`). Keys and values are case-sensitive.
- A key is a single word without spaces.

## Protocol

- One command per line. A line ends with `\n`; `\r\n` is accepted too.
- One reply line per command, ending with `\r\n`. Everything is UTF-8.
- A line that never ends (the connection drops in the middle of a command) is discarded, never executed.
- Errors start with `-ERR`:

| Reply | When |
|---|---|
| `-ERR unknown command 'X'` | The command name is not `GET`, `SET` or `DEL`. |
| `-ERR wrong number of arguments for 'get' command` | Wrong number of arguments (same for `set` and `del`). |
| `-ERR empty command` | A blank line. |
| `-ERR invalid character in command` | A carriage return or line feed inside the command. |
| `-ERR input too big` | The line is longer than 1 MiB when encoded as UTF-8. |
| `-ERR Line exceeded maximum allowed length of 1048576 characters` | The line is over the limit. The server sends this and closes the connection. |
| `-ERR internal error` | An unexpected bug in the server. It is also logged on the server. |

## Architecture

```
client --TCP--> Server --line--> CommandParser --Command--> CommandExecutor --> StorageEngine
                (thread per      (validates, using          (runs the           (ConcurrentHashMap)
                 client)          the CommandSpec table)     command)
```

| Class | Responsibility |
|---|---|
| `Server` | Accepts connections, runs each client on its own thread, reads lines with a length limit and writes the replies. |
| `CommandParser` | Turns a raw line into a `Command` and validates it against the table of supported commands. Throws `CommandException` for bad input. |
| `CommandSpec` | Describes one command: its name, how many arguments it takes, and whether the last argument takes the rest of the line (used by `SET` for its value). |
| `Command` | A parsed command: an upper-case name and a list of arguments. |
| `CommandExecutor` | Runs a `Command` against the storage and returns the reply. |
| `StorageEngine` | The data itself, in a `ConcurrentHashMap`, so clients can use it at the same time. |
| `LineTooLongException` | Thrown by the server when a client sends a line over the limit. |
| `Client` | A small console client for trying the server. |
| `Repl` | Runs parser, executor and storage in one process, without a network. Handy for quick checks. |

### Design notes

- **The parser only parses.** It knows the shape of each command, but not what the command does. The executor trusts that every `Command` it receives is valid.
- **Table-driven commands.** Adding a command means: add a line to the `COMMANDS` table in `CommandParser`, add a `case` in `CommandExecutor.execute`, and add tests.
- **Length limit while reading.** `BufferedReader.readLine()` would buffer an unlimited amount of data, so the server reads character by character and stops as soon as a line passes the limit (`CommandParser.MAX_INPUT_BYTES`, 1 MiB).
- **Testable server.** `Server` takes a port (0 means "any free port") and the maximum line length in its constructor, and has `start()` and `close()`. This lets tests start a fresh server on a free port.

## Tests

Tests use JUnit 5:

- `CommandParserTest`, `StorageEngineTest`, `CommandExecutorTest`: unit tests per layer. `CommandExecutorTest` also runs lines through the real parser and storage.
- `ServerIntegrationTest`: starts a real server on a free port and talks to it over sockets. It covers the commands, errors that keep the connection open, shared storage between clients, 20 clients working at the same time, the line length limit (exactly at the limit and one character over), and lines that are never finished.

In IntelliJ: add JUnit 5 to the project libraries, then right-click the source folder and choose **Run All Tests**.

## Known limitations

- **Not the real Redis protocol (RESP).** `redis-cli` and Redis client libraries cannot talk to this server.
- **`(nil)` cannot be told apart from a value.** If someone stores the text `(nil)`, `GET` returns the same text as for a missing key.
- **Memory only.** Data is lost when the server stops. There is no persistence and no expiry (TTL).
- **Only three commands.** `DEL` takes a single key.
- **One thread per client**, with no limit on connections and no idle timeout.
- **No authentication and no encryption.**

## Ideas for future work

- Speak RESP, so that `redis-cli` works
- More commands (`EXISTS`, `INCR`, `MSET`, ...) and key expiry
- Persistence (snapshots or an append-only log)
- A non-blocking server based on NIO and a single event loop, like the real Redis
- Connection limits and idle timeouts
