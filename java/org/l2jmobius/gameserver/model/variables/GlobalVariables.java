package org.l2jmobius.gameserver.model.variables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * GlobalVariables - 全局变量永久存储 - 自动读写数据库
 */
public class GlobalVariables
{
	private static final Map<String, String> VARS = new ConcurrentHashMap<>();
	private static final GlobalVariables INSTANCE = new GlobalVariables();

	// 构造时加载数据库
	private GlobalVariables()
	{
		load();
	}

	public static GlobalVariables getInstance()
	{
		return INSTANCE;
	}

	/**
	 * 从数据库读取所有变量
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT name, value FROM sglobals_variables");
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				VARS.put(rs.getString("name"), rs.getString("value"));
			}
			System.out.println("### GlobalVariables Loaded: " + VARS.size());
		}
		catch (Exception e)
		{
			System.out.println("GlobalVariables load Error: " + e);
		}
	}

	// ===================== Getter ========================

	public String getString(String key, String def)
	{
		return VARS.getOrDefault(key, def);
	}

	public static int getInt(String key, int def)
	{
		try
		{
			return Integer.parseInt(VARS.getOrDefault(key, String.valueOf(def)));
		}
		catch (Exception e)
		{
			return def;
		}
	}

	public static long getLong(String key, long def)
	{
		try
		{
			return Long.parseLong(VARS.getOrDefault(key, String.valueOf(def)));
		}
		catch (Exception e)
		{
			return def;
		}
	}

	// ===================== SET + SAVE ========================

	public synchronized static void set(String key, Object value)
	{
		String val = String.valueOf(value);
		VARS.put(key, val);
		save(key, val);
	}

	/**
	 * 写入 DB（存在=更新 / 不存在=新建）
	 */
	private static void save(String key, String value)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO sglobals_variables (name, value) VALUES (?, ?)"))
		{
			ps.setString(1, key);
			ps.setString(2, value);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			System.out.println("GlobalVariables Save Error: " + key + " = " + value);
		}
	}
}
