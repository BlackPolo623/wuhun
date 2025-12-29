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
package org.l2jmobius.gameserver.model.actor.enums.player;

import static org.l2jmobius.gameserver.model.actor.enums.player.RankingOlympiadScope.SELF;
import static org.l2jmobius.gameserver.model.actor.enums.player.RankingOlympiadScope.TOP_100;
import static org.l2jmobius.gameserver.model.actor.enums.player.RankingOlympiadScope.TOP_50;

/**
 * @author Berezkin Nikolay
 */
public enum RankingOlympiadCategory
{
	SERVER,
	CLASS;
	
	public RankingOlympiadScope getScopeByGroup(int id)
	{
		switch (this)
		{
			case SERVER:
			{
				return id == 0 ? TOP_100 : SELF;
			}
			case CLASS:
			{
				return id == 0 ? TOP_50 : SELF;
			}
			default:
			{
				return null;
			}
		}
	}
}
