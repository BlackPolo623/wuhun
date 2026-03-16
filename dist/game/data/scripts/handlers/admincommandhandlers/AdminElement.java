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

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Admin attribute command handler.
 * 支援部位：頭盔/胸甲/腿甲/手套/鞋子/武器/副手/項鍊/右耳環/左耳環/右戒指/左戒指
 * 指令格式：//setlXX <屬性> <數值>
 * 屬性名稱：Fire / Water / Wind / Earth / Dark / Holy / None(清除)
 * 數值無上限（輸入 0 或 None 可清除）
 */
public class AdminElement implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_setlh",    // 頭盔
		"admin_setlc",    // 胸甲
		"admin_setll",    // 腿甲
		"admin_setlg",    // 手套
		"admin_setlb",    // 鞋子
		"admin_setlw",    // 武器
		"admin_setls",    // 副手/盾
		"admin_setln",    // 項鍊
		"admin_setlrear", // 右耳環
		"admin_setllear", // 左耳環
		"admin_setlrring",// 右戒指
		"admin_setllring" // 左戒指
	};

	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		// 取出純指令名稱（空格前的部分），用 equals 精確比對，避免 startsWith 前綴誤判
		final String cmd = command.contains(" ") ? command.substring(0, command.indexOf(' ')) : command;

		int armorType = -1;
		switch (cmd)
		{
			case "admin_setlh":
				armorType = Inventory.PAPERDOLL_HEAD;
				break;
			case "admin_setlc":
				armorType = Inventory.PAPERDOLL_CHEST;
				break;
			case "admin_setlg":
				armorType = Inventory.PAPERDOLL_GLOVES;
				break;
			case "admin_setlb":
				armorType = Inventory.PAPERDOLL_FEET;
				break;
			case "admin_setll":
				armorType = Inventory.PAPERDOLL_LEGS;
				break;
			case "admin_setlw":
				armorType = Inventory.PAPERDOLL_RHAND;
				break;
			case "admin_setls":
				armorType = Inventory.PAPERDOLL_LHAND;
				break;
			case "admin_setln":
				armorType = Inventory.PAPERDOLL_NECK;
				break;
			// 耳環：優先抓左耳（LEAR），再找右耳（REAR）
			case "admin_setlrear":
				armorType = Inventory.PAPERDOLL_LEAR;
				if (activeChar.getTarget() != null && activeChar.getTarget().isPlayer())
				{
					final Item lear = activeChar.getTarget().asPlayer().getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR);
					final Item rear = activeChar.getTarget().asPlayer().getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR);
					// 右耳環按鈕：有右耳先用右耳，否則用左耳
					armorType = (rear != null) ? Inventory.PAPERDOLL_REAR : Inventory.PAPERDOLL_LEAR;
				}
				break;
			case "admin_setllear":
				armorType = Inventory.PAPERDOLL_LEAR;
				if (activeChar.getTarget() != null && activeChar.getTarget().isPlayer())
				{
					final Item lear2 = activeChar.getTarget().asPlayer().getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR);
					final Item rear2 = activeChar.getTarget().asPlayer().getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR);
					// 左耳環按鈕：有兩隻就用左耳，只有一隻也用左耳
					armorType = (lear2 != null) ? Inventory.PAPERDOLL_LEAR : (rear2 != null ? Inventory.PAPERDOLL_REAR : Inventory.PAPERDOLL_LEAR);
				}
				break;
			// 戒指：優先抓左手（LFINGER），再找右手（RFINGER）
			case "admin_setlrring":
				armorType = Inventory.PAPERDOLL_RFINGER;
				if (activeChar.getTarget() != null && activeChar.getTarget().isPlayer())
				{
					final Item lring = activeChar.getTarget().asPlayer().getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER);
					final Item rring = activeChar.getTarget().asPlayer().getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER);
					// 右戒指按鈕：有右戒先用右戒，否則用左戒
					armorType = (rring != null) ? Inventory.PAPERDOLL_RFINGER : Inventory.PAPERDOLL_LFINGER;
				}
				break;
			case "admin_setllring":
				armorType = Inventory.PAPERDOLL_LFINGER;
				if (activeChar.getTarget() != null && activeChar.getTarget().isPlayer())
				{
					final Item lring2 = activeChar.getTarget().asPlayer().getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER);
					final Item rring2 = activeChar.getTarget().asPlayer().getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER);
					// 左戒指按鈕：有兩隻就用左戒，只有一隻也用左戒
					armorType = (lring2 != null) ? Inventory.PAPERDOLL_LFINGER : (rring2 != null ? Inventory.PAPERDOLL_RFINGER : Inventory.PAPERDOLL_LFINGER);
				}
				break;
		}

		if (armorType != -1)
		{
			try
			{
				final String[] args = command.split(" ");
				final AttributeType type = AttributeType.findByName(args[1]);
				final int value = Integer.parseInt(args[2]);
				if ((type == null) || (value < 0))
				{
					activeChar.sendSysMessage("用法：//setlXX <屬性名稱> <數值>  屬性：Fire/Water/Wind/Earth/Dark/Holy/None");
					return false;
				}

				setElement(activeChar, type, value, armorType);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("用法：//setlXX <屬性名稱> <數值>  屬性：Fire/Water/Wind/Earth/Dark/Holy/None");
				return false;
			}
		}

		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void setElement(Player activeChar, AttributeType type, int value, int armorType)
	{
		// get the target
		WorldObject target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}

		Player player = null;
		if (target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}

		Item itemInstance = null;

		final Item parmorInstance = player.getInventory().getPaperdollItem(armorType);
		if ((parmorInstance != null) && (parmorInstance.getLocationSlot() == armorType))
		{
			itemInstance = parmorInstance;
		}

		if (itemInstance != null)
		{
			String old;
			String current;
			final AttributeHolder element = itemInstance.getAttribute(type);
			if (element == null)
			{
				old = "None";
			}
			else
			{
				old = element.toString();
			}

			// set enchant value
			player.getInventory().unEquipItemInSlot(armorType);
			if (type == AttributeType.NONE)
			{
				itemInstance.clearAllAttributes();
			}
			else if (value < 1)
			{
				itemInstance.clearAttribute(type);
			}
			else
			{
				itemInstance.setAttribute(new AttributeHolder(type, value), true);
			}

			player.getInventory().equipItem(itemInstance);

			if (itemInstance.getAttributes() == null)
			{
				current = "None";
			}
			else
			{
				final AttributeHolder updated = itemInstance.getAttribute(type);
				current = updated != null ? updated.toString() : "None";
			}

			// send packets
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(itemInstance);
			player.sendInventoryUpdate(iu);

			// informations
			activeChar.sendSysMessage("已將 " + player.getName() + " 的 " + itemInstance.getTemplate().getName() + " 屬性從 " + old + " 改為 " + current + "。");
			if (player != activeChar)
			{
				player.sendMessage(activeChar.getName() + " 已將你的 " + itemInstance.getTemplate().getName() + " 屬性從 " + old + " 改為 " + current + "。");
			}
		}
		else
		{
			activeChar.sendSysMessage("目標該部位沒有裝備道具。");
		}
	}
}
