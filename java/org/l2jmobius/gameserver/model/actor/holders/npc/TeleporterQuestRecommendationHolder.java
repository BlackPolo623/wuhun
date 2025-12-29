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
package org.l2jmobius.gameserver.model.actor.holders.npc;

/**
 * @author Mobius
 */
public class TeleporterQuestRecommendationHolder
{
	private final int _npcId;
	private final String _questName;
	private final int[] _conditions; // -1 = all conditions
	private final String _html;
	
	public TeleporterQuestRecommendationHolder(int npcId, String questName, int[] conditions, String html)
	{
		_npcId = npcId;
		_questName = questName;
		_conditions = conditions;
		_html = html;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public String getQuestName()
	{
		return _questName;
	}
	
	public int[] getConditions()
	{
		return _conditions;
	}
	
	public String getHtml()
	{
		return _html;
	}
}
