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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class PopupEventHud extends ServerPacket
{
	public static final int MONSTER_INVASION_EVIL_MOSTERS_LEGION = 1;
	public static final int MONSTER_INVASION_OL_MAHUM_ALLIANCE = 2;
	public static final int MONSTER_INVASION_FURIOUS_LIZARDMENS_LEGION = 3;
	public static final int MONSTER_INVASION_BRUTAL_MONSTERS_LEGION = 4;
	public static final int MONSTER_INVASION_TANTAR_LIZARDMEN_TRIBE = 5;
	public static final int LEGION_OF_DARKNESS_INVASION_PLAINS_OF_LIZARDMEN = 6;
	public static final int LEGION_OF_DARKNESS_INVASION_WESTERN_DRAGON_VALLEY = 7;
	public static final int LEGION_OF_DARKNESS_INVASION_EASTERN_DRAGON_VALLEY = 8;
	public static final int LEGION_OF_DARKNESS_INVASION_MELAT_LIZARDMEN = 9;
	public static final int LEGION_OF_DARKNESS_INVASION_TAYGA_CAMP = 10;
	public static final int DEATHMATCH_WITH_THE_LEGION_OF_DARKNESS_GIRAN_CASTLE = 11;
	public static final int WATERMELON_SORTING_WEST_TALKING_ISLAND = 12;
	public static final int FINAL_BLOW_TO_WATERMELON_FANTASY_ISLE = 13;
	public static final int CATACOMBS = 14;
	public static final int MONSTER_INVASION_SHIFTING_MIRAGE = 15;
	
	private final int _id;
	private final boolean _enabled;
	
	public PopupEventHud(int id, boolean enabled)
	{
		_id = id;
		_enabled = enabled;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_POPUP_EVENT_HUD.writeId(this, buffer);
		buffer.writeInt(_id);
		buffer.writeByte(_enabled);
	}
}
