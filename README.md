# MTMultiblocks Documentation

MTMultiblocks is an extension for the Minetorio framework, providing a robust API for defining, validating, and managing multiblocks. It handles component mapping, rotational states, port proxies, 3D display animations, YAML blueprint serialization, and offers a safe, isolated `AddonDevice` registration and saving system.

## 1. Dependency Setup

Add the following to your `build.gradle`:

```groovy
dependencies {
    implementation 'io.github.darksoulq:mt_multiblocks:Tag'
}
```

---

## 2. Addon Devices & Registration

- **AddonDevice:** The base class for all custom devices. Use this instead of `Device` to isolate your plugin's save data. This ensures that if your plugin is ever removed, it does not wipe Minetorio's main device data file.
- **MultiblockDevice:** The base class for multiblock cores. It extends `AddonDevice` and implements `MTMultiblock`.

### Extending AddonDevice

Use this for custom devices.

```java
public class SimpleGenerator extends AddonDevice {
    public static final String CLASS_NAME = "Simple Generator";

    public SimpleGenerator(Location loc) {
        super(loc);
        this.deviceName = CLASS_NAME;
        this.setMaterial("IRON_BLOCK");
        this.setUseUI(true);
    }

    @Override
    public void action() {
        // Device tick logic here
    }

    @Override
    public void updateUI() {
        // UI update logic here
    }
}
```

### Extending MultiblockDevice

```java
public class AdvancedCrusher extends MultiblockDevice {
    public static final String CLASS_NAME = "Advanced Crusher";

    public AdvancedCrusher(Location loc) {
        super(loc);
        this.deviceName = CLASS_NAME;
        this.setMaterial("FURNACE");
    }

    @Override
    public MultiblockBlueprint getBlueprint(BlueprintContext context) {
        MultiblockBlueprint blueprint = new MultiblockBlueprint();
        // Define multiblock structure here
        return blueprint;
    }

    @Override
    public void action() {
        // Device tick logic here
    }

    @Override
    public void updateUI() {}
}
```

Register your devices in your plugin's `onEnable` using `AddonDeviceRegistry` and `AddonDeviceTemplate`:

```java
// Instantiate the registry using your plugin instance
AddonDeviceRegistry registry = new AddonDeviceRegistry(this);

// Define your custom device
AddonDeviceTemplate crusher = new AddonDeviceTemplate(AdvancedCrusher.CLASS_NAME)
    .description("An advanced multiblock crusher.")
    .category(ItemMenu.Category.MANUFACTURING)
    .deviceClass(AdvancedCrusher.class)
    .prototype(() -> new AdvancedCrusher(null));

// Register it
registry.register(crusher);

// Apply the registry. This writes to devices.yml, registers to Minetorio's API,
// loads your addon's data file, and starts its save timer.
registry.apply();
```

---

## 3. Multiblock Blueprint API

A `MultiblockBlueprint` defines the structural layout, component offsets, and placement constraints of a multiblock.

Components are divided into two types:

- **Required Components:** The core structure. If broken, the entire multiblock disassembles.
- **Optional Components:** Functional extensions. They can be added or removed without breaking the core device.

### Manual Blueprint Assembly

You can build blueprints programmatically by defining vector offsets relative to the core `(0, 0, 0)`:

```java
MultiblockBlueprint blueprint = new MultiblockBlueprint();

blueprint.addComponent(
    new Vector(0, 1, 0), 
    Bukkit.createBlockData(Material.COPPER_BLOCK), 
    new BasicLiquidPort(true, false)
);

    blueprint.addOptionalComponent(
    new Vector(1, 0, 0), 
    List.of(Bukkit.createBlockData(Material.GLASS), Bukkit.createBlockData(Material.TINTED_GLASS)),
    new BasicItemPort(false, true)
);
```

### Using PatternBuilder

For complex structures, `PatternBuilder` allows for an intuitive, layer-by-layer matrix assembly.

```java
MultiblockBlueprint boilerBlueprint = new PatternBuilder()
    .setCoreChar('C')
    .addMapping('O', new RequiredComponent(Bukkit.createBlockData(Material.COPPER_BLOCK), new BasicLiquidPort(true, false)))
    .addMapping('L', new OptionalComponent(new TagChoice(Tag.LOGS, Bukkit.createBlockData(Material.OAK_LOG)), null))
    .addLayer(
        "LCL",
        " O ",
        " O "
    )
    .build();
```

---

## 4. Placement Conditions API

`PlacementCondition` is a functional interface used to validate the environment before assembly. It evaluates a `BlueprintContext` and returns a `ValidationResult` to provide detailed failure feedback.

```java
blueprint.addPlacementCondition(context -> {
    if (ContextConditions.isBelowY(context, 63)) {
    return ValidationResult.success();
    }
        return ValidationResult.fail("Core must be placed at or below sea level!");
});
```

The `BlueprintContext` object provides access to the placement context, including:

- `getCoreLocation()`
- `getFacing()`
- `getPlayer()`
- `getItemStack()`
- `getDevice()`

---

## 5. Ports & Port Registry

Ports define functionality mapped to secondary blocks (items, liquids, redstone, interactions).

### Built-in Ports

