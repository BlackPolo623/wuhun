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
package org.l2jmobius.gameserver.handler;

import java.util.Collection;

import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * BOSS Drop Handler Interface
 * <p>
 * Allows custom scripts to intercept and handle BOSS drops.
 * This interface provides a clean separation between core and custom scripts,
 * avoiding circular dependencies.
 * </p>
 * <p>
 * Usage Example: Boss Auction System, Boss Drop Distribution, etc.
 * </p>
 * @author 黑普羅
 */
public interface IBossDropHandler
{
	/**
	 * Handles BOSS drop items.
	 * <p>
	 * This method is called after drop calculation but before items are generated on the ground.
	 * If this method returns true, the items will NOT be dropped on the ground.
	 * </p>
	 * @param boss The killed BOSS
	 * @param killer The player who killed the BOSS (can be null)
	 * @param drops The calculated drop items
	 * @return true if the handler processed the drops (items won't drop on ground), false for normal drop behavior
	 */
	boolean handleBossDrop(Attackable boss, Player killer, Collection<ItemHolder> drops);

	/**
	 * Checks if this BOSS should be handled by this handler.
	 * <p>
	 * This method is called first to determine if the handler wants to process this BOSS.
	 * If returns false, the drops will be processed normally.
	 * </p>
	 * @param bossId The NPC ID of the BOSS
	 * @return true if this handler wants to process this BOSS, false otherwise
	 */
	boolean shouldHandle(int bossId);
}
