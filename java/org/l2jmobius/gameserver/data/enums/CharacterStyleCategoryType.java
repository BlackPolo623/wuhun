package org.l2jmobius.gameserver.data.enums;

import java.util.Locale;

/**
 * @author Brado
 */
public enum CharacterStyleCategoryType
{
	APPEARANCE_WEAPON(0),      // 武器外觀 ✅
	KILL_EFFECT(1),            // 擊殺特效 (原本是1,順序對了!)
	CHAT_BACKGROUND(2),        // 名稱/聊天背景 (原本是3,改成2)
	APPEARANCE_ARMOR(3);       // 防具外觀 (原本是1,改成3)

	public final int _categoryId;

	CharacterStyleCategoryType(int categoryId)
	{
		_categoryId = categoryId;
	}

	public int getClientId()
	{
		return _categoryId;
	}

	public static CharacterStyleCategoryType from(String s)
	{
		return CharacterStyleCategoryType.valueOf(s.trim().toUpperCase(Locale.ROOT));
	}

	public static CharacterStyleCategoryType getByClientId(int clientId)
	{
		for (CharacterStyleCategoryType style : values())
		{
			if (style.getClientId() == clientId)
			{
				return style;
			}
		}

		return null;
	}
}