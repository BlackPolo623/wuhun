/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.holders.player.CombatPowerHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExItemScore extends ServerPacket
{
	private final int _total;
	private final int _equipedItem;
	private final int _relics;
	private final int _relicsCollection;
	private final int _adenLab;
	private final int _ensoul;
	private final int _bless;
	
	public ExItemScore(CombatPowerHolder combatPowerHolder)
	{
		_total = combatPowerHolder.getTotalCombatPower();
		_equipedItem = combatPowerHolder.getItemCombatPower();
		_relics = combatPowerHolder.getRelicEffectCombatPower();
		_relicsCollection = combatPowerHolder.getRelicCollectionCombatPower();
		_adenLab = combatPowerHolder.getAdenLabCollectionCP();
		_ensoul = combatPowerHolder.getEnsoulCP();
		_bless = combatPowerHolder.getBlessCP();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ITEM_SCORE.writeId(this, buffer);
		buffer.writeInt(_total);
		buffer.writeInt(_equipedItem);
		buffer.writeInt(_relics);
		buffer.writeInt(_relicsCollection);
		buffer.writeInt(_adenLab);
		buffer.writeInt(_ensoul);
		buffer.writeInt(_bless);
	}
}
