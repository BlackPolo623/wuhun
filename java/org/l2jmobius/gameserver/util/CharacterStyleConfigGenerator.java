package org.l2jmobius.gameserver.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;

import static org.l2jmobius.commons.util.IXmlReader.LOGGER;

/**
 * 自動生成 CharacterStylesData.xml 配置
 */
public class CharacterStyleConfigGenerator
{
	private static final Map<String, String> PENDING_CONFIGS = new ConcurrentHashMap<>();

	/**
	 * 記錄缺失的配置
	 */
	public static void recordMissingStyle(CharacterStyleCategoryType category, int styleId, int costItemId)
	{
		final String key = category.name() + "_" + styleId;
		if (!PENDING_CONFIGS.containsKey(key))
		{
			final String config = generateStyleConfig(category, styleId, costItemId);
			PENDING_CONFIGS.put(key, config);
			LOGGER.info("記錄缺失的造型配置: " + key);
		}
	}

	/**
	 * 生成 XML 配置
	 */
	private static String generateStyleConfig(CharacterStyleCategoryType category, int styleId, int costItemId)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("    <style styleId=\"").append(styleId).append("\" name=\"造型_").append(styleId).append("\"");

		if (category == CharacterStyleCategoryType.APPEARANCE_WEAPON)
		{
			sb.append(" shiftWeaponId=\"0\" weaponType=\"SWORD\"");
		}
		else if (category == CharacterStyleCategoryType.KILL_EFFECT)
		{
			sb.append(" skillId=\"0\" skillLevel=\"1\"");
		}

		sb.append(">\n");
		sb.append("        <cost>\n");
		sb.append("            <item id=\"").append(costItemId).append("\" count=\"1\" />\n");
		sb.append("        </cost>\n");
		sb.append("    </style>\n");

		return sb.toString();
	}

	/**
	 * 導出所有缺失的配置到文件
	 */
	public static void exportMissingConfigs()
	{
		if (PENDING_CONFIGS.isEmpty())
		{
			LOGGER.info("沒有缺失的造型配置");
			return;
		}

		try
		{
			final File file = new File("log/missing_character_styles.xml");
			file.getParentFile().mkdirs();

			try (FileWriter writer = new FileWriter(file))
			{
				writer.write("<!-- 缺失的造型配置,請複製到 CharacterStylesData.xml 中 -->\n");
				writer.write("<list>\n");

				// 按類型分組
				for (CharacterStyleCategoryType type : CharacterStyleCategoryType.values())
				{
					boolean hasType = false;
					for (Map.Entry<String, String> entry : PENDING_CONFIGS.entrySet())
					{
						if (entry.getKey().startsWith(type.name()))
						{
							if (!hasType)
							{
								writer.write("\n    <category type=\"" + type.name() + "\" swapCostId=\"57\" swapCostCount=\"1000\">\n");
								hasType = true;
							}
							writer.write(entry.getValue());
						}
					}
					if (hasType)
					{
						writer.write("    </category>\n");
					}
				}

				writer.write("</list>\n");
			}

			LOGGER.info("缺失的造型配置已導出到: " + file.getAbsolutePath());
			LOGGER.info("共 " + PENDING_CONFIGS.size() + " 個配置");
		}
		catch (IOException e)
		{
			LOGGER.severe("導出配置失敗: " + e.getMessage());
		}
	}
}