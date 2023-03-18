package com.shatteredpixel.shatteredpixeldungeon.items.weapon.thrown;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

abstract public class ThrownWeapon extends Weapon {
    {
        stackable = true;
        levelKnown = false;

        bones = true;

        defaultAction = AC_THROW;
        usesTargeting = true;
    }

    protected boolean sticky = true;

    public static final float MAX_DURABILITY = 100;
    protected float durability = MAX_DURABILITY;
    protected float baseUses = 10;

    public boolean holster;

    //used to reduce durability from the source weapon stack, rather than the one being thrown.
    protected ThrownWeapon parent;

    public int tier;

    @Override
    //FIXME some logic here assumes the items are in the player's inventory. Might need to adjust
    public Item upgrade() {
        if (!bundleRestoring) {
            durability = MAX_DURABILITY;
            if (quantity > 1) {
                ThrownWeapon upgraded = (ThrownWeapon) split(1);
                upgraded.parent = null;

                upgraded = (ThrownWeapon) upgraded.upgrade();

                //try to put the upgraded into inventory, if it didn't already merge
                if (upgraded.quantity() == 1 && !upgraded.collect()) {
                    Dungeon.level.drop(upgraded, Dungeon.hero.pos);
                }
                updateQuickslot();
                return upgraded;
            } else {
                super.upgrade();

                Item similar = Dungeon.hero.belongings.getSimilar(this);
                if (similar != null){
                    detach(Dungeon.hero.belongings.backpack);
                    Item result = similar.merge(this);
                    updateQuickslot();
                    return result;
                }
                updateQuickslot();
                return this;
            }

        } else {
            return super.upgrade();
        }
    }

    @Override
    public boolean collect(Bag container) {
        if (container instanceof MagicalHolster) holster = true;
        return super.collect(container);
    }

    @Override
    public int throwPos(Hero user, int dst) {

        boolean projecting = hasEnchant(Projecting.class, user);
        if (!projecting && Random.Int(3) < user.pointsInTalent(Talent.SHARED_ENCHANTMENT)){
            SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);
            if (bow != null && bow.hasEnchant(Projecting.class, user)) {
                projecting = true;
            }
        }

