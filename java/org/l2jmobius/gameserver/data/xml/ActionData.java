/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.ActionDataHolder;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author UnAfraid
 */
public class ActionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ActionData.class.getName());
	
	private final Map<Integer, ActionDataHolder> _actionData = new HashMap<>();
	private final Map<Integer, Integer> _actionSkillsData = new HashMap<>();
	
	protected ActionData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_actionData.clear();
		_actionSkillsData.clear();
		parseDatapackFile("data/ActionData.xml");
		
		for (ActionDataHolder holder : _actionData.values())
		{
			if (holder.getHandler().equals("PetSkillUse") || holder.getHandler().equals("ServitorSkillUse"))
			{
				_actionSkillsData.put(holder.getOptionId(), holder.getId());
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _actionData.size() + " player actions.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "action", actionNode ->
		{
			final ActionDataHolder holder = new ActionDataHolder(new StatSet(parseAttributes(actionNode)));
			_actionData.put(holder.getId(), holder);
		}));
	}
	
	/**
	 * Retrieves the action data associated with the specified action ID.
	 * @param id the unique identifier of the action
	 * @return the {@link ActionDataHolder} associated with the given ID, or {@code null} if not found
	 */
	public ActionDataHolder getActionData(int id)
	{
		return _actionData.get(id);
	}
	
	/**
	 * Retrieves the action ID associated with a specific skill ID.
	 * @param skillId the unique identifier of the skill
	 * @return the action ID associated with the specified skill ID, or {@code -1} if not found
	 */
	public int getSkillActionId(int skillId)
	{
		return _actionSkillsData.getOrDefault(skillId, -1);
	}
	
	/**
	 * Retrieves a list of all action IDs.
	 * @return an array of all action IDs available in the action data
	 */
	public int[] getActionIdList()
	{
		return _actionData.keySet().stream().mapToInt(Number::intValue).toArray();
	}
	
	public static ActionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ActionData INSTANCE = new ActionData();
	}
}
