SupplySign is a Paper plugin that lets admins place `[Supply]` signs that give players unlimited items or kits. Right-click the sign (or the chest it is attached to) to open a chest-like window full of the listed items, and take what you need.

This is a maintained fork of [redsgreens/SupplySign](https://github.com/redsgreens/SupplySign), updated for Paper 26.2 and 26.3. The original plugin was last updated in 2021 (Minecraft 1.12). The fork is at [VashaTS/SupplySign](https://github.com/VashaTS/SupplySign).

## Signs

1. Place a sign.
2. Set the first line to `[Supply]` (it turns dark blue when accepted).
3. On each of the remaining lines put the name of an item, e.g. `diamond`, `steak`, `torch`. Item names from the old `items.csv` still work, and so does any Minecraft item name (`golden_apple`, `minecraft:diamond_pickaxe`, `goldenapple`). Old numeric ids such as `35:14` still work too.
4. Right-click the sign to open the window.

**Chest sign:** place the sign on the ground next to a chest (not within one block of another chest). After you finish editing, the sign attaches itself to the chest as a wall sign. Right-clicking the sign or the chest shows the contents of the sign. On a large chest you can put a sign on both halves.

**Dispenser:** attach a sign to a dispenser the same way. The dispenser is always refilled with the items on the sign (one slot per item, up to nine). Dispensers need redstone to fire. A dispenser with a supply sign cannot be opened.

**Kits:** set the second line to `kit:` followed by the kit name, e.g. `kit:wool`. Included kits: wool, dye, woodtools, woodplus, irontools, ironplus, diamondtool, diamondplus, goldtools, goldplus, desert, makecake, chainmail, stonebricks, bigredmush, cookedmeat. Add your own in `plugins/SupplySign/kits.yml`.

Signs have two sides: a sign works if either side has `[Supply]` on its first line, and the items are read from that side.

## Commands and permissions

- `/supplysign reload` (`supplysign.reload`) — reloads config, items and kits
- `/supplysign listkits` — lists available kits

- `supplysign.create` — place supply signs (default: op)
- `supplysign.access` — use any supply sign or kit (default: op)
- `supplysign.access.<kit>` — use only the kit named `<kit>`
- `supplysign.destroy` — break supply signs and the chests or dispensers they are attached to (default: op)
- `supplysign.reload` — use `/supplysign reload` (default: op)
- `supplysign.*` — all of the above (default: op)

## Configuration

`plugins/SupplySign/config.yml`:

- `ShowErrorsInClient` (Yes/No): show permission and other errors to players
- `AllowNonOpAccess` (Yes/No): let players without the access permission use signs
- `FixSignOnSignGlitch` (SupplySignOnly / Global / Disabled): remove a sign that is placed on a supply sign

Custom item names go in `plugins/SupplySign/items-custom.csv`. Place the jar in the server `plugins` folder and restart. It runs on the server only; players do not need a client mod.

## Compatibility

Paper **26.2** and **26.3**, and newer Paper builds as long as the sign and chest APIs stay compatible. Requires Java 25.

## Credits and license

- Original plugin: [redsgreens](https://github.com/redsgreens) ([redsgreens/SupplySign](https://github.com/redsgreens/SupplySign)). The original was based heavily on the `[Free]` sign code from Essentials by Zenexer, and thanks go to vitaminmoo for testing it and developing the `kits.yml` format.
- Maintained fork: [VashaTS/SupplySign](https://github.com/VashaTS/SupplySign)
- Issues: [github.com/VashaTS/SupplySign/issues](https://github.com/VashaTS/SupplySign/issues)
- License: [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html)
