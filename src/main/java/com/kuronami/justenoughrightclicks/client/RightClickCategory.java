package com.kuronami.justenoughrightclicks.client;

import com.kuronami.justenoughrightclicks.JustEnoughRightClicks;
import com.kuronami.justenoughrightclicks.index.RightClickIndex;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One row: source block + the item in hand -> the resulting block. */
public final class RightClickCategory extends AbstractRecipeCategory<RightClickIndex.Entry> {
    public static final RecipeType<RightClickIndex.Entry> TYPE = RecipeType.create(
            JustEnoughRightClicks.MODID, "right_clicks", RightClickIndex.Entry.class);

    private static final int WIDTH = 86;
    private static final int HEIGHT = 26;

    private final IDrawable arrow;

    public RightClickCategory(IGuiHelper guiHelper) {
        super(TYPE,
                Component.translatable("jei.justenoughrightclicks.category.right_clicks"),
                guiHelper.createDrawableItemStack(new ItemStack(Items.WOODEN_AXE)),
                WIDTH, HEIGHT);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RightClickIndex.Entry recipe, IFocusGroup focuses) {
        builder.addInputSlot(2, 4)
                .setStandardSlotBackground()
                .addItemStack(recipe.from());
        builder.addInputSlot(20, 4)
                .setStandardSlotBackground()
                .addItemStacks(recipe.held());
        builder.addOutputSlot(62, 4)
                .setOutputSlotBackground()
                .addItemStack(recipe.to());
    }

    @Override
    public void draw(RightClickIndex.Entry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
            double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 40, 5);
    }
}
