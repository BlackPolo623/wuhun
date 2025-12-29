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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.enums.creature.Position;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * @author Nik, Mobius
 */
public class HitConditionBonusData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(HitConditionBonusData.class.getName());
	
	private int _frontBonus = 0;
	private int _sideBonus = 0;
	private int _backBonus = 0;
	private int _highBonus = 0;
	private int _lowBonus = 0;
	private int _darkBonus = 0;
	
	protected HitConditionBonusData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/stats/hitConditionBonus.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded hit condition bonuses.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		// Iterate through child elements of the root node.
		for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling())
		{
			// Get the value of the "val" attribute for each node.
			final NamedNodeMap attrs = d.getAttributes();
			
			// Set the corresponding bonus based on the node name.
			switch (d.getNodeName())
			{
				case "front":
				{
					_frontBonus = parseInteger(attrs, "val");
					break;
				}
				case "side":
				{
					_sideBonus = parseInteger(attrs, "val");
					break;
				}
				case "back":
				{
					_backBonus = parseInteger(attrs, "val");
					break;
				}
				case "high":
				{
					_highBonus = parseInteger(attrs, "val");
					break;
				}
				case "low":
				{
					_lowBonus = parseInteger(attrs, "val");
					break;
				}
				case "dark":
				{
					_darkBonus = parseInteger(attrs, "val");
					break;
				}
			}
		}
	}
	
	/**
	 * Calculates the condition bonus for the attacker against the target.
	 * @param attacker the attacking character.
	 * @param target the attacked character.
	 * @return the bonus multiplier of the attacker against the target.
	 */
	public double getConditionBonus(Creature attacker, Creature target)
	{
		double mod = 100d;
		
		// Apply height difference bonus.
		final int heightDifference = attacker.getZ() - target.getZ();
		if (heightDifference > 50)
		{
			mod += _highBonus;
		}
		else if (heightDifference < -50)
		{
			mod += _lowBonus;
		}
		
		// Apply night-time bonus.
		if (GameTimeTaskManager.getInstance().isNight())
		{
			mod += _darkBonus;
		}
		
		// Apply positional bonus.
		switch (Position.getPosition(attacker, target))
		{
			case SIDE:
			{
				mod += _sideBonus;
				break;
			}
			case BACK:
			{
				mod += _backBonus;
				break;
			}
			default:
			{
				mod += _frontBonus;
				break;
			}
		}
		
		// Return the final condition bonus multiplier, ensuring it is not less than zero.
		return Math.max(mod / 100d, 0);
	}
	
	public static HitConditionBonusData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HitConditionBonusData INSTANCE = new HitConditionBonusData();
	}
}
