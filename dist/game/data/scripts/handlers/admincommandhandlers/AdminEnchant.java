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
package handlers.admincommandhandlers;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.EnchantItemGroupsData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * @author CostyKiller
 */
public class AdminEnchant implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_seteh", // 6  頭盔
		"admin_setec", // 10 胸甲
		"admin_seteg", // 9  手套
		"admin_setel", // 11 腿甲
		"admin_seteb", // 12 鞋子
		"admin_setew", // 7  武器
		"admin_setes", // 8  副手/盾
		"admin_setle", // 1  右耳環
		"admin_setre", // 2  左耳環
		"admin_setlf", // 4  右戒指
		"admin_setrf", // 5  左戒指
		"admin_seten", // 3  項鍊
		"admin_setun", // 0  內衣
		"admin_setba", // 13 披風
		"admin_setbe", //    腰帶
		"admin_sethr", // 2  頭飾1
		"admin_seth2", // 3  頭飾2
		"admin_seta1", // 17 壺精1
		"admin_seta2", // 18 壺精2
		"admin_seta3", // 19 壺精3
		"admin_seta4", // 20 壺精4
		"admin_seta5", // 21 壺精5
		"admin_setd1", // 22 護身符1
		"admin_setd2", // 23 護身符2
		"admin_setd3", // 24 護身符3
		"admin_setd4", // 25 護身符4
		"admin_setd5", // 26 護身符5
		"admin_setd6", // 27 護身符6
		"admin_enchant"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_enchant"))
		{
			showMainPage(activeChar);
		}
		else
		{
			int slot = -1;
			if (command.startsWith("admin_seteh"))
			{
				slot = Inventory.PAPERDOLL_HEAD;
			}
			else if (command.startsWith("admin_setec"))
			{
				slot = Inventory.PAPERDOLL_CHEST;
			}
			else if (command.startsWith("admin_seteg"))
			{
				slot = Inventory.PAPERDOLL_GLOVES;
			}
			else if (command.startsWith("admin_seteb"))
			{
				slot = Inventory.PAPERDOLL_FEET;
			}
			else if (command.startsWith("admin_setel"))
			{
				slot = Inventory.PAPERDOLL_LEGS;
			}
			else if (command.startsWith("admin_setew"))
			{
				slot = Inventory.PAPERDOLL_RHAND;
			}
			else if (command.startsWith("admin_setes"))
			{
				slot = Inventory.PAPERDOLL_LHAND;
			}
			else if (command.startsWith("admin_setle"))
			{
				slot = Inventory.PAPERDOLL_LEAR;
			}
			else if (command.startsWith("admin_setre"))
			{
				slot = Inventory.PAPERDOLL_REAR;
			}
			else if (command.startsWith("admin_setlf"))
			{
				slot = Inventory.PAPERDOLL_LFINGER;
			}
			else if (command.startsWith("admin_setrf"))
			{
				slot = Inventory.PAPERDOLL_RFINGER;
			}
			else if (command.startsWith("admin_seten"))
			{
				slot = Inventory.PAPERDOLL_NECK;
			}
			else if (command.startsWith("admin_setun"))
			{
				slot = Inventory.PAPERDOLL_UNDER;
			}
			else if (command.startsWith("admin_setba"))
			{
				slot = Inventory.PAPERDOLL_CLOAK;
			}
			else if (command.startsWith("admin_setbe"))
			{
				slot = Inventory.PAPERDOLL_BELT;
			}
			else if (command.startsWith("admin_sethr"))
			{
				slot = Inventory.PAPERDOLL_HAIR;
			}
			else if (command.startsWith("admin_seth2"))
			{
				slot = Inventory.PAPERDOLL_HAIR2;
			}
			else if (command.startsWith("admin_seta1"))
			{
				slot = Inventory.PAPERDOLL_AGATHION1;
			}
			else if (command.startsWith("admin_seta2"))
			{
				slot = Inventory.PAPERDOLL_AGATHION2;
			}
			else if (command.startsWith("admin_seta3"))
			{
				slot = Inventory.PAPERDOLL_AGATHION3;
			}
			else if (command.startsWith("admin_seta4"))
			{
				slot = Inventory.PAPERDOLL_AGATHION4;
			}
			else if (command.startsWith("admin_seta5"))
			{
				slot = Inventory.PAPERDOLL_AGATHION5;
			}
			else if (command.startsWith("admin_setd1"))
			{
				slot = Inventory.PAPERDOLL_DECO1;
			}
			else if (command.startsWith("admin_setd2"))
			{
				slot = Inventory.PAPERDOLL_DECO2;
			}
			else if (command.startsWith("admin_setd3"))
			{
				slot = Inventory.PAPERDOLL_DECO3;
			}
			else if (command.startsWith("admin_setd4"))
			{
				slot = Inventory.PAPERDOLL_DECO4;
			}
			else if (command.startsWith("admin_setd5"))
			{
				slot = Inventory.PAPERDOLL_DECO5;
			}
			else if (command.startsWith("admin_setd6"))
			{
				slot = Inventory.PAPERDOLL_DECO6;
			}

			if (slot != -1)
			{
				try
				{
					final int ench = Integer.parseInt(command.substring(12));
					
					// check value
					if (ench < 0)
					{
						activeChar.sendSysMessage("需要大於0");
					}
					else
					{
						setEnchant(activeChar, ench, slot);
					}
				}
				catch (StringIndexOutOfBoundsException e)
				{
					activeChar.sendSysMessage("Please specify a new enchant value.");
				}
				catch (NumberFormatException e)
				{
					activeChar.sendSysMessage("Please specify a valid new enchant value.");
				}
			}
			
			// show the enchant menu after an action
			showMainPage(activeChar);
		}
		
		return true;
	}
	
	private void setEnchant(Player activeChar, int ench, int slot)
	{
		// Get the target.
		final Player player = activeChar.getTarget() != null ? activeChar.getTarget().asPlayer() : activeChar;
		if (player == null)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		// Now we need to find the equipped weapon of the targeted character...
		Item itemInstance = null;
		
		// Only attempt to enchant if there is a weapon equipped.
		final Item paperdollInstance = player.getInventory().getPaperdollItem(slot);
		if ((paperdollInstance != null) && (paperdollInstance.getLocationSlot() == slot))
		{
			itemInstance = paperdollInstance;
		}
		
		if (itemInstance != null)
		{
			final int curEnchant = itemInstance.getEnchantLevel();
			
			// Set enchant value.
			int enchant = ench;
			if (PlayerConfig.OVER_ENCHANT_PROTECTION && !player.isGM())
			{
				if (itemInstance.isWeapon())
				{
					if (enchant > EnchantItemGroupsData.getInstance().getMaxWeaponEnchant())
					{
						activeChar.sendSysMessage("Maximum enchantment for weapon items is " + EnchantItemGroupsData.getInstance().getMaxWeaponEnchant() + ".");
						enchant = EnchantItemGroupsData.getInstance().getMaxWeaponEnchant();
					}
				}
				else if (itemInstance.getTemplate().getType2() == ItemTemplate.TYPE2_ACCESSORY)
				{
					if (enchant > EnchantItemGroupsData.getInstance().getMaxAccessoryEnchant())
					{
						activeChar.sendSysMessage("Maximum enchantment for accessory items is " + EnchantItemGroupsData.getInstance().getMaxAccessoryEnchant() + ".");
						enchant = EnchantItemGroupsData.getInstance().getMaxAccessoryEnchant();
					}
				}
				else if (enchant > EnchantItemGroupsData.getInstance().getMaxArmorEnchant())
				{
					activeChar.sendSysMessage("Maximum enchantment for armor items is " + EnchantItemGroupsData.getInstance().getMaxArmorEnchant() + ".");
					enchant = EnchantItemGroupsData.getInstance().getMaxArmorEnchant();
				}
			}
			
			player.getInventory().unEquipItemInSlot(slot);
			itemInstance.setEnchantLevel(enchant);
			player.getInventory().equipItem(itemInstance);
			
			// Send packets.
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(itemInstance);
			player.sendInventoryUpdate(iu);
			player.broadcastUserInfo();
			
			// Information.
			activeChar.sendSysMessage("Changed enchantment of " + player.getName() + "'s " + itemInstance.getTemplate().getName() + " from " + curEnchant + " to " + enchant + ".");
			player.sendMessage("Admin has changed the enchantment of your " + itemInstance.getTemplate().getName() + " from " + curEnchant + " to " + enchant + ".");
		}
	}
	
	private void showMainPage(Player activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "enchant.htm");
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
