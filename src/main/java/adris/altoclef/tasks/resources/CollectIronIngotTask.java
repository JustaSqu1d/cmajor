package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.CraftInTableTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.container.SmeltInFurnaceTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.*;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.item.Items;

public class CollectIronIngotTask extends ResourceTask {

    private final int _count;

    public CollectIronIngotTask(int count) {
        super(Items.IRON_INGOT, count);
        _count = count;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        int nuggs = mod.getItemStorage().getItemCount(Items.IRON_NUGGET);
        if (nuggs >= 9) {
            // If we have enough nuggets, craft them.
            ItemTarget n = new ItemTarget(Items.IRON_NUGGET);
            CraftingRecipe recipe = CraftingRecipe.newShapedRecipe("iron_ingot", new ItemTarget[]{
                    n, n, n, n, n, n, n, n, n
            }, 1);
            return new CraftInTableTask(new RecipeTarget(Items.IRON_INGOT, _count, recipe));
        } else if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD){
                return new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(Items.IRON_INGOT, _count), new ItemTarget(Items.RAW_IRON, _count)));
        } else {
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectIronIngotTask && ((CollectIronIngotTask) other)._count == _count;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " iron.";
    }
}
