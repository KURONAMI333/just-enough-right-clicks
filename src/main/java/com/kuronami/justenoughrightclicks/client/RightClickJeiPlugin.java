package com.kuronami.justenoughrightclicks.client;

import com.kuronami.justenoughrightclicks.Config;
import com.kuronami.justenoughrightclicks.JustEnoughRightClicks;
import com.kuronami.justenoughrightclicks.index.RightClickIndex;
import com.kuronami.justenoughrightclicks.index.RightClickIndex.ScanResult;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

@JeiPlugin
public final class RightClickJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            JustEnoughRightClicks.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RightClickCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            JustEnoughRightClicks.LOGGER.warn("[justenoughrightclicks] no level while JEI registered recipes");
            return;
        }

        ScanResult result = RightClickIndex.scan(level, probePos(minecraft, level));
        registration.addRecipes(RightClickCategory.TYPE, result.entries());
        JustEnoughRightClicks.LOGGER.info(
                "[justenoughrightclicks] indexed {} entries from {} blocks ({} errors)",
                result.entries().size(), result.blocks(), result.errors());

        if (Config.DEBUG_DUMP.get()) {
            RightClickIndex.writeDump(result, FMLPaths.CONFIGDIR.get().resolve(RightClickIndex.DUMP_FILE));
        }
    }

    private static BlockPos probePos(Minecraft minecraft, Level level) {
        if (minecraft.player != null) {
            BlockPos base = minecraft.player.blockPosition();
            for (int dy = 1; dy <= 8; dy++) {
                BlockPos candidate = base.above(dy);
                if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()) {
                    return candidate;
                }
            }
        }
        BlockPos spawn = level.getSharedSpawnPos();
        return new BlockPos(spawn.getX(), level.getMaxBuildHeight() - 4, spawn.getZ());
    }
}
