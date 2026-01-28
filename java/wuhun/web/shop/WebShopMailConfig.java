/*
 * 武魂天堂2 - 網頁商城系統
 * Web Shop System for Wuhun Lineage 2
 */
package wuhun.web.shop;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * 網頁商城郵件管理器設定
 * @author Wuhun
 */
public class WebShopMailConfig
{
	// File
	private static final String CONFIG_FILE = "./config/Custom/WebShopMail.ini";

	// Constants
	public static boolean ENABLED;
	public static int CHECK_DELAY;

	public static void load()
	{
		final ConfigReader config = new ConfigReader(CONFIG_FILE);
		ENABLED = config.getBoolean("WebShopMailEnabled", true);
		CHECK_DELAY = config.getInt("CheckDelaySeconds", 10) * 1000;
	}
}
