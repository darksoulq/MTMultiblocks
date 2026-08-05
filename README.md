# MTMultiblocks Documentation

## 1. Dependency Setup (JitPack)

```groovy
repositories {
    mavenCentral()
    maven { url '[https://jitpack.io](https://jitpack.io)' }
}

dependencies {
    implementation 'com.github.darksoulq:mt_multiblocks:Tag'
}
```

---

## 2. Multiblock Blueprint API

`MultiblockBlueprint` defines the structure, component offsets, and placement constraints of a multiblock.

### Programmatic Blueprint Creation

```java
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.port.BasicItemPort;
import com.github.darksoulq.mt_multiblocks.port.BasicLiquidPort;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.util.Vector;

MultiblockBlueprint blueprint = new MultiblockBlueprint();

// Required components: core structure fails if these are missing/broken
blueprint.addComponent(
    new Vector(0, 1, 0),
    Bukkit.createBlockData(Material.PISTON),
    new BasicItemPort(true, false)
);

// Optional components: extend functionality without breaking core structure if destroyed
blueprint.addOptionalComponent(
    new Vector(0, -1, 0),
    Bukkit.createBlockData(Material.COPPER_BLOCK),
    new BasicLiquidPort(true, true, null)
);
```

### Placement Conditions API

`PlacementCondition` is a functional interface used to validate placement before assembly:

```java
@FunctionalInterface
public interface PlacementCondition {
    boolean canPlace(Location coreLoc, BlockFace facing, Player player);
}
```

Registering placement conditions to a blueprint:

```java
blueprint.addPlacementCondition((coreLoc, facing, player) -> {
    // Example: Core must be placed at or below sea level
    return coreLoc.getBlockY() <= 63;
});
```

---

## 3. Port System & Port Registry

Ports define functionality mapped to secondary blocks (items, liquids, redstone, interactions).

### Built-in Ports

| NamespacedKey | Class | Key Configuration Options |
| :--- | :--- | :--- |
| `mt_multiblocks:item_port` | `BasicItemPort` | `is_input`, `is_output`, `slots` |
| `mt_multiblocks:liquid_port` | `BasicLiquidPort` | `is_input`, `is_output`, `liquids` |
| `mt_multiblocks:structural_port` | `BasicStructuralPort` | None |
| `mt_multiblocks:interact_port` | `BasicInteractPort` | None |
| `mt_multiblocks:redstone_port` | `BasicRedstonePort` | `can_receive`, `can_emit` |

### Custom Port Registration

To define custom ports, implement `Port` and register a `PortFactory` via `PortRegistry`:

```java
import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.registry.PortRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

public class EnergyPort implements Port {
    private final int maxTransfer;

    public EnergyPort(int maxTransfer) {
        this.maxTransfer = maxTransfer;
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }
}

// Registration in plugin initialization
NamespacedKey key = new NamespacedKey(plugin, "energy_port");
PortRegistry.register(key, (ConfigurationSection config) -> {
    int maxTransfer = config.getInt("max_transfer", 1000);
    return new EnergyPort(maxTransfer);
});
```

---

## 4. Proxy Devices & Custom Proxy Factories

When secondary multiblock components are placed in the world, they are bound to a proxy device (`PortProxyDevice`) extending Minetorio's `Device`.

### Registering a Custom Proxy Factory

To override the default `PortProxyDevice` instantiation for specific port types:

```java
import com.github.darksoulq.mt_multiblocks.core.MultiblockRegistry;
import com.github.darksoulq.mt_multiblocks.core.ProxyFactory;

MultiblockRegistry.instance.registerProxyFactory(EnergyPort.class, (location, coreDevice, component) -> {
    return new CustomEnergyProxyDevice(location, coreDevice, component);
});
```

---

## 5. Blueprint YAML & Palette System

Blueprints can be saved and loaded dynamically using a key-based palette system.

### YAML Schema (`snake_case`)

```yaml
palette:
  p1:
    block_data: "minecraft:copper_block"
    port:
      type: "mt_multiblocks:liquid_port"
      is_input: true
      is_output: false
      liquids:
        - WATER
        - STEAM
  p2:
    block_data: "minecraft:hopper"
    port:
      type: "mt_multiblocks:item_port"
      is_input: false
      is_output: true

blocks:
  b0:
    x: 1
    y: 0
    z: 0
    palette_key: "p1"
    required: true
  b1:
    x: -1
    y: 0
    z: 0
    palette_key: "p2"
    required: false
```

