# Illegal Placement

A Minecraft 1.21 - 1.21.11 mod for Fabric, Forge and NeoForge: place blocks in states vanilla won't let you
place, keep them that way, and edit any block state from a panel. The server needs the mod; in singleplayer the
client is enough.

## Why

When you build a lot, vanilla gets in the way all the time. Half a bed, a fence that connects to nothing, a lever
hanging in the air - the game either refuses to place it, or "fixes" it the moment something next to it changes.
Axiom can do some of this, but it never felt handy for this kind of detail work. The debug stick and `/setblock`
get there too, but they're slow, and the commands take a while to learn. So this mod puts it on a key: press B,
aim, click.

## Requirements

Fabric needs Fabric Loader 0.19.5 or newer and Fabric API. For Forge and NeoForge, the table lists the oldest
build that works with each Minecraft version; newer builds of the same line work as well.

| Minecraft | Forge | NeoForge |
|---|---|---|
| 1.21 | 51.0.5 | 21.0.143 |
| 1.21.1 | 52.0.0 | 21.1.1 |
| 1.21.2 | - | 21.2.0-beta |
| 1.21.3 | 53.1.2 | 21.3.56 |
| 1.21.4 | 54.1.5 | 21.4.121 |
| 1.21.5 | 55.0.24 | 21.5.74 |
| 1.21.6 | 56.0.0 | 21.6.16-beta |
| 1.21.7 | 57.0.0 | 21.7.1-beta |
| 1.21.8 | 58.0.10 | 21.8.9 |
| 1.21.9 | 59.0.0 | 21.9.0-beta |
| 1.21.10 | 60.0.19 | 21.10.63 |
| 1.21.11 | 61.0.0 | 21.11.42 |

## Usage

| Key (rebindable) | Action |
|---|---|
| `B` | Toggle build mode |
| Right click | Place the held block at the target without validity checks (mid-air lever, flower on stone, half bed) |
| Left click | Remove the target block without neighbor updates or drops |
| Mouse wheel | Move the target nearer / farther, also through empty space (Shift = fine steps) |
| Middle click | Creative: pick the target block with its exact state (only stacks with the same state) |
| `G` | Panel for the aimed block or entity; aimed at empty space: Build Settings |
| `H` | Placement guide on/off (the faint boxes around the target) |

Most of the editing happens in the panel. For a block it lists every block state property; left click selects
the next value and right click the previous one (extending a piston also places its head). Below the properties
are two switches. **Lock state** makes the block ignore shape updates, neighbor updates and survival checks,
**Freeze time** makes it ignore random and scheduled ticks. For an entity that isn't a mob, the panel shows NBT
flags instead (`Fixed` for item frames and paintings, `Invisible`, `ShowArms`, `Facing`, ...). Pressing the key
while aiming at empty space opens Build Settings, with auto-lock and auto-freeze on placement, the overlays and
the default reach.

In build mode the outline of the aimed block also shows what the next block update would do to it. Orange means
it would change, and the status text says how: it would drop, be reshaped, or be switched by redstone
(quasi-connectivity included). Green means it's locked. If an orange block should stay the way it is, lock it in
the panel.

Locks are saved per dimension in `data/illegalplacement_locks.dat`. When a different block replaces a locked one,
the lock is removed.

## Commands

You don't need any of these for normal building. They require the same permission level as build mode.

| Command | |
|---|---|
| `/ipsetblock`, `/ipfill` | Same syntax as `/setblock` / `/fill`, but states go in exactly as given (see Litematica below) |
| `/iplock <from> <to>` | Locks the state of every block in the area; put `freeze` at the end to freeze time too |
| `/ipunlock <from> <to>` | Removes locks and freezes in the area |

## Litematica

Illegal states are plain vanilla block states, so schematics keep them (locks are not saved in a schematic).
Pasting in singleplayer leaves them untouched. On a server it's different: Litematica pastes through `/setblock`
and `/fill` there, which update the neighbors of every block they place - and before 1.21.4 they also recalculate
the placed block's own connections. Fences, walls, panes, stairs, redstone wire and unsupported blocks get "fixed"
while the paste is still arriving.

If the server has this mod, change these two options in Litematica's Generic settings:

- `commandNameSetblock` = `ipsetblock`
- `commandNameFill` = `ipfill`

The syntax is the same, but states are placed as given. Illegal blocks are locked as they arrive, and blocks that
would fall are frozen. Commands can't carry scheduled ticks, though, so a pressed button or a tripped tripwire in
the schematic would stay on forever. To avoid that, once nothing has been pasted for a second, buttons, pressure
plates, tripwires, detector rails, observers, target blocks, lightning rods and sculk sensors get their tick back
and switch off, the same as after a singleplayer paste. Lamps, torches and repeaters keep the pasted state.

## Config

Server settings are in `config/illegalplacement-server.properties`, which is created on first start:

| Key | Default | |
|---|---|---|
| `requiredPermissionLevel` | 2 | Permission level for build mode, panels and commands |
| `requireCreative` | true | Creative players only |
| `allowSingleplayerOwner` | true | The singleplayer/LAN host can always use it |
| `maxReach` | 64 | Max distance to an edited block |
| `registerPasteCommands` | true | Register `/ipsetblock` and `/ipfill` |
| `maxFillVolume` | 32768 | Max volume of one `/ipfill`, `/iplock` or `/ipunlock` |
| `autoLockIllegalOnCommandPaste` | true | Lock illegal blocks placed by the paste commands |

Client settings are stored in `config/illegalplacement-client.properties`. All of them can also be changed in game
under Build Settings.

## Known limitations

- Freeze time doesn't stop block entities from ticking (furnaces, hoppers).
- Display entities and markers have no hitbox, so the entity panel can't target them.
- Forge 1.21.3 - 1.21.7 and Fabric 1.21.9 have no world render event, so the build mode boxes are drawn on the HUD
  there and show through blocks.
- Since 1.21.6, `G` is also vanilla's Quick Actions key. The controls screen flags the conflict, but both work.
- OptiFine HD U J1 is made for Forge 52.0.x. Together with the mod it has only been tried on Forge 52.1.x, where
  the build mode status text was missing and removing containers crashed the server.

## Building

Building requires JDK 21. Every build targets one Minecraft version: the files in `versions/` list the supported
versions and the dependencies for each, and the Forge and NeoForge builds in them are the minimums from the table
above (there is no Forge for 1.21.2).

```
./gradlew build                      # 1.21.1, jars in <loader>/build/libs
./gradlew build -Pmc=1.21.4          # Any version from versions/
./gradlew :fabric:runClient -Pmc=1.21.4      # Also :forge: / :neoforge:, and runServer
```

The project uses the MultiLoader layout. The mod itself is in `common`, while `fabric`, `forge` and `neoforge`
contain the loader-specific glue. Code that differs between Minecraft versions lives next to `src/main` in folders
named after the versions they cover (for example `src/1.21.6-1.21.11`), and a build includes every folder whose
range contains its version.

## License

MIT © RagamuffinCat, see [LICENSE](LICENSE).
