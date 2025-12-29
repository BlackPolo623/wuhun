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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcuts;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

/**
 * @author Mobius
 */
public class RequestShortcutDel extends ClientPacket
{
	private int _slot;
	private int _page;
	
	@Override
	protected void readImpl()
	{
		final int position = readInt();
		_slot = position % Shortcuts.MAX_SHORTCUTS_PER_BAR;
		_page = position / Shortcuts.MAX_SHORTCUTS_PER_BAR;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_page > 24) || (_page < 0))
		{
			return;
		}
		
		// Delete the shortcut.
		final Shortcut oldShortcut = player.getShortcut(_slot, _page);
		player.deleteShortcut(_slot, _page);
		
		if (oldShortcut != null)
		{
			boolean removed = true;
			
			// Keep other similar shortcuts activated.
			if (oldShortcut.isAutoUse())
			{
				player.removeAutoShortcut(_slot, _page);
				for (Shortcut shortcut : player.getAllShortcuts())
				{
					if ((oldShortcut.getId() == shortcut.getId()) && (oldShortcut.getType() == shortcut.getType()))
					{
						player.addAutoShortcut(shortcut.getSlot(), shortcut.getPage());
						removed = false;
					}
				}
			}
			
			// Remove auto used ids.
			if (removed)
			{
				switch (oldShortcut.getType())
				{
					case SKILL:
					{
						AutoUseTaskManager.getInstance().removeAutoBuff(player, oldShortcut.getId());
						AutoUseTaskManager.getInstance().removeAutoSkill(player, oldShortcut.getId());
						break;
					}
					case ITEM:
					{
						AutoUseTaskManager.getInstance().removeAutoSupplyItem(player, oldShortcut.getId());
						break;
					}
					case ACTION:
					{
						AutoUseTaskManager.getInstance().removeAutoAction(player, oldShortcut.getId());
						break;
					}
				}
			}
		}
		
		player.restoreAutoShortcutVisual();
	}
}
