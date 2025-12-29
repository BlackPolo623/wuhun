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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

public class BlessedSpiritShot implements IItemHandler
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
		final Item weaponInst = player.getActiveWeaponInstance();
		final Weapon weaponItem = player.getActiveWeaponItem();
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		if (skills == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		final int itemId = item.getId();
		
		// Check if Blessed SpiritShot can be used
		if ((weaponInst == null) || (weaponItem.getSpiritShotCount() == 0))
		{
			if (!player.getAutoSoulShot().contains(itemId))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SPIRITSHOTS);
			}
			
			return false;
		}
		
		// Check if Blessed SpiritShot is already active (it can be charged over SpiritShot)
		if (player.isChargedShot(ShotType.BLESSED_SPIRITSHOTS))
		{
			return summonUseItem(playable, item);
		}
		
		// Consume Blessed SpiritShot if player has enough of them
		if (!player.destroyItem(ItemProcessType.NONE, item.getObjectId(), weaponItem.getSpiritShotCount(), null, false))
		{
			if (!player.disableAutoShot(itemId))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SPIRITSHOT_FOR_THAT);
			}
			
			return false;
		}
		
		// Charge Spirit shot
		player.chargeShot(ShotType.BLESSED_SPIRITSHOTS);
		
		// Send message to client
		if (!player.getAutoSoulShot().contains(item.getId()))
		{
			player.sendPacket(SystemMessageId.YOUR_SPIRITSHOT_HAS_BEEN_ENABLED);
		}
		
		// Visual effect change if player has equipped Sapphire level 3 or higher
		if (player.getActiveShappireJewel() != null)
		{
			player.broadcastSkillPacket(new MagicSkillUse(player, player, player.getActiveShappireJewel().getSkillId(), player.getActiveShappireJewel().getSkillLevel(), 0, 0), player);
		}
		else
		{
			skills.forEach(holder -> player.broadcastSkillPacket(new MagicSkillUse(player, player, holder.getSkillId(), holder.getSkillLevel(), 0, 0), player));
		}
		
		return true;
	}
	
	private boolean summonUseItem(Playable playable, Item item)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}
		
		final Player activeOwner = playable.asPlayer();
		if (!activeOwner.hasSummon())
		{
			activeOwner.sendPacket(SystemMessageId.SERVITORS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
			return false;
		}
		
		final Summon pet = playable.getPet();
		if ((pet != null) && pet.isDead())
		{
			activeOwner.sendPacket(SystemMessageId.A_DEAD_SERVITOR_CANNOT_USE_SOULSHOTS_SPIRITSHOTS);
			return false;
		}
		
		final List<Summon> aliveServitor = new ArrayList<>();
		for (Summon s : playable.getServitors().values())
		{
			if (!s.isDead())
			{
				aliveServitor.add(s);
			}
		}
		
		if ((pet == null) && aliveServitor.isEmpty())
		{
			activeOwner.sendPacket(SystemMessageId.A_DEAD_SERVITOR_CANNOT_USE_SOULSHOTS_SPIRITSHOTS);
			return false;
		}
		
		final int itemId = item.getId();
		final boolean isBlessed = ((itemId == 6647) || (itemId == 20334)); // TODO: Unhardcode these!
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		final ShotType shotType = isBlessed ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS;
		short shotConsumption = 0;
		if ((pet != null) && !pet.isChargedShot(shotType))
		{
			shotConsumption += pet.getSpiritShotsPerHit();
		}
		
		for (Summon servitors : aliveServitor)
		{
			if (!servitors.isChargedShot(shotType))
			{
				shotConsumption += servitors.getSpiritShotsPerHit();
			}
		}
		
		if (skills == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		final long shotCount = item.getCount();
		if (shotCount < shotConsumption)
		{
			// Not enough SpiritShots to use.
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SPIRITSHOTS_FOR_THE_SERVITOR);
			}
			
			return false;
		}
		
		if (!activeOwner.destroyItem(ItemProcessType.NONE, item.getObjectId(), shotConsumption, null, false))
		{
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SPIRITSHOTS_FOR_THE_SERVITOR);
			}
			
			return false;
		}
		
		// Pet uses the power of spirit.
		if ((pet != null) && !pet.isChargedShot(shotType))
		{
			activeOwner.sendMessage(isBlessed ? "Your pet uses blessed spiritshot." : "Your pet uses spiritshot."); // activeOwner.sendPacket(SystemMessageId.YOUR_PET_USES_SPIRITSHOT);
			pet.chargeShot(shotType);
			
			// Visual effect change if player has equipped Sapphire level 3 or higher
			if (activeOwner.getActiveShappireJewel() != null)
			{
				activeOwner.broadcastSkillPacket(new MagicSkillUse(pet, pet, activeOwner.getActiveShappireJewel().getSkillId(), activeOwner.getActiveShappireJewel().getSkillLevel(), 0, 0), pet);
			}
			else
			{
				skills.forEach(holder -> activeOwner.broadcastSkillPacket(new MagicSkillUse(pet, pet, holder.getSkillId(), holder.getSkillLevel(), 0, 0), pet));
			}
		}
		
		aliveServitor.forEach(s ->
		{
			if (!s.isChargedShot(shotType))
			{
				activeOwner.sendMessage(isBlessed ? "Your servitor uses blessed spiritshot." : "Your servitor uses spiritshot."); // activeOwner.sendPacket(SystemMessageId.YOUR_PET_USES_SPIRITSHOT);
				s.chargeShot(shotType);
				
				// Visual effect change if player has equipped Sapphire level 3 or higher
				if (activeOwner.getActiveShappireJewel() != null)
				{
					activeOwner.broadcastSkillPacket(new MagicSkillUse(s, s, activeOwner.getActiveShappireJewel().getSkillId(), activeOwner.getActiveShappireJewel().getSkillLevel(), 0, 0), s);
				}
				else
				{
					skills.forEach(holder -> activeOwner.broadcastSkillPacket(new MagicSkillUse(s, s, holder.getSkillId(), holder.getSkillLevel(), 0, 0), s));
				}
			}
		});
		
		return true;
	}
}
