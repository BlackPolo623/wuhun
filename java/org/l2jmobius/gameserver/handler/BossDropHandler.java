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
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * BOSS Drop Handler Manager
 * <p>
 * Manages the registration and invocation of BOSS drop handlers.
 * Custom scripts can register their handlers to intercept BOSS drops.
 * </p>
 * <p>
 * This design avoids circular dependencies by:
 * - Core defines the interface (IBossDropHandler)
 * - Core provides the registration mechanism (this class)
 * - Custom scripts implement the interface and register themselves
 * - Core calls the handler through the interface, not directly to custom code
 * </p>
 * @author 黑普羅
 */
public class BossDropHandler
{
	private static final Logger LOGGER = Logger.getLogger(BossDropHandler.class.getName());

	/** The registered BOSS drop handler (only one handler supported currently) */
	private static IBossDropHandler _handler = null;

	/**
	 * Private constructor to prevent instantiation.
	 */
	private BossDropHandler()
	{
	}

	/**
	 * Registers a BOSS drop handler.
	 * <p>
	 * This method should be called by custom scripts during initialization.
	 * Only one handler can be registered at a time. Registering a new handler
	 * will replace the previous one.
	 * </p>
	 * @param handler The handler to register
	 */
	public static void registerHandler(IBossDropHandler handler)
	{
		if (handler == null)
		{
			LOGGER.warning("Attempted to register a null BOSS drop handler.");
			return;
		}

		_handler = handler;
		LOGGER.info("BOSS Drop Handler registered: " + handler.getClass().getSimpleName());
	}

	/**
	 * Unregisters the current BOSS drop handler.
	 * <p>
	 * After calling this method, BOSS drops will be processed normally.
	 * </p>
	 */
	public static void unregisterHandler()
	{
		if (_handler != null)
		{
			LOGGER.info("BOSS Drop Handler unregistered: " + _handler.getClass().getSimpleName());
			_handler = null;
		}
	}

	/**
	 * Checks if a handler is registered.
	 * @return true if a handler is registered, false otherwise
	 */
	public static boolean hasHandler()
	{
		return _handler != null;
	}

	/**
	 * Handles BOSS drop through the registered handler.
	 * <p>
	 * This method is called by Attackable.doItemDrop() after drop calculation
	 * but before items are generated on the ground.
	 * </p>
	 * @param boss The killed BOSS
	 * @param killer The player who killed the BOSS (can be null)
	 * @param drops The calculated drop items
	 * @return true if the handler processed the drops (don't drop items on ground), false for normal drop behavior
	 */
	public static boolean handleDrop(Attackable boss, Player killer, Collection<ItemHolder> drops)
	{
		// No handler registered, use normal drop behavior
		if (_handler == null)
		{
			return false;
		}

		try
		{
			// Check if this BOSS should be handled
			if (!_handler.shouldHandle(boss.getId()))
			{
				return false; // Handler doesn't want to process this BOSS
			}

			// Let the handler process the drops
			return _handler.handleBossDrop(boss, killer, drops);
		}
		catch (Exception e)
		{
			LOGGER.warning("Error in BOSS drop handler for BOSS " + boss.getName() + " (ID: " + boss.getId() + "): " + e.getMessage());
			e.printStackTrace();
			return false; // On error, use normal drop behavior to prevent item loss
		}
	}
}
