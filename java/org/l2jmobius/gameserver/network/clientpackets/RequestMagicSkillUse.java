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

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.transform.Transform;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * @author Mobius
 */
public class RequestMagicSkillUse extends ClientPacket
{
	private int _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_magicId = readInt(); // Identifier of the used skill
		_ctrlPressed = readInt() != 0; // True if it's a ForceAttack : Ctrl pressed
		_shiftPressed = readByte() != 0; // True if Shift pressed
	}
	
	@Override
	protected void runImpl()
	{
		// Get the current Player of the player.
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Consider skill replacements.
		_magicId = player.getReplacementSkill(_magicId);
		
		// Get the level of the used skill.
		Skill skill = player.getKnownSkill(_magicId);
		if (skill == null)
		{
			if ((_magicId == CommonSkill.HAIR_ACCESSORY_SET.getId()) || ((_magicId > 1565) && (_magicId < 1570))) // subClass change SkillTree
			{
				skill = SkillData.getInstance().getSkill(_magicId, 1);
			}
			else // Check for known pet skill.
			{
				Playable pet = null;
				if (player.hasServitors())
				{
					for (Summon summon : player.getServitors().values())
					{
						skill = summon.getKnownSkill(_magicId);
						if (skill != null)
						{
							pet = summon;
							break;
						}
					}
				}
				
				if ((skill == null) && player.hasPet())
				{
					pet = player.getPet();
					skill = pet.getKnownSkill(_magicId);
				}
				
				if ((skill != null) && (pet != null))
				{
					player.onActionRequest();
					pet.setTarget(null);
					pet.useMagic(skill, null, _ctrlPressed, false);
					return;
				}
			}
			
			// Custom skill with displayId.
			if (skill == null)
			{
				skill = player.getCustomSkill(_magicId);
			}
			
			if (skill == null)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Skill is blocked from player use.
		if (skill.isBlockActionUseSkill())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Avoid Use of Skills in AirShip.
		if (player.isInAirShip())
		{
			player.sendPacket(SystemMessageId.THIS_ACTION_IS_PROHIBITED_WHILE_MOUNTED_OR_ON_AN_AIRSHIP);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Do not cast non transform skills when transformed.
		final Transform transform = player.getTransformation();
		if ((transform != null) && !transform.canUseWeaponStats() && !player.hasTransformSkill(skill))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.onActionRequest();
		player.useMagic(skill, null, _ctrlPressed, _shiftPressed);
	}
}
