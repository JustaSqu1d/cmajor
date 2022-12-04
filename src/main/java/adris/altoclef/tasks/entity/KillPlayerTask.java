package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.progresscheck.IProgressChecker;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import adris.altoclef.util.progresscheck.ProgressCheckerRetry;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

/**
 * Kill a player given their username
 */
public class KillPlayerTask extends AbstractKillEntityTask {

    private final String _playerName;

    private final IProgressChecker<Double> _distancePlayerCheck = new ProgressCheckerRetry<>(new LinearProgressChecker(5, -2), 3);

    private boolean _shielding = false;

    public KillPlayerTask(String name) {
        super(7, 1);
        _playerName = name;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // If we're closer to the player, our task isn't bad.
        Optional<Entity> player = getEntityTarget(mod);
        if (player.isEmpty()) {
            _distancePlayerCheck.reset();
        } else {
            double distSq = player.get().squaredDistanceTo(mod.getPlayer());
            if (distSq < 80 * 80) {
                _distancePlayerCheck.reset();
            }
            _distancePlayerCheck.setProgress(-1 * distSq);
            if (!_distancePlayerCheck.failed()) {
                _progress.reset();
            }
            if (distSq < 5 * 5) {
                startShielding(mod);
            }
            else {
                stopShielding(mod);
            }
        }
        return super.onTick(mod);
    }

    private void startShielding(AltoClef mod) {
        ItemStack handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot());
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (handItem.isFood()) {
            mod.getSlotHandler().clickSlot(PlayerSlot.getEquipSlot(), 0, SlotActionType.PICKUP);
        }
        if (cursor.isFood()) {
            Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(mod));
            if (toMoveTo.isPresent()) {
                Slot garbageSlot = toMoveTo.get();
                mod.getSlotHandler().clickSlot(garbageSlot, 0, SlotActionType.PICKUP);
            }
        }
        mod.getInputControls().hold(Input.CLICK_RIGHT);
        mod.getClientBaritone().getPathingBehavior().softCancelIfSafe();
        _shielding = true;
        mod.getExtraBaritoneSettings().setInteractionPaused(true);
    }

    private void stopShielding(AltoClef mod) {
        if (_shielding) {
            ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
            if (cursor.isFood()) {
                Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(mod));
                if (toMoveTo.isPresent()) {
                    Slot garbageSlot = toMoveTo.get();
                    mod.getSlotHandler().clickSlot(garbageSlot, 0, SlotActionType.PICKUP);
                }
            }
            mod.getInputControls().release(Input.CLICK_RIGHT);
            mod.getInputControls().release(Input.JUMP);
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
            _shielding = false;
        }
    }

    @Override
    protected boolean isSubEqual(AbstractDoToEntityTask other) {
        if (other instanceof KillPlayerTask task) {
            return task._playerName.equals(_playerName);
        }
        return false;
    }

    @Override
    protected Optional<Entity> getEntityTarget(AltoClef mod) {
        if (mod.getEntityTracker().isPlayerLoaded(_playerName)) {
            return mod.getEntityTracker().getPlayerEntity(_playerName).map(Entity.class::cast);
        }
        return Optional.empty();
    }

    @Override
    protected String toDebugString() {
        return "Punking " + _playerName;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        if (_shielding) stopShielding(mod);
    }
}
