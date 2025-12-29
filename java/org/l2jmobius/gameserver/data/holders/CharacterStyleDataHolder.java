package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Brado
 */
public final class CharacterStyleDataHolder
{
	public final int _styleId;
	public final String _name;
	public final WeaponType _weaponType;
	public final int _shiftWeaponId;
	public final int _shiftArmorId;  // ★ 新增: 防具外觀ID
	private final SkillHolder _skillHolder;
	public final List<ItemHolder> _cost;

	// 通用構造函數 (CHAT_BACKGROUND 等)
	public CharacterStyleDataHolder(int styleId, String name, List<ItemHolder> cost)
	{
		_styleId = styleId;
		_name = name;
		_shiftWeaponId = 0;
		_shiftArmorId = 0;  // ★ 初始化
		_skillHolder = null;
		_weaponType = WeaponType.NONE;
		_cost = Collections.unmodifiableList(new ArrayList<>(cost));
	}

	// 武器外觀構造函數 (APPEARANCE_WEAPON)
	public CharacterStyleDataHolder(int styleId, String name, int shiftWeaponId, WeaponType weaponType, List<ItemHolder> cost)
	{
		_styleId = styleId;
		_name = name;
		_shiftWeaponId = shiftWeaponId;
		_shiftArmorId = 0;  // ★ 初始化
		_skillHolder = null;
		_weaponType = weaponType;
		_cost = Collections.unmodifiableList(new ArrayList<>(cost));
	}

	// ★ 新增: 防具外觀構造函數 (APPEARANCE_ARMOR)
	public CharacterStyleDataHolder(int styleId, String name, int shiftArmorId, List<ItemHolder> cost)
	{
		_styleId = styleId;
		_name = name;
		_shiftWeaponId = 0;
		_shiftArmorId = shiftArmorId;
		_skillHolder = null;
		_weaponType = WeaponType.NONE;
		_cost = Collections.unmodifiableList(new ArrayList<>(cost));
	}

	// 擊殺特效構造函數 (KILL_EFFECT)
	public CharacterStyleDataHolder(int styleId, String name, SkillHolder skillHolder, List<ItemHolder> cost)
	{
		_styleId = styleId;
		_name = name;
		_shiftWeaponId = 0;
		_shiftArmorId = 0;  // ★ 初始化
		_weaponType = WeaponType.NONE;
		_skillHolder = skillHolder;
		_cost = Collections.unmodifiableList(new ArrayList<>(cost));
	}

	public WeaponType getWeaponType()
	{
		return _weaponType;
	}

	public SkillHolder getSkillHolder()
	{
		return _skillHolder;
	}

	public int getShiftWeaponId()
	{
		return _shiftWeaponId;
	}

	// ★ 新增: 獲取防具外觀ID
	public int getShiftArmorId()
	{
		return _shiftArmorId;
	}

	public int getStyleId()
	{
		return _styleId;
	}

	public List<ItemHolder> getCosts()
	{
		return _cost;
	}

	@Override
	public String toString()
	{
		return "Style{id=" + _styleId + ", name='" + _name + '\''
				+ ", shiftWeaponId=" + _shiftWeaponId
				+ ", shiftArmorId=" + _shiftArmorId  // ★ 新增
				+ ", cost=" + _cost + '}';
	}
}