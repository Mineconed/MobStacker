<p align="center"><img src="MobStacker_ikon_128.png" width="128"></p>
<h1 align="center">MobStacker</h1>
<p align="center">Stack mobs into one with a number and stop the lag.<br>Made by <b>Mineconed</b> • Minecraft 1.8 to 1.21</p>

## Features
- 📦 Stacks mobs into one with a **number above it**
- ⚔️ Kill one at a time with a sword
- 🏭 **Mob farm support**: fall, lava and other farm deaths kill the whole stack with all drops and XP
- 🧠 Smart: babies, slime sizes and sheep colors are not mixed
- 🐕 Tamed, leashed and named mobs are never stacked
- ⏱️ Mobs that walk together are also stacked
- 📊 **/ms stats** shows how many mobs you saved
- 🚫 Blacklist for mobs like villagers and bosses
- 💾 Stacks stay after a restart
- 🌐 English and Danish built in

## Setup
1. Put **MobStacker.jar** in your plugins folder
2. Start the server. Done!
3. (Optional) change max stack size and blacklist in the config and run **/ms reload**

## Commands
| Command | What it does |
|---|---|
| `/ms stats` | how many stacks and mobs |
| `/ms stackall` | stack all mobs close to each other now |
| `/ms reload` | reload |

## Permissions
| Permission | What it does |
|---|---|
| `mobstacker.admin` | use the commands (op) |

## Config
| Setting | What it does |
|---|---|
| `name_format` | name above a stack, {amount} and {type} |
| `always_show_name` | show the name without looking at the mob |
| `max_stack_size` | max mobs in one stack |
| `radius` | how far mobs look for a stack |
| `spawn_reasons` | which spawns are stacked |
| `merge_task_seconds` | stack mobs that walk together. 0 = off |
| `kill_whole_stack_causes` | deaths that kill the whole stack |
| `blacklist` | mobs that are never stacked |
| `disabled_worlds` | worlds without stacking |

## Versions
1.8 to 1.21.

## Build
You need Java 8 or newer and Maven.
```
mvn package
```
The plugin is in `target/MobStacker.jar`.

## License
MIT. Made by Mineconed.
