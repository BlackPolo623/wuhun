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
package org.l2jmobius.gameserver.network.serverpackets.newskillenchant;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.EnchantStarHolder;
import org.l2jmobius.gameserver.data.xml.SkillEnchantData;
import org.l2jmobius.gameserver.model.ItemInfo;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * @author Serenitty
 */
public class ExSkillEnchantInfo extends AbstractItemPacket
{
	private final Skill _skill;
	private final Player _player;
	private final EnchantStarHolder _starHolder;
	
	public ExSkillEnchantInfo(Skill skill, Player player)
	{
		_skill = skill;
		_player = player;
		_starHolder = SkillEnchantData.getInstance().getEnchantStar(SkillEnchantData.getInstance().getSkillEnchant(skill.getId()).getStarLevel());
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SKILL_ENCHANT_INFO.writeId(this, buffer);
		buffer.writeInt(_skill.getId());
		
		// buffer.writeInt(_player.getKnownSkill(_skill.getId()).getSubLevel());
		buffer.writeInt(_skill.getSubLevel());
		
		buffer.writeInt(_player.getSkillEnchantExp(_starHolder.getLevel()));
		buffer.writeInt(_starHolder.getExpMax());
		buffer.writeInt(SkillEnchantData.getInstance().getChanceEnchantMap(_skill) * 100);
		buffer.writeShort(calculatePacketSize(new ItemInfo(new Item(Inventory.ADENA_ID))));
		buffer.writeInt(Inventory.ADENA_ID);
		buffer.writeLong(1000000);
	}
}
