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

import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author poltomb, Mobius
 */
public class AdminSummon implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
			{
					"admin_summon",
					"admin_summon2"
			};

	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		int id = 0;
		long count = 1;
		final String[] data = command.split(" ");
		if (data.length < 2)
		{
			activeChar.sendSysMessage(data[0].equals("admin_summon2") ? "使用方法: //summon2 [數量] <id>" : "使用方法: //summon <id> [數量]");
			return false;
		}

		try
		{
			if (data[0].equals("admin_summon2"))
			{
				count = Long.parseLong(data[1]);
				if (data.length > 2)
				{
					id = Integer.parseInt(data[2]);
				}
			}
			else // admin_summon
			{
				id = Integer.parseInt(data[1]);
				if (data.length > 2)
				{
					count = Long.parseLong(data[2]);
				}
			}
		}
		catch (NumberFormatException nfe)
		{
			activeChar.sendSysMessage("指令格式錯誤");
			return false;
		}

		// 判斷是創建物品還是召喚怪物
		if (id < 1000000)
		{
			// 創建物品
			createItem(activeChar, id, count);
		}
		else
		{
			id -= 1000000;
			activeChar.sendSysMessage("這是臨時召喚，怪物不會重生。即將召喚" + id );
			spawnMonster(activeChar, id, (int) count);
		}

		return true;
	}

	/**
	 * 創建物品
	 */
	private void createItem(Player activeChar, int itemId, long count)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			activeChar.sendSysMessage("找不到物品 ID: " + itemId);
			return;
		}

		if (count > 1)
		{
			activeChar.sendSysMessage("創建了 " + template.getName() + " x" + count);
		}
		else
		{
			activeChar.sendSysMessage("創建了 " + template.getName());
		}

		// 添加物品到玩家背包
		activeChar.addItem(ItemProcessType.NONE, itemId, count, activeChar, true);
	}

	/**
	 * 召喚怪物（臨時刷怪，不會重生）
	 */
	private void spawnMonster(Player activeChar, int npcId, int mobCount)
	{
		WorldObject target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}

		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		if (template == null)
		{
			activeChar.sendSysMessage("找不到 NPC ID: " + npcId);
			return;
		}

		if (!FakePlayersConfig.FAKE_PLAYERS_ENABLED && template.isFakePlayer())
		{
			activeChar.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
			return;
		}

		try
		{
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(target);
			spawn.setAmount(mobCount);
			spawn.setHeading(activeChar.getHeading());
			spawn.setRespawnDelay(0); // 臨時刷怪，重生時間設為 0

			if (activeChar.isInInstance())
			{
				spawn.setInstanceId(activeChar.getInstanceId());
			}

			SpawnTable.getInstance().addSpawn(spawn);
			spawn.init();
			spawn.stopRespawn(); // 停止重生

			spawn.getLastSpawn().broadcastInfo();
			activeChar.sendSysMessage("召喚了 " + template.getName() + " x" + mobCount);
		}
		catch (Exception e)
		{
			activeChar.sendSysMessage("召喚失敗: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}