        if (projecting
                && (Dungeon.level.passable[dst] || Dungeon.level.avoid[dst])
                && Dungeon.level.distance(user.pos, dst) <= Math.round(4 * RingOfArcana.enchantPowerMultiplier(user))){
            return dst;
        } else {
            return super.throwPos(user, dst);
        }
    }

    @Override
    public float accuracyFactor(Char owner, Char target) {
        float accFactor = super.accuracyFactor(owner, target);
        if (owner instanceof Hero && owner.buff(Momentum.class) != null && owner.buff(Momentum.class).freerunning()){
            accFactor *= 1f + 0.2f*((Hero) owner).pointsInTalent(Talent.PROJECTILE_MOMENTUM);
        }

        accFactor *= adjacentAccFactor(owner, target);

        return accFactor;
    }

    protected float adjacentAccFactor(Char owner, Char target){
        if (Dungeon.level.adjacent( owner.pos, target.pos )) {
            if (owner instanceof Hero){
                return (0.5f + 0.2f*((Hero) owner).pointsInTalent(Talent.POINT_BLANK));
            } else {
                return 0.5f;
            }
        } else {
            return 1.5f;
        }
    }

    @Override
    public void doThrow(Hero hero) {
        parent = null; //reset parent before throwing, just incase
        super.doThrow(hero);
    }

    @Override
    protected void onThrow( int cell ) {
        Char enemy = Actor.findChar( cell );
        if (enemy == null || enemy == curUser) {
            parent = null;

            //metamorphed seer shot logic
            if (curUser.hasTalent(Talent.SEER_SHOT)
                    && curUser.heroClass != HeroClass.HUNTRESS
                    && curUser.buff(Talent.SeerShotCooldown.class) == null){
                if (Actor.findChar(cell) == null) {
                    RevealedArea a = Buff.affect(curUser, RevealedArea.class, 5 * curUser.pointsInTalent(Talent.SEER_SHOT));
                    a.depth = Dungeon.depth;
                    a.pos = cell;
                    Buff.affect(curUser, Talent.SeerShotCooldown.class, 20f);
                }
            }

            super.onThrow( cell );
        } else {
            if (!curUser.shoot( enemy, this )) {
                rangedMiss( cell );
            } else {

                rangedHit( enemy, cell );

            }
        }
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (attacker == Dungeon.hero && Random.Int(3) < Dungeon.hero.pointsInTalent(Talent.SHARED_ENCHANTMENT)){
            SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);
            if (bow != null && bow.enchantment != null && Dungeon.hero.buff(MagicImmune.class) == null) {
                damage = bow.enchantment.proc(this, attacker, defender, damage);
            }
        }

        return super.proc(attacker, defender, damage);
    }

    public String status() {
        //show quantity even when it is 1
        return Integer.toString( quantity );
    }

    @Override
    public float castDelay(Char user, int dst) {
        return delayFactor( user );
    }

    protected void rangedHit( Char enemy, int cell ){
        decrementDurability();
        if (durability > 0){
            //attempt to stick the missile weapon to the enemy, just drop it if we can't.
            if (sticky && enemy != null && enemy.isAlive() && enemy.alignment != Char.Alignment.ALLY){
                PinCushion p = Buff.affect(enemy, PinCushion.class);
                if (p.target == enemy){
                    p.stick(this);
                    return;
                }
            }
            Dungeon.level.drop( this, cell ).sprite.drop();
        }
    }

    protected void rangedMiss( int cell ) {
        parent = null;
        super.onThrow(cell);
    }

    public float durabilityLeft(){
        return durability;
    }

    public void repair( float amount ){
        durability += amount;
        durability = Math.min(durability, MAX_DURABILITY);
    }

    public float durabilityPerUse(){
        float usages = baseUses * (float)(Math.pow(3, level()));

        //+50%/75% durability
        if (Dungeon.hero.hasTalent(Talent.DURABLE_PROJECTILES)){
            usages *= 1.25f + (0.25f*Dungeon.hero.pointsInTalent(Talent.DURABLE_PROJECTILES));
        }
        if (holster) {
            usages *= MagicalHolster.HOLSTER_DURABILITY_FACTOR;
        }

        usages *= RingOfSharpshooting.durabilityMultiplier( Dungeon.hero );

        //at 100 uses, items just last forever.
        if (usages >= 100f) return 0;

        usages = Math.round(usages);

        //add a tiny amount to account for rounding error for calculations like 1/3
        return (MAX_DURABILITY/usages) + 0.001f;
    }

    protected void decrementDurability(){
        //if this weapon was thrown from a source stack, degrade that stack.
        //unless a weapon is about to break, then break the one being thrown
        if (parent != null){
            if (parent.durability <= parent.durabilityPerUse()){
                durability = 0;
                parent.durability = MAX_DURABILITY;
            } else {
                parent.durability -= parent.durabilityPerUse();
                if (parent.durability > 0 && parent.durability <= parent.durabilityPerUse()){
                    if (level() <= 0) GLog.w(Messages.get(this, "about_to_break"));
                    else             GLog.n(Messages.get(this, "about_to_break"));
                }
            }
            parent = null;
        } else {
            durability -= durabilityPerUse();
            if (durability > 0 && durability <= durabilityPerUse()){
                if (level() <= 0)GLog.w(Messages.get(this, "about_to_break"));
                else             GLog.n(Messages.get(this, "about_to_break"));
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        durability = MAX_DURABILITY;
    }

    @Override
    public Item merge(Item other) {
        super.merge(other);
        if (isSimilar(other)) {
            durability += ((ThrownWeapon)other).durability;
            durability -= MAX_DURABILITY;
            while (durability <= 0){
                quantity -= 1;
                durability += MAX_DURABILITY;
            }
        }
        return this;
    }

    @Override
    public Item split(int amount) {
        bundleRestoring = true;
        Item split = super.split(amount);
        bundleRestoring = false;

        //unless the thrown weapon will break, split off a max durability item and
        //have it reduce the durability of the main stack. Cleaner to the player this way
        if (split != null){
            ThrownWeapon m = (ThrownWeapon) split;
            m.durability = MAX_DURABILITY;
            m.parent = this;
        }

        return split;
    }

    @Override
    public String info() {

        String info = desc();

        if (levelKnown) {
            info += "\n\n" + Messages.get(ThrownWeapon.class, "stats_known", tier, augment.damageFactor(min()), augment.damageFactor(max()), STRReq());
            if (STRReq() > Dungeon.hero.STR()) {
                info += " " + Messages.get(Weapon.class, "too_heavy");
            } else if (Dungeon.hero.STR() > STRReq()){
                info += " " + Messages.get(Weapon.class, "excess_str", Dungeon.hero.STR() - STRReq());
            }
        } else {
            info += "\n\n" + Messages.get(ThrownWeapon.class, "stats_unknown", tier, min(0), max(0), STRReq(0));
            if (STRReq(0) > Dungeon.hero.STR()) {
                info += " " + Messages.get(ThrownWeapon.class, "probably_too_heavy");
            }
        }

        String statsInfo = statsInfo();
        if (!statsInfo.equals("")) info += "\n" + statsInfo;

        info += "\n\n" + Messages.get(ThrownWeapon.class, "distance");

        info += "\n\n" + Messages.get(this, "durability");

        if (durabilityPerUse() > 0){
            info += " " + Messages.get(this, "uses_left",
                    (int)Math.ceil(durability/durabilityPerUse()),
                    (int)Math.ceil(MAX_DURABILITY/durabilityPerUse()));
        } else {
            info += " " + Messages.get(this, "unlimited_uses");
        }

        switch (augment) {
            case SPEED:
                info += " " + Messages.get(Weapon.class, "faster");
                break;
            case DAMAGE:
                info += " " + Messages.get(Weapon.class, "stronger");
                break;
            case NONE:
        }

        if (enchantment != null && (cursedKnown || !enchantment.curse())){
            info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", enchantment.name()));
            info += " " + enchantment.desc();
        }

        if (cursed && isEquipped( Dungeon.hero )) {
            info += "\n\n" + Messages.get(Weapon.class, "cursed_worn");
        } else if (cursedKnown && cursed) {
            info += "\n\n" + Messages.get(Weapon.class, "cursed");
        } else if (!isIdentified() && cursedKnown){
            if (enchantment != null && enchantment.curse()) {
                info += "\n\n" + Messages.get(Weapon.class, "weak_cursed");
            } else {
                info += "\n\n" + Messages.get(Weapon.class, "not_cursed");
            }
        }

        return info;
    }

    public String statsInfo(){
        return Messages.get(this, "stats_desc");
    }

    private static final String DURABILITY = "durability";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(DURABILITY, durability);
    }

    private static boolean bundleRestoring = false;

    @Override
    public void restoreFromBundle(Bundle bundle) {
        bundleRestoring = true;
        super.restoreFromBundle(bundle);
        bundleRestoring = false;
        durability = bundle.getFloat(DURABILITY);
    }

    public static class PlaceHolder extends MissileWeapon {

        {
            image = ItemSpriteSheet.MISSILE_HOLDER;
        }

        @Override
        public boolean isSimilar(Item item) {
            return item instanceof MissileWeapon;
        }

        @Override
        public String info() {
            return "";
        }
    }

//    @Override
//    public ArrayList<String> actions(Hero hero ) {
//        ArrayList<String> actions = super.actions( hero );
//        if( !actions.contains( AC_UNEQUIP ) ) actions.add( AC_EQUIP );
//        return actions;
//    }

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

    //different damage rolls for melee and ranged.
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

    public int rangedMin() {
        return Math.max(0, min( buffedLvl() + RingOfSharpshooting.levelDamageBonus(Dungeon.hero) ));
    }

    public int rangedMin(int lvl) {
        return  2 * tier +                      //base
                (tier == 1 ? lvl : 2*lvl);      //level scaling
    }

    public int rangedMax() {
        return Math.max(0, max( buffedLvl() + RingOfSharpshooting.levelDamageBonus(Dungeon.hero) ));
    }

    public int rangedMax(int lvl) {
        return  5 * tier +                      //base
                (tier == 1 ? 2*lvl : tier*lvl); //level scaling
    }

    public int rangedDamageRoll(Char owner) {
        int damage = augment.damageFactor(super.damageRoll( owner ));//return Random.NormalIntRange( min(), max() );

        if (owner instanceof Hero) {
            int exStr = ((Hero)owner).STR() - STRReq();
            if (exStr > 0) {
                damage += Random.IntRange( 0, exStr );
            }
            if (owner.buff(Momentum.class) != null && owner.buff(Momentum.class).freerunning()) {
                damage = Math.round(damage * (1f + 0.15f * ((Hero) owner).pointsInTalent(Talent.PROJECTILE_MOMENTUM)));
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

    public boolean thrownCollect(Bag container) {
        if (Dungeon.hero.belongings.weapon instanceof ThrownWeapon) {
            ThrownWeapon weapon = (ThrownWeapon) Dungeon.hero.belongings.weapon();
            int slot = Dungeon.quickslot.getSlot(weapon);
            weapon.doUnequip(Dungeon.hero, true);
            boolean collect = super.collect(container);
            weapon.doEquip(Dungeon.hero);
            if (slot > -1) Dungeon.quickslot.setSlot(slot, weapon);
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

    //on throw add a placeholder
}