### Loading Blueprint Instances

Use `BlueprintLoader` to convert YAML configurations into `MultiblockBlueprint` instances:

```java
import com.github.darksoulq.mt_multiblocks.config.BlueprintLoader;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import org.bukkit.configuration.file.YamlConfiguration;

File file = new File(getDataFolder(), "blueprints/boiler.yml");
MultiblockBlueprint blueprintFromFile = BlueprintLoader.loadBlueprint(file);

// Or load from a ConfigurationSection directly:
YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
MultiblockBlueprint blueprintFromConfig = BlueprintLoader.loadBlueprint(config);
```

---

## 6. In-Game Selection & Creation Command

Blueprints can be generated in-game via Paper Brigadier commands.

### Workflow
1. Mark two corners of the target multiblock area using `/mtb pos1` and `/mtb pos2`.
2. Place exactly **one Bedrock block** within the selected volume to designate the relative core position (`0, 0, 0`).
3. Run `/mtb save <filename>`.

### Error Handling
* If **0 Bedrock blocks** exist in the selection, saving fails.
* If **>1 Bedrock blocks** exist in the selection, saving fails.
* The Bedrock block is automatically excluded from the generated YAML components and used solely as the `0,0,0` offset pivot.

> You must manually set the ports in the file

---

## 7. External Multiblock Registration

Register blueprints to non-`MTMultiblock` `Device` instances using device names:

```java
import com.github.darksoulq.mt_multiblocks.ExternalMultiblocks;

ExternalMultiblocks.registerBlueprint("Crusher", crusherBlueprint);
boolean exists = ExternalMultiblocks.hasBlueprint("Crusher");
MultiblockBlueprint retrieved = ExternalMultiblocks.getBlueprint("Crusher");
```

---

## 8. Event System API

All events reside in `com.github.darksoulq.mt_multiblocks.events`.

### Event Overview

| Event | Cancellable | Description |
| :--- | :--- | :--- |
| `MultiblockAssembleEvent` | Yes | Fired when a core device attempts to form its multiblock structure. |
| `MultiblockDisassembleEvent` | No | Fired when a core device or required proxy is broken/disassembled. |
| `MultiblockComponentAddEvent` | Yes | Fired when an optional or secondary component block is added post-assembly. |
| `MultiblockComponentRemoveEvent` | No | Fired when an optional component block is broken or removed. |
| `MultiblockExplodeEvent` | Yes | Fired during block/entity explosions affecting multiblock components. |

### `MultiblockExplodeEvent` Details

Provides contextual metadata detailing explosion origin and targeted components:

```java
@EventHandler
public void onMultiblockExplode(MultiblockExplodeEvent event) {
    MultiblockExplodeEvent.Cause cause = event.getCause(); // BLOCK, ENTITY, PLUGIN, UNKNOWN
    Entity entity = event.getEntity();
    Block sourceBlock = event.getBlock();

    Set<Device> cores = event.getCoresToDisassemble();
    Set<PortProxyDevice> optionals = event.getOptionalProxiesHit();
    Set<Block> requiredBlocks = event.getRequiredBlocksHit();

    // Cancel disassembly/destruction of specific required components
    if (cause == MultiblockExplodeEvent.Cause.ENTITY) {
        event.setCancelled(true);
    }
}
```

---

## 9. Behaviors API

`Behaviors` (`com.github.darksoulq.mt_multiblocks.core.Behaviors`) exposes certain utility methods to interact with multiblocks:

```java
// Handles structural validation failure for core devices
Behaviors.handleValidationFailure(coreDevice);

// Handles structural validation failure for optional proxies
Behaviors.handleOptionalValidationFailure(proxyDevice);

// Manually disassemble a multiblock structure
Behaviors.disassembleMultiblock(coreDevice, blueprint, player);

// Process explosion impact on blocks
Behaviors.handleExplosion(blockList, MultiblockExplodeEvent.Cause.PLUGIN, null, null);

// Check if a block is part of an active multiblock core/proxy
boolean protectedBlock = Behaviors.isProtectedBlock(block);
```

---