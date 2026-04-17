/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.commons.threads.ThreadPool;

/**
 * A Peace Zone
 * @author durgus
 */
public class PeaceZone extends ZoneType
{
	public PeaceZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			
			// PVP possible during siege, now for siege participants only
			// Could also check if this town is in siege, or if any siege is going on
			if ((player.getSiegeState() != 0) && (GeneralConfig.PEACE_ZONE_MODE == 1))
			{
				return;
			}
		}
		
		if (GeneralConfig.PEACE_ZONE_MODE != 2)
		{
			creature.setInsideZone(ZoneId.PEACE, true);
		}
		
		if (!getAllowStore())
		{
			creature.setInsideZone(ZoneId.NO_STORE, true);
		}
		
		// Send player info to nearby players.
		if (creature.isPlayer())
		{
			creature.broadcastInfo();
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (GeneralConfig.PEACE_ZONE_MODE != 2)
		{
			creature.setInsideZone(ZoneId.PEACE, false);
		}

		if (!getAllowStore())
		{
			creature.setInsideZone(ZoneId.NO_STORE, false);
		}

		// Send player info to nearby players.
		if (creature.isPlayer() && !creature.isTeleporting())
		{
			creature.broadcastInfo();
		}

		// 【魂契系統】玩家離開和平區時，若有召喚寵物則自動收回
		// 延遲 500ms 執行，確保 zone flag 已完整更新後再判斷
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			ThreadPool.schedule(() ->
			{
				if (player.isOnline() && player.hasPet() && !player.isInsideZone(ZoneId.PEACE))
				{
					player.getPet().unSummon(player);
					player.sendMessage("野外地區不允許攜帶寵物，寵物已自動收回。");
					player.sendMessage("【魂契】功能已開放，締結後寵物能力可在野外持續生效。");
					player.sendMessage("請至孵化NPC與您的寵物締結魂契。");
				}
			}, 500);
		}
	}
	
	@Override
	public void setEnabled(boolean value)
	{
		super.setEnabled(value);
		if (value)
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if ((player != null) && isInsideZone(player))
				{
					revalidateInZone(player);
					
					if (player.getPet() != null)
					{
						revalidateInZone(player.getPet());
					}
					
					for (Summon summon : player.getServitors().values())
					{
						revalidateInZone(summon);
					}
				}
			}
		}
		else
		{
			for (Creature creature : getCharactersInside())
			{
				if (creature != null)
				{
					removeCharacter(creature);
				}
			}
		}
	}
}
