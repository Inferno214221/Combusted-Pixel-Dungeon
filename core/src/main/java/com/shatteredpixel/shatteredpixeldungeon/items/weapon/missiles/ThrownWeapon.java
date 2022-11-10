package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

abstract public class ThrownWeapon extends MissileWeapon{
    {
        stackable = true;
        levelKnown = false;

        bones = true;

        defaultAction = AC_THROW;
        usesTargeting = true;
    }

    @Override
    public ArrayList<String> actions(Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        if( !actions.contains( AC_UNEQUIP ) ) actions.add( AC_EQUIP );
        return actions;
    }

    @Override
    public int min(int lvl) {
        return  tier +  //base
                lvl;    //level scaling
    }

    @Override
    public int max(int lvl) {
        return  5*(tier+1) +    //base
                lvl*(tier+1);   //level scaling
    }

    public int STRReq(int lvl){
        return STRReq(tier, lvl);
    }

    //Cant hit melee?
    @Override
    public int damageRoll(Char owner) {
        int damage = augment.damageFactor(super.damageRoll( owner ));

        if (owner instanceof Hero) {
            int exStr = ((Hero)owner).STR() - STRReq();
            if (exStr > 0) {
                damage += Random.IntRange( 0, exStr );
            }
        }

        return damage;
    }

    public boolean isIdentified() {
        return levelKnown && cursedKnown;
    }

    @Override
    public Item random() {
        //+0: 75% (3/4)
        //+1: 20% (4/20)
        //+2: 5%  (1/20)
        int n = 0;
        if (Random.Int(4) == 0) {
            n++;
            if (Random.Int(5) == 0) {
                n++;
            }
        }
        level(n);

        //30% chance to be cursed
        //10% chance to be enchanted
        float effectRoll = Random.Float();
        if (effectRoll < 0.3f) {
            enchant(Enchantment.randomCurse());
            cursed = true;
        } else if (effectRoll >= 0.9f){
            enchant();
        }

        return this;
    }

//    @Override
//    public boolean collect(Bag container) {//aaargh
//        if (Dungeon.hero.belongings.weapon.isSimilar(this)) {
//            ThrownWeapon weapon = (ThrownWeapon) Dungeon.hero.belongings.weapon();
//            weapon.merge(this);
//        }
//        return super.collect(container);
//    }

    public boolean thrownCollect(Bag container) {//aaargh
        if (Dungeon.hero.belongings.weapon instanceof ThrownWeapon) {
            ThrownWeapon weapon = (ThrownWeapon) Dungeon.hero.belongings.weapon();
            weapon.doUnequip(Dungeon.hero, true);
            boolean collect = super.collect(container);
            weapon.doEquip(Dungeon.hero);
            return collect;
        }
        return super.collect(container);
    }

    public boolean doPickUp(Hero hero, int pos) {
        if (thrownCollect( hero.belongings.backpack )) {

            GameScene.pickUp( this, pos );
            Sample.INSTANCE.play( Assets.Sounds.ITEM );
            hero.spendAndNext( TIME_TO_PICK_UP );
            return true;

        } else {
            return false;
        }
    }

    @Override
    public int value() {
        int price = 20 * tier;
        if (hasGoodEnchant()) {
            price *= 1.5;
        }
        if (cursedKnown && (cursed || hasCurseEnchant())) {
            price /= 2;
        }
        if (levelKnown && level() > 0) {
            price *= (level() + 1);
        }
        if (price < 1) {
            price = 1;
        }
        return price;
    }
}
