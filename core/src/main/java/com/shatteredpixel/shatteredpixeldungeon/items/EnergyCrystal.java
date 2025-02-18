/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
 *
 * Combusted Pixel Dungeon
 * Copyright (C) 2022-2023 Inferno214221 (inferno214221@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class EnergyCrystal extends Item {

	private static final String TXT_VALUE	= "%+d";

	{
		image = ItemSpriteSheet.ENERGY;
		stackable = true;
	}

	public EnergyCrystal() {
		this( 1 );
	}

	public EnergyCrystal( int value ) {
		this.quantity = value;
	}

	@Override
	public ArrayList<String> actions(Hero hero ) {
		//return new ArrayList<>();
		return super.actions( hero );
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {

		Dungeon.energy += quantity;
		//TODO track energy collected maybe? We do already track recipes crafted though..
		//TODO Statistics.goldCollected += quantity;
		//Badges.validateGoldCollected();

		GameScene.pickUp( this, pos );
		hero.sprite.showStatus( 0x44CCFF, TXT_VALUE, quantity);
		hero.spendAndNext( TIME_TO_PICK_UP );

		Sample.INSTANCE.play( Assets.Sounds.ITEM );

		updateQuickslot();

		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public Item random() {
		quantity = Random.IntRange( 4, 6 );
		return this;
	}

}
