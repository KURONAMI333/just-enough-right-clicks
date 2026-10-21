package com.kuronami.justenoughrightclicks;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.kuronami.justenoughrightclicks.index.RightClickIndex;
import com.kuronami.justenoughrightclicks.index.RightClickIndex.Entry;
import com.kuronami.justenoughrightclicks.index.RightClickIndex.ScanResult;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Just Enough Right-Clicks.
 *
 * <p>The visible feature lives entirely on the client: a JEI category built from
 * {@link RightClickIndex}. The mod still loads on a dedicated server so the same index can be
 * run there for verification; with the system property below unset it does nothing.
 */
@Mod(JustEnoughRightClicks.MODID)
public final class JustEnoughRightClicks {
    public static final String MODID = "justenoughrightclicks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JustEnoughRightClicks(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        }
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(RightClickIndex.VERIFY_PROPERTY)) {
            return;
        }

        ServerLevel level = event.getServer().overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos probe = new BlockPos(spawn.getX(), level.getMaxBuildHeight() - 4, spawn.getZ());

        long start = System.nanoTime();
        ScanResult result = RightClickIndex.scan(level, probe);
        long millis = (System.nanoTime() - start) / 1_000_000L;

        LOGGER.info("[justenoughrightclicks] verify blocks={} probes={} entries={} errors={} ms={}",
                result.blocks(), result.probes(), result.entries().size(), result.errors(), millis);

        int modded = 0;
        boolean fir = false;
        for (Entry entry : result.entries()) {
            String from = RightClickIndex.id(entry.from());
            String to = RightClickIndex.id(entry.to());
            if (!from.startsWith("minecraft:")) {
                modded++;
            }
            if (from.equals("biomesoplenty:fir_log") && to.equals("biomesoplenty:stripped_fir_log")) {
                fir = true;
                LOGGER.info("[justenoughrightclicks] verify fir_log -> stripped_fir_log held={}",
                        RightClickIndex.id(entry.held().get(0)));
            }
        }
        LOGGER.info("[justenoughrightclicks] verify non_vanilla_sources={}", modded);
        if (!fir) {
            LOGGER.warn("[justenoughrightclicks] verify fir_log -> stripped_fir_log NOT found");
        }

        RightClickIndex.writeDump(result, FMLPaths.CONFIGDIR.get().resolve(RightClickIndex.DUMP_FILE));
    }
}
