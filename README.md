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

`MultiblockBlueprint` defines the structure, component offsets, and placement constraints of a multiblock. It utilizes a `PatternBuilder` to intuitively map characters to structural matrices.

### Component Choices & Structure Components
Structure components are strictly divided into `RequiredComponent` and `OptionalComponent` (sealed interface `StructureComponent`).

You bind these components using a `ComponentChoice`, which allows either a strict block match (`BlockDataChoice`) or an entire material group (`TagChoice`).

### Programmatic Blueprint Creation (`PatternBuilder`)

```java
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.RequiredComponent;
import com.github.darksoulq.mt_multiblocks.OptionalComponent;
import com.github.darksoulq.mt_multiblocks.choice.BlockDataChoice;
import com.github.darksoulq.mt_multiblocks.choice.TagChoice;
import com.github.darksoulq.mt_multiblocks.port.BasicItemPort;
import com.github.darksoulq.mt_multiblocks.port.BasicLiquidPort;
import com.github.darksoulq.mt_multiblocks.util.PatternBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;

// The PatternBuilder allows for intuitive layer-by-layer matrix assembly
MultiblockBlueprint boilerBlueprint = new PatternBuilder()
    .setCoreChar('C') // 'C' is the default core character representing (0,0,0)
    
    // Required components: Core structure fails/breaks if these are missing
    .addMapping('O', new RequiredComponent(new BlockDataChoice(Bukkit.createBlockData(Material.COPPER_BLOCK)), new BasicLiquidPort(true, false)))
    .addMapping('A', new RequiredComponent(new BlockDataChoice(Bukkit.createBlockData(Material.POLISHED_ANDESITE)), new BasicLiquidPort(false, true)))
    
    // Optional components: Functionality extensions (can be missing without breaking the core)
    // TagChoice allows ANY block in a given Tag (e.g., ANY log), with a default fallback BlockData for displays
    .addMapping('L', new OptionalComponent(new TagChoice(Tag.LOGS, Bukkit.createBlockData(Material.OAK_LOG)), null))
    
    // Add layers (bottom to top, arrays of strings)
    .addLayer(
        "LCL",
        " A ",
        " O "
    )
    .build();
```

### Placement Conditions API & ValidationResult

`PlacementCondition` is a functional interface used to validate placement before assembly. It uses `ValidationResult` to provide detailed failure feedback instead of raw booleans.

```java
import com.github.darksoulq.mt_multiblocks.util.ValidationResult;

@FunctionalInterface
public interface PlacementCondition {
    ValidationResult canPlace(Location coreLoc, BlockFace facing, Player player);
}
```

Registering placement conditions to a blueprint:

```java
blueprint.addPlacementCondition((coreLoc, facing, player) -> {
    if (coreLoc.getBlockY() <= 63) {
        return ValidationResult.success();
    }
    return ValidationResult.fail("Core must be placed at or below sea level!");
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
| `mt_multiblocks:redstone_port` | `BasicRedstonePort` | `can_receive`, `can_emit`, `emissionLevel` |

### Redstone Port Interoperability
The `RedstonePort` interface provides deep interoperability with vanilla redstone components, allowing multiblock components to actively push analogue power states to non-redstone proxy blocks.

```java
public interface RedstonePort extends Port {
    boolean canReceive();
    boolean canEmit();
    int getEmissionLevel(Device coreDevice); // Must be clamped 0-15
    void onRedstoneReceive(Device coreDevice, int powerLevel);
}
```
*Note: `BasicRedstonePort` manages the `emissionLevel` bounds automatically and safely sets the power of adjacent `AnaloguePowerable` block states directly from the proxy device.*

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
    block_data: "minecraft:copper_block" # BlockDataChoices take explicit block data
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
    required: true # Will parse as RequiredComponent
  b1:
    x: -1
    y: 0
    z: 0
    palette_key: "p2"
    required: false # Will parse as OptionalComponent
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

> You must manually set the ports, optional toggles, and Tag choices in the generated file.

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
| `MultiblockComponentAddEvent` | Yes | Fired when an optional component block is added post-assembly. |
| `MultiblockComponentRemoveEvent` | No | Fired when an optional component block is broken or removed. |
| `MultiblockExplodeEvent` | Yes | Fired during block/entity explosions affecting multiblock components. |

### Explosion Behavior & Handling
Explosions strictly differentiate between `RequiredComponent` and `OptionalComponent`:
* If an **Optional Component** is destroyed by an explosion, it breaks safely and triggers `MultiblockComponentRemoveEvent` (Core remains intact).
* If a **Required Component** is destroyed by an explosion, the **ENTIRE CORE** and multiblock undergo full safe disassembly.

```java
@EventHandler
public void onMultiblockExplode(MultiblockExplodeEvent event) {
    MultiblockExplodeEvent.Cause cause = event.getCause(); // BLOCK, ENTITY, PLUGIN, UNKNOWN
    Entity entity = event.getEntity();
    Block sourceBlock = event.getBlock();

    Set<Device> cores = event.getCoresToDisassemble();
    Set<PortProxyDevice> optionals = event.getOptionalProxiesHit();

    // Cancel disassembly/destruction of specific required components
    if (cause == MultiblockExplodeEvent.Cause.ENTITY) {
        event.setCancelled(true);
    }
}
```

---