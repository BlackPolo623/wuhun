package custom.RunMerchant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * 簡單 DAO，用於讀取/寫入城市倍率
 */
public final class RunMerchantDAO
{
	public static final Logger LOGGER = Logger.getLogger(RunMerchantDAO.class.getName());
	private static final String SELECT_SQL = "SELECT multiplier FROM runmerchant_multipliers WHERE city_id = ?";
	private static final String UPSERT_SQL = "INSERT INTO runmerchant_multipliers (city_id, multiplier, last_updated) VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE multiplier = ?, last_updated = NOW()";

	private RunMerchantDAO()
	{
	}

	public static double loadMultiplier(int cityId)
	{
		double mult = 0.1;
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_SQL))
		{
			ps.setInt(1, cityId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					mult = rs.getDouble("multiplier");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "RunMerchantDAO: loadMultiplier error for cityId=" + cityId, e);
		}
		return mult;
	}

	public static void saveMultiplier(int cityId, double multiplier)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(UPSERT_SQL))
		{
			ps.setInt(1, cityId);
			ps.setDouble(2, multiplier);
			ps.setDouble(3, multiplier);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "RunMerchantDAO: saveMultiplier error for cityId=" + cityId + " multiplier=" + multiplier, e);
		}
	}
}