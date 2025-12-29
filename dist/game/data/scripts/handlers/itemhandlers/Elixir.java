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
package handlers.itemhandlers;

import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Elixir extends ItemSkills
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}
		
		final Player player = playable.asPlayer();
		final int effectBonus = (int) player.getStat().getValue(Stat.ELIXIR_USAGE_LIMIT, 0);
		final int elixirsAvailable = player.getVariables().getInt(PlayerVariables.ELIXIRS_AVAILABLE, 0);
		if ((player.getLevel() < 76) || //
			((player.getLevel() < 85) && (elixirsAvailable >= (5 + effectBonus))) || //
			((player.getLevel() < 87) && (elixirsAvailable >= (7 + effectBonus))) || //
			((player.getLevel() < 88) && (elixirsAvailable >= (9 + effectBonus))) || //
			((player.getLevel() < 89) && (elixirsAvailable >= (11 + effectBonus))) || //
			((player.getLevel() < 90) && (elixirsAvailable >= (13 + effectBonus))) || //
			((player.getLevel() < 91) && (elixirsAvailable >= (15 + effectBonus))) || //
			((player.getLevel() < 92) && (elixirsAvailable >= (17 + effectBonus))) || //
			((player.getLevel() < 93) && (elixirsAvailable >= (19 + effectBonus))) || //
			((player.getLevel() < 94) && (elixirsAvailable >= (21 + effectBonus))) || //
			((player.getLevel() < 95) && (elixirsAvailable >= (23 + effectBonus))) || //
			((player.getLevel() < 100) && (elixirsAvailable >= (25 + effectBonus))))
		{
			player.sendPacket(SystemMessageId.THE_ELIXIR_IS_UNAVAILABLE);
			return false;
		}
		
		if (super.onItemUse(player, item, forceUse))
		{
			player.getVariables().set(PlayerVariables.ELIXIRS_AVAILABLE, elixirsAvailable + 1);
			player.sendPacket(new SystemMessage(SystemMessageId.DUE_TO_THE_ELIXIR_S_EFFECT_YOUR_STATS_S1).addInt(elixirsAvailable + 1));
			player.broadcastUserInfo();
			return true;
		}
		
		return false;
	}
}
