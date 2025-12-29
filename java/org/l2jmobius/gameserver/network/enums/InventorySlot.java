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
package org.l2jmobius.gameserver.network.enums;

import org.l2jmobius.gameserver.model.interfaces.IUpdateTypeComponent;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;

/**
 * @author UnAfraid, Mobius
 */
public enum InventorySlot implements IUpdateTypeComponent
{
	UNDER(Inventory.PAPERDOLL_UNDER),
	REAR(Inventory.PAPERDOLL_REAR),
	LEAR(Inventory.PAPERDOLL_LEAR),
	NECK(Inventory.PAPERDOLL_NECK),
	RFINGER(Inventory.PAPERDOLL_RFINGER),
	LFINGER(Inventory.PAPERDOLL_LFINGER),
	HEAD(Inventory.PAPERDOLL_HEAD),
	RHAND(Inventory.PAPERDOLL_RHAND),
	LHAND(Inventory.PAPERDOLL_LHAND),
	GLOVES(Inventory.PAPERDOLL_GLOVES),
	CHEST(Inventory.PAPERDOLL_CHEST),
	LEGS(Inventory.PAPERDOLL_LEGS),
	FEET(Inventory.PAPERDOLL_FEET),
	CLOAK(Inventory.PAPERDOLL_CLOAK),
	LRHAND(Inventory.PAPERDOLL_RHAND),
	HAIR(Inventory.PAPERDOLL_HAIR),
	HAIR2(Inventory.PAPERDOLL_HAIR2),
	RBRACELET(Inventory.PAPERDOLL_RBRACELET),
	LBRACELET(Inventory.PAPERDOLL_LBRACELET),
	AGATHION1(Inventory.PAPERDOLL_AGATHION1),
	AGATHION2(Inventory.PAPERDOLL_AGATHION2),
	AGATHION3(Inventory.PAPERDOLL_AGATHION3),
	AGATHION4(Inventory.PAPERDOLL_AGATHION4),
	AGATHION5(Inventory.PAPERDOLL_AGATHION5),
	DECO1(Inventory.PAPERDOLL_DECO1),
	DECO2(Inventory.PAPERDOLL_DECO2),
	DECO3(Inventory.PAPERDOLL_DECO3),
	DECO4(Inventory.PAPERDOLL_DECO4),
	DECO5(Inventory.PAPERDOLL_DECO5),
	DECO6(Inventory.PAPERDOLL_DECO6),
	BELT(Inventory.PAPERDOLL_BELT),
	BROOCH(Inventory.PAPERDOLL_BROOCH),
	BROOCH_JEWEL(Inventory.PAPERDOLL_BROOCH_JEWEL1),
	BROOCH_JEWEL2(Inventory.PAPERDOLL_BROOCH_JEWEL2),
	BROOCH_JEWEL3(Inventory.PAPERDOLL_BROOCH_JEWEL3),
	BROOCH_JEWEL4(Inventory.PAPERDOLL_BROOCH_JEWEL4),
	BROOCH_JEWEL5(Inventory.PAPERDOLL_BROOCH_JEWEL5),
	BROOCH_JEWEL6(Inventory.PAPERDOLL_BROOCH_JEWEL6),
	ARTIFACT_BOOK(Inventory.PAPERDOLL_ARTIFACT_BOOK),
	ARTIFACT1(Inventory.PAPERDOLL_ARTIFACT1),
	ARTIFACT2(Inventory.PAPERDOLL_ARTIFACT2),
	ARTIFACT3(Inventory.PAPERDOLL_ARTIFACT3),
	ARTIFACT4(Inventory.PAPERDOLL_ARTIFACT4),
	ARTIFACT5(Inventory.PAPERDOLL_ARTIFACT5),
	ARTIFACT6(Inventory.PAPERDOLL_ARTIFACT6),
	ARTIFACT7(Inventory.PAPERDOLL_ARTIFACT7),
	ARTIFACT8(Inventory.PAPERDOLL_ARTIFACT8),
	ARTIFACT9(Inventory.PAPERDOLL_ARTIFACT9),
	ARTIFACT10(Inventory.PAPERDOLL_ARTIFACT10),
	ARTIFACT11(Inventory.PAPERDOLL_ARTIFACT11),
	ARTIFACT12(Inventory.PAPERDOLL_ARTIFACT12),
	ARTIFACT13(Inventory.PAPERDOLL_ARTIFACT13),
	ARTIFACT14(Inventory.PAPERDOLL_ARTIFACT14),
	ARTIFACT15(Inventory.PAPERDOLL_ARTIFACT15),
	ARTIFACT16(Inventory.PAPERDOLL_ARTIFACT16),
	ARTIFACT17(Inventory.PAPERDOLL_ARTIFACT17),
	ARTIFACT18(Inventory.PAPERDOLL_ARTIFACT18),
	ARTIFACT19(Inventory.PAPERDOLL_ARTIFACT19),
	ARTIFACT20(Inventory.PAPERDOLL_ARTIFACT20),
	ARTIFACT21(Inventory.PAPERDOLL_ARTIFACT21);
	
	private final int _paperdollSlot;
	
	private InventorySlot(int paperdollSlot)
	{
		_paperdollSlot = paperdollSlot;
	}
	
	public int getSlot()
	{
		return _paperdollSlot;
	}
	
	@Override
	public int getMask()
	{
		return ordinal();
	}
}
