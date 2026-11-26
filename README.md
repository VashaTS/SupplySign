# SupplySign

SupplySign is a Paper/Bukkit plugin that lets admins place `[Supply]` signs that give players unlimited items or kits. Right-click the sign (or the chest it is attached to) to open a chest-like window full of the listed items, and take what you need. This repository is a maintained fork of [redsgreens/SupplySign][1], ported to modern Paper. The original plugin was last updated in 2021 (Minecraft 1.12).

[1]: https://github.com/redsgreens/SupplySign

# Supported versions

* **Paper 26.2 and 26.3**, and newer Paper builds as long as the sign, chest and `BlockData` APIs stay compatible
* Java 25 (the version Paper 26.x requires)
* Server only; players do not need a client mod

# Links

* [Downloads](https://github.com/VashaTS/SupplySign/releases)
* [Original repository](https://github.com/redsgreens/SupplySign)
* [Original BukkitDev page](http://dev.bukkit.org/server-mods/supplysign/)

# Usage

1. Place a sign.
2. Set the first line to `[Supply]` (it turns dark blue when accepted).
3. On each of the remaining lines put the name of an item, e.g. `diamond`, `steak`, `torch`. Item names from the old `items.csv` still work, and so does any Minecraft item name (`golden_apple`, `minecraft:diamond_pickaxe`, `goldenapple`). Old numeric ids such as `35:14` still work too.
4. Right-click the sign to open the window.

**Chest sign:** place the sign on the ground next to a chest (not within one block of another chest). After you finish editing, the sign attaches itself to the chest as a wall sign. Right-clicking the sign or the chest shows the contents of the sign. On a large chest you can put a sign on both halves. You can also place a wall sign on the chest yourself.

**Dispenser:** attach a sign to a dispenser the same way. The dispenser is always refilled with the items on the sign (one slot per item, up to nine). Dispensers need redstone to fire, so wire it up and test it before attaching the sign. A dispenser with a supply sign cannot be opened.

**Kits:** set the second line to `kit:` followed by the kit name, e.g. `kit:wool`. Included kits: wool, dye, woodtools, woodplus, irontools, ironplus, diamondtool, diamondplus, goldtools, goldplus, desert, makecake, chainmail, stonebricks, bigredmush, cookedmeat. Add your own in `plugins/SupplySign/kits.yml`.

**Signs have two sides:** a sign works if either side has `[Supply]` on its first line, and the items are read from that side.

# Commands and permissions

| Command | Permission | What it does |
| --- | --- | --- |
| `/supplysign reload` | `supplysign.reload` | Reloads config, items and kits |
| `/supplysign listkits` | none | Lists available kits |

| Permission | What it allows | Default |
| --- | --- | --- |
| `supplysign.create` | Place supply signs | op |
| `supplysign.access` | Use any supply sign or kit | op |
| `supplysign.access.<kit>` | Use only the kit named `<kit>` | none |
| `supplysign.destroy` | Break supply signs and the chests/dispensers they are attached to | op |
| `supplysign.reload` | Use `/supplysign reload` | op |
| `supplysign.*` | All of the above | op |

# Configuration

`plugins/SupplySign/config.yml`:

* `ShowErrorsInClient` (Yes/No): show permission and other errors to players, not only in the console
* `AllowNonOpAccess` (Yes/No): let players without the access permission use signs
* `FixSignOnSignGlitch` (SupplySignOnly / Global / Disabled): remove a sign that is placed on a supply sign

Data files in `plugins/SupplySign/`:

* `items-custom.csv` - your own item names: `alias,MATERIAL[,amount[,potion-type]]`, e.g. `stackofdiamonds,DIAMOND,64`. The old `alias,id,damage,amount` format is still read.
* `kits.yml` - kits, each a list of item names
* `items-base.csv` - a reference copy of the built-in aliases; it is rewritten on every start, do not edit it

# Changes in 2.0.0 (Paper 26.x port)

The original plugin targeted Bukkit 1.12 with numeric item ids. 2.0.0 keeps the behaviour and replaces everything that no longer exists on Paper 26.x:

* **Signs**: all wood types are supported (the old `SIGN_POST` / `WALL_SIGN` materials are gone). Sign text uses Adventure components and the two-sided `SignSide` API instead of `getLine`/`setLine`. The `[Supply]` header is dark blue text. Wall signs are rotated with `BlockData` instead of the removed `setData`.
* **Items**: the numeric-id tables (`items.csv`) were converted to modern `Material` names, using Minecraft's own legacy-to-modern mapping. Legacy ids on signs and in `items-custom.csv` are translated through a bundled table. Every item of the running Minecraft version can be written on a sign by name. Potion entries use `PotionType`.
* **Chests**: double chests are detected from the chest `BlockData` (left/right half) instead of by looking for adjacent chests. A sign only counts for a chest it is actually attached to.
* **Window**: the supply window is a 36-slot chest window (the old `InventoryType.PLAYER` window is not available).
* **Config, kits**: read with Bukkit's `YamlConfiguration` instead of a bundled SnakeYAML; permissions are declared in `plugin.yml`; messages use Adventure; the console can use `/supplysign`.
* **Fixes**: names are matched case-insensitively, `id.damage` and `kit: name` (with a space) work, the second interact event for the off hand is ignored, a sign removed by the sign-on-sign fix is returned to the player instead of replacing the whole stack in their hand, and signs that cannot be placed are cancelled properly.
* Dropped: the migration of a pre-1.9 `items.csv` in the data folder, and the `debugees` code that was never used.

# Building

You need **JDK 25** and **Maven 3.9+**.

```bash
git clone https://github.com/VashaTS/SupplySign.git
cd SupplySign
mvn -DskipTests package
```

The plugin jar is written to `target/SupplySign-2.0.0.jar`.

# Credits and license

SupplySign is released under the [GNU General Public License v3.0](LICENSE.txt), the same license as the original.

* Original plugin: **[redsgreens](https://github.com/redsgreens)** ([redsgreens/SupplySign](https://github.com/redsgreens/SupplySign)). The original was based heavily on the `[Free]` sign code from Essentials by Zenexer, and thanks go to vitaminmoo for testing it and developing the `kits.yml` format.
* Paper 26.x port: [VashaTS](https://github.com/VashaTS/SupplySign)
