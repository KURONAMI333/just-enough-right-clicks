package com.kuronami.justenoughrightclicks.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Builds the list of block transformations a tool right-click can produce.
 *
 * <p>Every row comes from the game itself: {@code BlockState#getToolModifiedState} runs the
 * NeoForge tool-modification event and then the vanilla default for the same ability, and
 * {@code HoneycombItem#getWaxed} supplies waxing. No block or item is listed by hand.
 *
 * <p>Rows where the block does not change are dropped, because a state-only change (a campfire
 * going out, a candle lighting) cannot show a different icon and would only confuse the list.
 * Blocks without an item form are dropped for the same reason: JEI cannot render them.
 */
public final class RightClickIndex {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String VERIFY_PROPERTY = "justenoughrightclicks.verify";
    public static final String DUMP_FILE = "justenoughrightclicks-dump.txt";

    private static final String ACTION_WAX = "honeycomb_wax";

    private record Probe(String action, ItemAbility ability) {
    }

    private static final List<Probe> PROBES = List.of(
            new Probe("axe_strip", ItemAbilities.AXE_STRIP),
            new Probe("axe_scrape", ItemAbilities.AXE_SCRAPE),
            new Probe("axe_wax_off", ItemAbilities.AXE_WAX_OFF),
            new Probe("shovel_flatten", ItemAbilities.SHOVEL_FLATTEN),
            new Probe("shovel_douse", ItemAbilities.SHOVEL_DOUSE),
            new Probe("hoe_till", ItemAbilities.HOE_TILL),
            new Probe("shears_trim", ItemAbilities.SHEARS_TRIM),
            new Probe("shears_carve", ItemAbilities.SHEARS_CARVE),
            new Probe("firestarter_light", ItemAbilities.FIRESTARTER_LIGHT));

    /** One JEI row: source block, every item that can perform the action, and the result. */
    public record Entry(ItemStack from, List<ItemStack> held, ItemStack to, String action) {
    }

    public record ScanResult(List<Entry> entries, int blocks, int probes, int errors) {
    }

    private static final class Row {
        private final Block from;
        private final Block to;
        private final Map<ResourceLocation, ItemStack> held = new LinkedHashMap<>();
        private final List<String> actions = new ArrayList<>();

        private Row(Block from, Block to) {
            this.from = from;
            this.to = to;
        }
    }

    private RightClickIndex() {
    }

    /**
     * Scans every registered block. {@code probePos} must be a position whose block above is air;
     * it is only read by handlers that look at the world, never modified.
     */
    public static ScanResult scan(Level level, BlockPos probePos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(probePos), Direction.UP, probePos, false);

        Map<ItemAbility, List<ItemStack>> tools = new LinkedHashMap<>();
        for (Probe probe : PROBES) {
            tools.put(probe.ability(), collectItems(probe.ability()));
        }

        Map<String, Row> rows = new LinkedHashMap<>();
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            blocks.add(block);
        }

        int errors = 0;
        for (Block block : blocks) {
            if (block.asItem() == Items.AIR) {
                continue;
            }
            BlockState state = block.defaultBlockState();
            String blockId = id(block);

            for (Probe probe : PROBES) {
                List<ItemStack> held = tools.get(probe.ability());
                if (held.isEmpty()) {
                    continue;
                }
                try {
                    UseOnContext context = new UseOnContext(level, null, InteractionHand.MAIN_HAND,
                            held.get(0).copy(), hit);
                    BlockState result = state.getToolModifiedState(context, probe.ability(), true);
                    accept(rows, block, result, held, probe.action());
                } catch (Throwable t) {
                    errors++;
                    LOGGER.warn("[justenoughrightclicks] skipped {} / {}: {}", blockId, probe.action(), t.toString());
                }
            }

            try {
                Optional<BlockState> waxed = HoneycombItem.getWaxed(state);
                if (waxed.isPresent()) {
                    accept(rows, block, waxed.get(), List.of(new ItemStack(Items.HONEYCOMB)), ACTION_WAX);
                }
            } catch (Throwable t) {
                errors++;
                LOGGER.warn("[justenoughrightclicks] skipped {} / {}: {}", blockId, ACTION_WAX, t.toString());
            }
        }

        List<Entry> entries = new ArrayList<>(rows.size());
        for (Row row : rows.values()) {
            List<ItemStack> held = new ArrayList<>(row.held.values());
            held.sort(Comparator.comparing(stack -> id(stack)));
            entries.add(new Entry(new ItemStack(row.from.asItem()), List.copyOf(held),
                    new ItemStack(row.to.asItem()), String.join("+", row.actions)));
        }
        entries.sort(Comparator.comparing((Entry entry) -> id(entry.from()))
                .thenComparing(entry -> id(entry.to())));

        return new ScanResult(List.copyOf(entries), blocks.size(), PROBES.size() + 1, errors);
    }

    private static void accept(Map<String, Row> rows, Block from, BlockState result, List<ItemStack> held,
            String action) {
        if (result == null) {
            return;
        }
        Block to = result.getBlock();
        if (to == from || to.asItem() == Items.AIR) {
            return;
        }
        Row row = rows.computeIfAbsent(id(from) + "|" + id(to), key -> new Row(from, to));
        if (!row.actions.contains(action)) {
            row.actions.add(action);
        }
        for (ItemStack stack : held) {
            row.held.putIfAbsent(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.copy());
        }
    }

    private static List<ItemStack> collectItems(ItemAbility ability) {
        List<ItemStack> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (stack.canPerformAction(ability)) {
                items.add(stack);
            }
        }
        items.sort(Comparator.comparing(RightClickIndex::id));
        return List.copyOf(items);
    }

    public static void writeDump(ScanResult result, Path path) {
        List<String> lines = new ArrayList<>();
        lines.add("# justenoughrightclicks blocks=" + result.blocks() + " probes=" + result.probes()
                + " entries=" + result.entries().size() + " errors=" + result.errors());
        lines.add("# source_block | held_items | result_block | actions");
        for (Entry entry : result.entries()) {
            StringBuilder held = new StringBuilder();
            for (ItemStack stack : entry.held()) {
                if (held.length() > 0) {
                    held.append(',');
                }
                held.append(id(stack));
            }
            lines.add(id(entry.from()) + " | " + held + " | " + id(entry.to()) + " | "
                    + entry.action());
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
            LOGGER.info("[justenoughrightclicks] wrote {}", path);
        } catch (IOException e) {
            LOGGER.warn("[justenoughrightclicks] could not write {}: {}", path, e.toString());
        }
    }

    public static String id(ItemStack stack) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static String id(Block block) {
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
    }
}
