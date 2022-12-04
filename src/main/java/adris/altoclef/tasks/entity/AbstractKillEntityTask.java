package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.Optional;

/**
 * Attacks an entity, but the target entity must be specified.
 */
public abstract class AbstractKillEntityTask extends AbstractDoToEntityTask {
    private static final double OTHER_FORCE_FIELD_RANGE = 2;

    // Not the "striking" distance, but the "ok we're close enough, lower our guard for other mobs and focus on this one" range.
    private static final double CONSIDER_COMBAT_RANGE = 10;

    public AbstractKillEntityTask() {
        this(CONSIDER_COMBAT_RANGE, OTHER_FORCE_FIELD_RANGE);
    }

    public AbstractKillEntityTask(double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    public AbstractKillEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(maintainDistance, combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    private boolean _shielding = false;

    public boolean isShielding() {
        return _shielding;
    }

    public static void equipWeapon(AltoClef mod) {
        List<ItemStack> invStacks = mod.getItemStorage().getItemStacksPlayerInventory(true);
        if (!invStacks.isEmpty()) {
            float handDamage = Float.NEGATIVE_INFINITY;
            for (ItemStack invStack : invStacks) {
                if (invStack.getItem() instanceof SwordItem item) {
                    float itemDamage = item.getMaterial().getAttackDamage();
                    Item handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
                    if (handItem instanceof SwordItem handToolItem) {
                        handDamage = handToolItem.getMaterial().getAttackDamage();
                    }
                    if (itemDamage > handDamage) {
                        mod.getSlotHandler().forceEquipItem(item);
                    } else {
                        mod.getSlotHandler().forceEquipItem(handItem);
                    }
                }
            }
        }
    }

    @Override
    protected Task onEntityInteract(AltoClef mod, Entity entity) {
        if (!mod.getFoodChain().isTryingToEat() && !mod.getMLGBucketChain().isFallingOhNo(mod) &&
                mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting() &&
                mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            float hitProg = mod.getPlayer().getAttackCooldownProgress(0);
            // Equip weapon
            equipWeapon(mod);
            // Look at them
            LookHelper.lookAt(mod, entity.getEyePos());
            if (hitProg >= 1) {
                if (mod.getPlayer().isOnGround() || mod.getPlayer().getVelocity().getY() < 0 || mod.getPlayer().isTouchingWater()) {
                    boolean old_value = mod.getPlayer().isSprinting();
                    mod.getInputControls().hold(Input.SPRINT);

                    mod.getControllerExtras().attack(entity);

                    mod.getInputControls().release(Input.SPRINT);
                    if (old_value) mod.getInputControls().hold(Input.SPRINT);
                }
            }
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }
}
