package com.shatteredpixel.shatteredpixeldungeon.items.kits;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;

abstract class Kit extends Item {
    @Override
    public boolean collect( Bag container ) {
        giveKitItems(Dungeon.hero);
        return true;
    }

    abstract public void giveKitItems(Hero hero);
}
