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

import java.util.List;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcuts;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutRegister;
import org.l2jmobius.gameserver.network.serverpackets.autoplay.ExActivateAutoShortcut;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

/**
 * @author Mobius
 */
public class RequestShortcutReg extends ClientPacket
{
	private ShortcutType _type;
	private int _id;
	private int _slot;
	private int _page;
	private int _level;
	private int _subLevel;
	private int _characterType; // 1 - player, 2 - pet
	private boolean _active;
	
	@Override
	protected void readImpl()
	{
		final int typeId = readInt();
		_type = ShortcutType.values()[(typeId < 1) || (typeId > 6) ? 0 : typeId];
		final int position = readInt();
		_slot = position % Shortcuts.MAX_SHORTCUTS_PER_BAR;
		_page = position / Shortcuts.MAX_SHORTCUTS_PER_BAR;
		_active = readByte() == 1; // 228
		_id = readInt();
		_level = readShort();
		_subLevel = readShort(); // Sublevel
		_characterType = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_page > 25) || (_page < 0))
		{
			return;
		}
		
		// Auto play checks.
		if ((_page == 22) || (_page == 24)) // Consumables.
		{
			if (_type != ShortcutType.ITEM)
			{
				return;
			}
			
			final Item item = player.getInventory().getItemByObjectId(_id);
			if ((item == null) || item.isPotion())
			{
				return;
			}
		}
		else if (_page == 23)
		{
			if ((_slot == 0) || (_slot == 3)) // Macro.
			{
				if (_type != ShortcutType.MACRO)
				{
					return;
				}
			}
			else // Healing potion.
			{
				final Item item = player.getInventory().getItemByObjectId(_id);
				if ((item == null) || !item.isPotion())
				{
					return;
				}
			}
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
						if (player.getInventory().getItemByObjectId(oldShortcut.getId()).isPotion())
						{
							AutoUseTaskManager.getInstance().removeAutoPotionItem(player);
						}
						else
						{
							AutoUseTaskManager.getInstance().removeAutoSupplyItem(player, oldShortcut.getId());
						}
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
		
		final Shortcut sc = new Shortcut(_slot, _page, _type, _id, _level, _subLevel, _characterType);
		sc.setAutoUse(_active);
		player.registerShortcut(sc);
		player.sendPacket(new ShortcutRegister(sc, player));
		player.sendPacket(new ExActivateAutoShortcut(sc, _active));
		player.sendSkillList();
		
		// When id is not auto used, deactivate auto shortcuts.
		if (!player.getAutoUseSettings().isAutoSkill(_id) && !player.getAutoUseSettings().getAutoSupplyItems().contains(_id))
		{
			final List<Integer> positions = player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_SHORTCUTS);
			final Integer position = _slot + (_page * Shortcuts.MAX_SHORTCUTS_PER_BAR);
			if (!positions.contains(position))
			{
				return;
			}
			
			positions.remove(position);
			player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_SHORTCUTS, positions);
			return;
		}
		
		// Activate if any other similar shortcut is activated.
		for (Shortcut shortcut : player.getAllShortcuts())
		{
			if (!shortcut.isAutoUse() || (shortcut.getId() != _id) || (shortcut.getType() != _type))
			{
				continue;
			}
			
			player.addAutoShortcut(_slot, _page);
			break;
		}
	}
}
