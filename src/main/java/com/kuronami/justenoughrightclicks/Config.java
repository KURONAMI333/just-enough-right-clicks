package com.kuronami.justenoughrightclicks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_DUMP = BUILDER
            .comment("Write the indexed right-click list to config/justenoughrightclicks-dump.txt.")
            .define("debugDump", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
