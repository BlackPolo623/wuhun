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
package org.l2jmobius.gameserver.network.clientpackets.pet;

import java.util.List;
import java.util.Map.Entry;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.enums.EvolveLevel;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.PetTypeData;
import org.l2jmobius.gameserver.model.PetData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author Berezkin Nikolay, Mobius
 */
public class ExEvolvePet extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getPlayer();
		if (activeChar == null)
		{
			return;
		}
		
		final Pet pet = activeChar.getPet();
		if (pet == null)
		{
			return;
		}
		
		if (!activeChar.isMounted() && !pet.isDead() && !activeChar.isDead() && !pet.isHungry() && !activeChar.isControlBlocked() && !activeChar.isInDuel() && !activeChar.isSitting() && !activeChar.isFishing() && !activeChar.isInCombat() && !pet.isInCombat())
		{
			final boolean isAbleToEvolveLevel1 = (pet.getLevel() >= 40) && (pet.getEvolveLevel() == EvolveLevel.None.ordinal());
			final boolean isAbleToEvolveLevel2 = (pet.getLevel() >= 76) && (pet.getEvolveLevel() == EvolveLevel.First.ordinal());
			
			if (isAbleToEvolveLevel1 && (activeChar.destroyItemByItemId(ItemProcessType.FEE, 94096, 1, null, true) || activeChar.destroyItemByItemId(ItemProcessType.FEE, 103391, 1, null, true)))
			{
				doEvolve(activeChar, pet, EvolveLevel.First);
			}
			else if (isAbleToEvolveLevel2 && (activeChar.destroyItemByItemId(ItemProcessType.FEE, 94117, 1, null, true) || activeChar.destroyItemByItemId(ItemProcessType.FEE, 103392, 1, null, true)))
			{
				doEvolve(activeChar, pet, EvolveLevel.Second);
			}
		}
		else
		{
			activeChar.sendMessage("You can't evolve in this time."); // TODO: Proper system messages.
		}
	}
	
	private void doEvolve(Player activeChar, Pet pet, EvolveLevel evolveLevel)
	{
		final Item controlItem = pet.getControlItem();
		pet.unSummon(activeChar);
		final List<PetData> pets = PetDataTable.getInstance().getPetDatasByEvolve(controlItem.getId(), evolveLevel);
		final PetData targetPet = pets.get(Rnd.get(pets.size()));
		final PetData petData = PetDataTable.getInstance().getPetData(targetPet.getNpcId());
		if ((petData == null) || (petData.getNpcId() == -1))
		{
			return;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(evolveLevel == EvolveLevel.Second ? pet.getId() + 2 : petData.getNpcId());
		final Pet evolved = Pet.spawnPet(npcTemplate, activeChar, controlItem);
		if (evolved == null)
		{
			return;
		}
		
		if (evolveLevel == EvolveLevel.First)
		{
			final Entry<Integer, SkillHolder> skillType = PetTypeData.getInstance().getRandomSkill();
			final String name = PetTypeData.getInstance().getNamePrefix(skillType.getKey()) + " " + PetTypeData.getInstance().getRandomName();
			evolved.addSkill(skillType.getValue().getSkill());
			evolved.setName(name);
			PetDataTable.getInstance().setPetName(controlItem.getObjectId(), name);
		}
		
		activeChar.setPet(evolved);
		evolved.setShowSummonAnimation(true);
		evolved.setEvolveLevel(evolveLevel);
		evolved.setRunning();
		evolved.storeEvolvedPets(evolveLevel.ordinal(), evolved.getPetData().getIndex(), controlItem.getObjectId());
		controlItem.setEnchantLevel(evolved.getLevel());
		evolved.spawnMe(pet.getX(), pet.getY(), pet.getZ());
		evolved.startFeed();
	}
}
