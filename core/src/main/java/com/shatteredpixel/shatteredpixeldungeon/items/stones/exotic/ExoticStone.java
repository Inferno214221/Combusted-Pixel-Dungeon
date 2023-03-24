package com.shatteredpixel.shatteredpixeldungeon.items.stones.exotic;

import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;

public abstract class ExoticStone extends Runestone {
    @Override
    public int energyVal() {
        return 4 * quantity;
    }
}
