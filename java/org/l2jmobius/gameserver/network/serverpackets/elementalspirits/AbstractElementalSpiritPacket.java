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
package org.l2jmobius.gameserver.network.serverpackets.elementalspirits;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author JoeAlisson
 */
abstract class AbstractElementalSpiritPacket extends ServerPacket
{
	void writeSpiritInfo(WritableBuffer buffer, ElementalSpirit spirit)
	{
		buffer.writeByte(spirit.getStage());
		buffer.writeInt(spirit.getNpcId());
		buffer.writeLong(spirit.getExperience());
		buffer.writeLong(spirit.getExperienceToNextLevel());
		buffer.writeLong(spirit.getExperienceToNextLevel());
		buffer.writeInt(spirit.getLevel());
		buffer.writeInt(spirit.getMaxLevel());
		buffer.writeInt(spirit.getAvailableCharacteristicsPoints());
		buffer.writeInt(spirit.getAttackPoints());
		buffer.writeInt(spirit.getDefensePoints());
		buffer.writeInt(spirit.getCriticalRatePoints());
		buffer.writeInt(spirit.getCriticalDamagePoints());
		buffer.writeInt(spirit.getMaxCharacteristics());
		buffer.writeInt(spirit.getMaxCharacteristics());
		buffer.writeInt(spirit.getMaxCharacteristics());
		buffer.writeInt(spirit.getMaxCharacteristics());
		buffer.writeByte(1); // unk
		for (int j = 0; j < 1; j++)
		{
			buffer.writeShort(2);
			buffer.writeLong(100);
		}
	}
}
