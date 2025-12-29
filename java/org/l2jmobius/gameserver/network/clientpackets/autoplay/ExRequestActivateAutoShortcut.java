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
package org.l2jmobius.gameserver.network.clientpackets.autoplay;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcuts;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

/**
 * @author Mobius
 */
public class ExRequestActivateAutoShortcut extends ClientPacket
{
	private int _slot;
	private int _page;
	private boolean _active;
	
	@Override
	protected void readImpl()
	{
		final int position = readShort();
		_slot = position % Shortcuts.MAX_SHORTCUTS_PER_BAR;
		_page = position / Shortcuts.MAX_SHORTCUTS_PER_BAR;
		_active = readByte() == 1;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_active)
		{
			player.addAutoShortcut(_slot, _page);
		}
		else
		{
			player.removeAutoShortcut(_slot, _page);
		}
		
		Item item = null;
		Skill skill = null;
		if ((_slot == -1) && (_page == 0) && _active)
		{
			for (int i = 0; i < 12; i++)
			{
				final Shortcut autoUseAllSupply1 = player.getShortcut(i, 22);
				final Shortcut autoUseAllSupply2 = player.getShortcut(i, 24);
				
				if (autoUseAllSupply1 != null)
				{
					final Item itemAll1 = player.getInventory().getItemByObjectId(autoUseAllSupply1.getId());
					if (itemAll1 != null)
					{
						player.addAutoShortcut(i, 22);
						AutoUseTaskManager.getInstance().addAutoSupplyItem(player, itemAll1.getId());
					}
				}
				
				if (autoUseAllSupply2 != null)
				{
					final Item itemAll2 = player.getInventory().getItemByObjectId(autoUseAllSupply2.getId());
					if (itemAll2 != null)
					{
						player.addAutoShortcut(i, 24);
						AutoUseTaskManager.getInstance().addAutoSupplyItem(player, itemAll2.getId());
					}
				}
			}
			return;
		}
		else if ((_slot == -1) && (_page == 0) && !_active)
		{
			for (int i = 0; i < 12; i++)
			{
				final Shortcut autoUseAllSupply1 = player.getShortcut(i, 22);
				final Shortcut autoUseAllSupply2 = player.getShortcut(i, 24);
				
				if (autoUseAllSupply1 != null)
				{
					final Item itemAll1 = player.getInventory().getItemByObjectId(autoUseAllSupply1.getId());
					if (itemAll1 != null)
					{
						player.removeAutoShortcut(i, 22);
						AutoUseTaskManager.getInstance().removeAutoSupplyItem(player, itemAll1.getId());
					}
				}
				
				if (autoUseAllSupply2 != null)
				{
					final Item itemAll2 = player.getInventory().getItemByObjectId(autoUseAllSupply2.getId());
					if (itemAll2 != null)
					{
						player.removeAutoShortcut(i, 24);
						AutoUseTaskManager.getInstance().removeAutoSupplyItem(player, itemAll2.getId());
					}
				}
			}
			return;
		}
		
		final Shortcut shortcut = player.getShortcut(_slot, _page);
		if ((shortcut == null))
		{
			return;
		}
		
		if (shortcut.getType() == ShortcutType.SKILL)
		{
			final int skillId = player.getReplacementSkill(shortcut.getId());
			skill = player.getKnownSkill(skillId);
			if (skill == null)
			{
				if (player.hasServitors())
				{
					for (Summon summon : player.getServitors().values())
					{
						skill = summon.getKnownSkill(skillId);
						if (skill != null)
						{
							break;
						}
					}
				}
				
				if ((skill == null) && player.hasPet())
				{
					skill = player.getPet().getKnownSkill(skillId);
				}
			}
		}
		else
		{
			item = player.getInventory().getItemByObjectId(shortcut.getId());
		}
		
		// stop
		if (!_active)
		{
			if (item != null)
			{
				// auto supply
				if (!item.isPotion())
				{
					AutoUseTaskManager.getInstance().removeAutoSupplyItem(player, item.getId());
				}
				else // auto potion
				{
					AutoUseTaskManager.getInstance().removeAutoPotionItem(player);
					AutoUseTaskManager.getInstance().removeAutoPetPotionItem(player);
				}
			}
			
			// auto skill
			if (skill != null)
			{
				if (skill.hasNegativeEffect())
				{
					AutoUseTaskManager.getInstance().removeAutoSkill(player, skill.getId());
				}
				else
				{
					AutoUseTaskManager.getInstance().removeAutoBuff(player, skill.getId());
				}
			}
			else // action
			{
				AutoUseTaskManager.getInstance().removeAutoAction(player, shortcut.getId());
			}
			return;
		}
		
		// start
		if ((item != null) && !item.isPotion())
		{
			// auto supply
			if (GeneralConfig.ENABLE_AUTO_ITEM)
			{
				AutoUseTaskManager.getInstance().addAutoSupplyItem(player, item.getId());
			}
		}
		else
		{
			// auto potion
			if (_page == 23)
			{
				if (_slot == 1)
				{
					if (GeneralConfig.ENABLE_AUTO_POTION && (item != null) && item.isPotion())
					{
						AutoUseTaskManager.getInstance().setAutoPotionItem(player, item.getId());
						return;
					}
				}
				else if (_slot == 2)
				{
					if (GeneralConfig.ENABLE_AUTO_PET_POTION && (item != null) && item.isPotion())
					{
						AutoUseTaskManager.getInstance().setAutoPetPotionItem(player, item.getId());
						return;
					}
				}
			}
			
			// auto skill
			if (GeneralConfig.ENABLE_AUTO_SKILL && (skill != null))
			{
				if (skill.hasNegativeEffect())
				{
					AutoUseTaskManager.getInstance().addAutoSkill(player, skill.getId());
				}
				else
				{
					AutoUseTaskManager.getInstance().addAutoBuff(player, skill.getId());
				}
				return;
			}
			
			// action
			AutoUseTaskManager.getInstance().addAutoAction(player, shortcut.getId());
		}
	}
}