| NamespacedKey | Class | Key Configuration Options |
| --- | --- | --- |
| `mt_multiblocks:item_port` | `BasicItemPort` | `is_input`, `is_output`, `slots` |
| `mt_multiblocks:liquid_port` | `BasicLiquidPort` | `is_input`, `is_output`, `liquids` |
| `mt_multiblocks:structural_port` | `BasicStructuralPort` | None |
| `mt_multiblocks:interact_port` | `BasicInteractPort` | None |
| `mt_multiblocks:redstone_port` | `BasicRedstonePort` | `can_receive`, `can_emit`, `emissionLevel` |

### Custom Port Registration

To define custom ports, implement `Port` and register a `PortFactory` via `PortRegistry`:

```java
NamespacedKey key = new NamespacedKey(plugin, "energy_port");
PortRegistry.register(key, config -> {
int maxTransfer = config.getInt("max_transfer", 1000);
    return new EnergyPort(maxTransfer);
});
```

---

## 6. Proxy Devices

To override the default proxy instantiation for specific port types, register a custom `ProxyFactory`:

```java
MultiblockRegistry.instance.registerProxyFactory(EnergyPort.class, (location, coreDevice, component) -> {
    return new CustomEnergyProxyDevice(location, coreDevice, component);
});
```

---

## 7. 3D Models & Animations

MTMultiblocks supports rendering visual, animated 3D models using Minecraft's Display Entities (Block, Item, and Text displays) bound to the multiblock structure.

### Defining Models and Parts

Models are composed of `ModelPart` instances. Each part is identified by a unique string ID and can have default translations, scales, and rotations applied.

```java
MultiblockModel model = new MultiblockModel()
    .addPart(ModelPart.block("main_gear", Bukkit.createBlockData(Material.COPPER_BLOCK))
        .translate(0, 1.5, 0)
        .scale(0.5, 0.5, 0.5))
    .addPart(ModelPart.item("core_crystal", new ItemStack(Material.DIAMOND))
        .translate(0, 0.5, 0));

blueprint.setModel(model);
```

### Creating Animations with Keyframes

Animations are built using timelines of `Keyframe` objects assigned to specific part IDs. Keyframes define the target state (translation, rotation, scale) and the `Easing` transition over a specific duration in ticks.

```java
ModelAnimation spinAnimation = new ModelAnimation("spin")
    .addKeyframe("main_gear", new Keyframe(20)
        .rotateY(180)
        .easing(Easing.EASE_IN_OUT_QUAD))
    .addKeyframe("main_gear", new Keyframe(20)
        .rotateY(360)
        .easing(Easing.EASE_IN_OUT_QUAD));

model.addAnimation(spinAnimation);
```

### Controlling Animations In-Game

You can retrieve the `AnimationController` for any active multiblock device to play, loop, or stop animations.

```java
AnimationController animController = AnimationController.get(device);

if (animController != null && !animController.isPlaying()) {
MultiblockModel model = animController.getBlueprintModel();
ModelAnimation anim = model.getAnimation("spin");
    
    animController.play(anim, true);
}
```

To stop an animation and reset all parts to their default definitions:

```java
animController.stop();
```

---

## 8. Blueprint YAML Schema

Blueprints can be saved and loaded using a key-based palette system.

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
  p2:
    block_data:
      - "minecraft:glass"
      - "minecraft:tinted_glass"
    port:
      type: "mt_multiblocks:item_port"
      is_input: false
      is_output: true
  p3:
    tag: "minecraft:logs"
    default_block_data: "minecraft:oak_log"
    port:
      type: "mt_multiblocks:structural_port"

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

---

## 9. In-Game Commands

Blueprints can be generated in-game via commands.

### Multiblock YAML Generation
1. Obtain the selection tool using `/mtb selector` or mark two corners of the target multiblock area using `/mtb pos1` and `/mtb pos2`.
2. Place exactly **one Bedrock block** within the selected volume to designate the relative core position (`0, 0, 0`).
3. Run `/mtb save <filename>`.

**Notes:**
- The Bedrock block is automatically excluded from the generated YAML components.
- You must manually configure the ports, optional toggles, and Tag choices in the generated `.yml` file.

---

## 10. External Multiblock Registration

Register blueprints to non-`MultiblockDevice` `Device` instances using device names via `ExternalMultiblocks`:

```java
ExternalMultiblocks.registerBlueprint("Crusher", crusherBlueprint);

ExternalMultiblocks.registerBlueprint("DynamicMachine", context -> generateBlueprint(context));

boolean exists = ExternalMultiblocks.hasBlueprint("Crusher");
MultiblockBlueprint retrieved = ExternalMultiblocks.getBlueprint("Crusher", context);
```

---

## 11. Events

All events reside in `com.github.darksoulq.mt_multiblocks.events`.

| Event | Cancellable | Description |
| --- | --- | --- |
| `MultiblockAssembleEvent` | Yes | Fired when a core device attempts to form its structure. |
| `MultiblockDisassembleEvent` | No | Fired when a core device or required proxy is broken/disassembled. |
| `MultiblockComponentAddEvent` | Yes | Fired when an optional component block is added post-assembly. |
| `MultiblockComponentRemoveEvent` | No | Fired when an optional component block is broken or removed. |
| `MultiblockExplodeEvent` | Yes | Fired during block/entity explosions affecting multiblock components. |

### Explosion Behavior

- **Optional Components:** Destruction triggers `MultiblockComponentRemoveEvent`. The core remains intact.
- **Required Components:** Destruction breaks the **entire core** and disassembles the multiblock.