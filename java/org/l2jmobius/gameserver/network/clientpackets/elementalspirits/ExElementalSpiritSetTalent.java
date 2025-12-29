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
package org.l2jmobius.gameserver.network.clientpackets.elementalspirits;

import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.ElementalSpiritType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ElementalSpiritSetTalent;

/**
 * @author JoeAlisson
 */
public class ExElementalSpiritSetTalent extends ClientPacket
{
	private byte _type;
	private byte _attackPoints;
	private byte _defensePoints;
	private byte _critRate;
	private byte _critDamage;
	
	@Override
	protected void readImpl()
	{
		_type = readByte();
		readByte(); // Characteristics for now always 4
		
		readByte(); // attack id
		_attackPoints = readByte();
		readByte(); // defense id
		_defensePoints = readByte();
		readByte(); // crit rate id
		_critRate = readByte();
		readByte(); // crit damage id
		_critDamage = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final ElementalSpirit spirit = player.getElementalSpirit(ElementalSpiritType.of(_type));
		boolean result = false;
		if (spirit != null)
		{
			if ((_attackPoints > 0) && (spirit.getAvailableCharacteristicsPoints() >= _attackPoints))
			{
				spirit.addAttackPoints(_attackPoints);
				result = true;
			}
			
			if ((_defensePoints > 0) && (spirit.getAvailableCharacteristicsPoints() >= _defensePoints))
			{
				spirit.addDefensePoints(_defensePoints);
				result = true;
			}
			
			if ((_critRate > 0) && (spirit.getAvailableCharacteristicsPoints() >= _critRate))
			{
				spirit.addCritRatePoints(_critRate);
				result = true;
			}
			
			if ((_critDamage > 0) && (spirit.getAvailableCharacteristicsPoints() >= _critDamage))
			{
				spirit.addCritDamage(_critDamage);
				result = true;
			}
		}
		
		if (result)
		{
			final UserInfo userInfo = new UserInfo(player);
			userInfo.addComponentType(UserInfoType.ATT_SPIRITS);
			player.sendPacket(userInfo);
			player.sendPacket(new SystemMessage(SystemMessageId.CHARACTERISTICS_WERE_APPLIED_SUCCESSFULLY));
		}
		
		player.sendPacket(new ElementalSpiritSetTalent(player, _type, result));
	}
}
