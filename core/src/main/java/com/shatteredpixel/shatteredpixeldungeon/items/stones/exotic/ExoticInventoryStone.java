package com.shatteredpixel.shatteredpixeldungeon.items.stones.exotic;

import com.shatteredpixel.shatteredpixeldungeon.items.stones.InventoryStone;

public abstract class ExoticInventoryStone extends InventoryStone {
    @Override
    public int energyVal() {
        return 4 * quantity;
    }
}
