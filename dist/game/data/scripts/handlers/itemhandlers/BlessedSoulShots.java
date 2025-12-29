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

import org.l2jmobius.commons.util.Rnd;
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

/**
 * @author Mobius
 */
public class BlessedSoulShots implements IItemHandler
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
		
		// Check if Soul shot can be used
		if ((weaponInst == null) || (weaponItem.getSoulShotCount() == 0))
		{
			if (!player.getAutoSoulShot().contains(itemId))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SOULSHOTS);
			}
			
			return false;
		}
		
		// Check if Soul shot is already active
		if (player.isChargedShot(ShotType.BLESSED_SOULSHOTS))
		{
			return summonUseItem(playable, item);
		}
		
		// Consume Soul shots if player has enough of them
		int ssCount = weaponItem.getSoulShotCount();
		if ((weaponItem.getReducedSoulShot() > 0) && (Rnd.get(100) < weaponItem.getReducedSoulShotChance()))
		{
			ssCount = weaponItem.getReducedSoulShot();
		}
		
		if (!player.destroyItem(ItemProcessType.NONE, item.getObjectId(), ssCount, null, false))
		{
			if (!player.disableAutoShot(itemId))
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS);
			}
			
			return false;
		}
		
		// Charge soul shot
		player.chargeShot(ShotType.BLESSED_SOULSHOTS);
		
		// Send message to client
		if (!player.getAutoSoulShot().contains(item.getId()))
		{
			player.sendPacket(SystemMessageId.YOUR_SOULSHOTS_ARE_ENABLED);
		}
		
		// Visual effect change if player has equipped Ruby level 3 or higher
		if (player.getActiveRubyJewel() != null)
		{
			player.broadcastSkillPacket(new MagicSkillUse(player, player, player.getActiveRubyJewel().getSkillId(), player.getActiveRubyJewel().getSkillLevel(), 0, 0), player);
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
		final long shotCount = item.getCount();
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		short shotConsumption = 0;
		if ((pet != null) && !pet.isChargedShot(ShotType.SOULSHOTS))
		{
			shotConsumption += pet.getSoulShotsPerHit();
		}
		
		for (Summon servitors : aliveServitor)
		{
			if (!servitors.isChargedShot(ShotType.SOULSHOTS))
			{
				shotConsumption += servitors.getSoulShotsPerHit();
			}
		}
		
		if (skills == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		if (shotCount < shotConsumption)
		{
			// Not enough Soulshots to use.
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_SERVITOR);
			}
			
			return false;
		}
		
		// If the player doesn't have enough beast soulshot remaining, remove any auto soulshot task.
		if (!activeOwner.destroyItem(ItemProcessType.NONE, item.getObjectId(), shotConsumption, null, false))
		{
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_SERVITOR);
			}
			
			return false;
		}
		
		// Pet uses the power of spirit.
		if ((pet != null) && !pet.isChargedShot(ShotType.SOULSHOTS))
		{
			activeOwner.sendMessage("Your pet uses soulshot."); // activeOwner.sendPacket(SystemMessageId.YOUR_PET_USES_SPIRITSHOT);
			pet.chargeShot(ShotType.SOULSHOTS);
			
			// Visual effect change if player has equipped Ruby level 3 or higher
			if (activeOwner.getActiveRubyJewel() != null)
			{
				activeOwner.broadcastSkillPacket(new MagicSkillUse(pet, pet, activeOwner.getActiveRubyJewel().getSkillId(), activeOwner.getActiveRubyJewel().getSkillLevel(), 0, 0), pet);
			}
			else
			{
				skills.forEach(holder -> activeOwner.broadcastSkillPacket(new MagicSkillUse(pet, pet, holder.getSkillId(), holder.getSkillLevel(), 0, 0), pet));
			}
		}
		
		aliveServitor.forEach(s ->
		{
			if (!s.isChargedShot(ShotType.SOULSHOTS))
			{
				activeOwner.sendMessage("Your servitor uses soulshot."); // activeOwner.sendPacket(SystemMessageId.YOUR_PET_USES_SPIRITSHOT);
				s.chargeShot(ShotType.SOULSHOTS);
				
				// Visual effect change if player has equipped Ruby level 3 or higher
				if (activeOwner.getActiveRubyJewel() != null)
				{
					activeOwner.broadcastSkillPacket(new MagicSkillUse(s, s, activeOwner.getActiveRubyJewel().getSkillId(), activeOwner.getActiveRubyJewel().getSkillLevel(), 0, 0), s);
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
