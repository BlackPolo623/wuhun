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
package org.l2jmobius.gameserver.model.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.util.DXT1ImageCompressor;

/**
 * @author JoeAlisson
 */
public class CaptchaGenerator
{
	private static final Map<Integer, Captcha> CAPTCHAS = new ConcurrentHashMap<>();
	private static final DXT1ImageCompressor COMPRESSOR = new DXT1ImageCompressor();
	
	private CaptchaGenerator()
	{
	}
	
	public Captcha next()
	{
		final int id = Rnd.get(CAPTCHAS.size() + 5);
		return CAPTCHAS.computeIfAbsent(id, this::generateCaptcha);
	}
	
	public Captcha next(int previousId)
	{
		int id = Rnd.get(CAPTCHAS.size() + 5);
		if (id == previousId)
		{
			id++;
		}
		
		return CAPTCHAS.computeIfAbsent(id, this::generateCaptcha);
	}
	
	private int generateCaptchaCode()
	{
		return Rnd.get(111111, 999999);
	}
	
	private Captcha generateCaptcha(int id)
	{
		final int height = 32;
		final int width = 128;
		
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
		Graphics2D graphics = createGraphics(height, width, image);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
		
		final int code = generateCaptchaCode();
		
		writeCode(code, graphics);
		addNoise(graphics);
		graphics.dispose();
		return new Captcha(id, code, COMPRESSOR.compress(image));
	}
	
	private Graphics2D createGraphics(int height, int width, BufferedImage image)
	{
		final Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, width, height);
		return graphics;
	}
	
	private void writeCode(int code, Graphics2D graphics)
	{
		final String text = String.valueOf(code);
		final FontMetrics metrics = graphics.getFontMetrics();
		
		final int textStart = 10;
		for (int i = 0; i < text.length(); i++)
		{
			final char character = text.charAt(i);
			final int charWidth = metrics.charWidth(character) + 5;
			graphics.setColor(getColor());
			graphics.drawString("" + character, textStart + (i * charWidth), Rnd.get(24, 32));
		}
	}
	
	private void addNoise(Graphics2D graphics)
	{
		graphics.setColor(Color.WHITE);
		for (int i = 0; i < 20; i++)
		{
			graphics.fillOval(Rnd.get(10, 122), Rnd.get(6, 20), 4, 4);
		}
		
		for (int i = 0; i < 6; i++)
		{
			graphics.drawLine(Rnd.get(30, 90), Rnd.get(6, 28), Rnd.get(80, 120), Rnd.get(10, 26));
		}
	}
	
	private Color getColor()
	{
		switch (Rnd.get(5))
		{
			case 1:
			{
				return Color.WHITE;
			}
			case 2:
			{
				return Color.RED;
			}
			case 3:
			{
				return Color.YELLOW;
			}
			case 4:
			{
				return Color.CYAN;
			}
			default:
			{
				return Color.GREEN;
			}
		}
	}
	
	public static CaptchaGenerator getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		private static final CaptchaGenerator INSTANCE = new CaptchaGenerator();
	}
}
