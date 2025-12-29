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
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.ActionType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Mobius
 */
public class RequestAutoSoulShot extends ClientPacket
{
	private int _itemId;
	private boolean _enable;
	private int _type;
	
	@Override
	protected void readImpl()
	{
		_itemId = readInt();
		_enable = readInt() == 1;
		_type = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!player.isInStoreMode() && (player.getActiveRequester() == null) && !player.isDead())
		{
			final Item item = player.getInventory().getItemByItemId(_itemId);
			if (item == null)
			{
				return;
			}
			
			if (_enable)
			{
				if (!player.getInventory().canManipulateWithItemId(item.getId()))
				{
					player.sendMessage("Cannot use this item.");
					return;
				}
				
				if (isSummonShot(item.getTemplate()))
				{
					if (player.hasSummon())
					{
						final boolean isSoulshot = item.getEtcItem().getDefaultAction() == ActionType.SUMMON_SOULSHOT;
						final boolean isSpiritshot = item.getEtcItem().getDefaultAction() == ActionType.SUMMON_SPIRITSHOT;
						if (isSoulshot)
						{
							int soulshotCount = 0;
							final Summon pet = player.getPet();
							if (pet != null)
							{
								soulshotCount += pet.getSoulShotsPerHit();
							}
							
							for (Summon servitor : player.getServitors().values())
							{
								soulshotCount += servitor.getSoulShotsPerHit();
							}
							
							if (soulshotCount > item.getCount())
							{
								player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_SERVITOR);
								return;
							}
						}
						else if (isSpiritshot)
						{
							int spiritshotCount = 0;
							final Summon pet = player.getPet();
							if (pet != null)
							{
								spiritshotCount += pet.getSpiritShotsPerHit();
							}
							
							for (Summon servitor : player.getServitors().values())
							{
								spiritshotCount += servitor.getSpiritShotsPerHit();
							}
							
							if (spiritshotCount > item.getCount())
							{
								player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_SERVITOR);
								return;
							}
						}
						
						// Activate shots
						player.addAutoSoulShot(_itemId);
						player.sendPacket(new ExAutoSoulShot(_itemId, _enable, _type));
						
						// Recharge summon's shots
						final Summon pet = player.getPet();
						if (pet != null)
						{
							// Send message
							if (!pet.isChargedShot(item.getTemplate().getDefaultAction() == ActionType.SUMMON_SOULSHOT ? ShotType.SOULSHOTS : ((item.getId() == 6647) || (item.getId() == 20334)) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS))
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_ACTIVATED);
								sm.addItemName(item);
								player.sendPacket(sm);
							}
							
							// Charge
							pet.rechargeShots(isSoulshot, isSpiritshot, false);
						}
						
						for (Summon summon : player.getServitors().values())
						{
							// Send message
							if (!summon.isChargedShot(item.getTemplate().getDefaultAction() == ActionType.SUMMON_SOULSHOT ? ShotType.SOULSHOTS : ((item.getId() == 6647) || (item.getId() == 20334)) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS))
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_ACTIVATED);
								sm.addItemName(item);
								player.sendPacket(sm);
							}
							
							// Charge
							summon.rechargeShots(isSoulshot, isSpiritshot, false);
						}
					}
					else
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR_AND_THEREFORE_CANNOT_USE_THE_AUTOMATIC_USE_FUNCTION);
					}
				}
				else if (isPlayerShot(item.getTemplate()))
				{
					final boolean isSoulshot = item.getEtcItem().getDefaultAction() == ActionType.SOULSHOT;
					final boolean isSpiritshot = item.getEtcItem().getDefaultAction() == ActionType.SPIRITSHOT;
					final boolean isFishingshot = item.getEtcItem().getDefaultAction() == ActionType.FISHINGSHOT;
					if (player.getActiveWeaponItem() == player.getFistsWeaponItem())
					{
						player.sendPacket(isSoulshot ? SystemMessageId.THE_SOULSHOT_YOU_ARE_ATTEMPTING_TO_USE_DOES_NOT_MATCH_THE_GRADE_OF_YOUR_EQUIPPED_WEAPON : SystemMessageId.YOUR_SPIRITSHOT_DOES_NOT_MATCH_THE_WEAPON_S_GRADE);
						return;
					}
					
					// Activate shots
					player.addAutoSoulShot(_itemId);
					player.sendPacket(new ExAutoSoulShot(_itemId, _enable, _type));
					
					// Send message
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_ACTIVATED);
					sm.addItemName(item);
					player.sendPacket(sm);
					
					// Recharge player's shots
					player.rechargeShots(isSoulshot, isSpiritshot, isFishingshot);
				}
			}
			else
			{
				// Cancel auto shots
				player.removeAutoSoulShot(_itemId);
				player.sendPacket(new ExAutoSoulShot(_itemId, _enable, _type));
				
				// Send message
				final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
				sm.addItemName(item);
				player.sendPacket(sm);
			}
		}
	}
	
	public static boolean isPlayerShot(ItemTemplate item)
	{
		switch (item.getDefaultAction())
		{
			case SPIRITSHOT:
			case SOULSHOT:
			case FISHINGSHOT:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	public static boolean isSummonShot(ItemTemplate item)
	{
		switch (item.getDefaultAction())
		{
			case SUMMON_SPIRITSHOT:
			case SUMMON_SOULSHOT:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
}
