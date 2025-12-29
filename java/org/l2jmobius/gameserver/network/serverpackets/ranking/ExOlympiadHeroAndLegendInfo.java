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
package org.l2jmobius.gameserver.network.serverpackets.ranking;

import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.managers.RankManager.HeroInfo;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author NviX, Mobius
 */
public class ExOlympiadHeroAndLegendInfo extends ServerPacket
{
	private final Collection<HeroInfo> _heroes;
	
	public ExOlympiadHeroAndLegendInfo()
	{
		_heroes = RankManager.getInstance().getSnapshotHeroList();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_OLYMPIAD_HERO_AND_LEGEND_INFO.writeId(this, buffer);
		if (_heroes.isEmpty())
		{
			return;
		}
		
		boolean wroteCount = false;
		for (HeroInfo hero : _heroes)
		{
			if (hero.isTopHero)
			{
				buffer.writeByte(1); // ?? shows 78 on JP
				buffer.writeByte(1); // ?? shows 0 on JP
			}
			else if (!wroteCount)
			{
				wroteCount = true;
				buffer.writeInt(Hero.getInstance().getHeroes().size() - 1);
			}
			
			buffer.writeSizedString(hero.charName);
			buffer.writeSizedString(hero.clanName);
			buffer.writeInt(hero.serverId);
			buffer.writeInt(hero.race);
			buffer.writeInt(hero.isMale ? 1 : 0);
			buffer.writeInt(hero.baseClass);
			buffer.writeInt(hero.level);
			buffer.writeInt(hero.legendCount);
			buffer.writeInt(hero.competitionsWon);
			buffer.writeInt(hero.competitionsLost);
			buffer.writeInt(hero.competitionsDrawn); // DrawCount (474)
			buffer.writeInt(hero.olympiadPoints);
			buffer.writeInt(hero.clanLevel);
			
			if (!hero.isTopHero)
			{
				buffer.writeInt(_heroes.size() - 1); // Write the number of additional heroes.
			}
		}
	}
}
