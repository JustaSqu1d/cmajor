package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import baritone.api.utils.input.Input;

/**
 * Attacks an entity, but the target entity must be specified.
 */
public abstract class AbstractKillEntityTask extends AbstractDoToEntityTask {

    private static final double OTHER_FORCE_FIELD_RANGE = 2;

    // Not the "striking" distance, but the "ok we're close enough, lower our guard for other mobs and focus on this one" range.
    private static final double CONSIDER_COMBAT_RANGE = 10;

    private Integer _hitCooldown = 0;

    private static final Item[] WEAPON_ITEMS = new Item[]{
            Items.NETHERITE_SWORD,
            Items.DIAMOND_SWORD,
            Items.IRON_SWORD,
            Items.STONE_SWORD,
            Items.WOODEN_SWORD,
            Items.GOLDEN_SWORD,
            Items.NETHERITE_AXE,
            Items.DIAMOND_AXE,
            Items.IRON_AXE,
            Items.STONE_AXE,
            Items.WOODEN_AXE,
            Items.GOLDEN_AXE
    };

    public AbstractKillEntityTask() {
        this(CONSIDER_COMBAT_RANGE, OTHER_FORCE_FIELD_RANGE);
    }

    public AbstractKillEntityTask(double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    public AbstractKillEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(maintainDistance, combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    public static void equipWeapon(AltoClef mod) {
        for (Item item : WEAPON_ITEMS) {
            if (mod.getItemStorage().hasItem(item)) {
                mod.getSlotHandler().forceEquipItem(item);
                return;
            }
        }
    }

    @Override
    protected Task onEntityInteract(AltoClef mod, Entity entity) {
        equipWeapon(mod);

        float hitProg = mod.getPlayer().getAttackCooldownProgress(0);

        if (hitProg >= 0.99 && _hitCooldown == 0) {
            _hitCooldown = 1;
            mod.getInputControls().hold(Input.SPRINT);
            mod.getControllerExtras().attack(entity);
            mod.getInputControls().release(Input.SPRINT);
            mod.getInputControls().hold(Input.SPRINT);
        }
        return null;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        _hitCooldown = Math.max(0, _hitCooldown - 1);
        return super.onTick(mod);
    }
}
