package custom.ScratchCard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

public class ScratchCardDAO
{
    private static final Logger LOGGER = Logger.getLogger(ScratchCardDAO.class.getName());

    private static final String SELECT_SQL =
            "SELECT card_type, board_state, opened_positions, opened_count, accumulated_count " +
                    "FROM scratch_cards WHERE player_id = ?";

    private static final String INSERT_SQL =
            "INSERT INTO scratch_cards (player_id, card_type, board_state, opened_positions, " +
                    "opened_count, accumulated_count, purchase_time) " +
                    "VALUES (?, ?, ?, '', 0, 0, ?) ON DUPLICATE KEY UPDATE " +
                    "card_type = VALUES(card_type), board_state = VALUES(board_state), " +
                    "opened_positions = '', opened_count = 0, accumulated_count = 0, " +
                    "purchase_time = VALUES(purchase_time)";

    private static final String UPDATE_SQL =
            "UPDATE scratch_cards SET opened_positions = ?, opened_count = ?, accumulated_count = ? " +
                    "WHERE player_id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM scratch_cards WHERE player_id = ?";

    public static class ScratchCardState
    {
        public int cardType;  // 新增:刮刮樂類型
        public String boardState;
        public String openedPositions;
        public int openedCount;
        public int accumulatedCount;

        public ScratchCardState(int cardType, String boardState, String openedPositions,
                                int openedCount, int accumulatedCount)
        {
            this.cardType = cardType;
            this.boardState = boardState;
            this.openedPositions = openedPositions;
            this.openedCount = openedCount;
            this.accumulatedCount = accumulatedCount;
        }
    }

    public static ScratchCardState loadState(int playerId)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_SQL))
        {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return new ScratchCardState(
                            rs.getInt("card_type"),
                            rs.getString("board_state"),
                            rs.getString("opened_positions"),
                            rs.getInt("opened_count"),
                            rs.getInt("accumulated_count")
                    );
                }
            }
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "Failed to load scratch card state for player: " + playerId, e);
        }
        return null;
    }

    public static boolean createNew(int playerId, int cardType, String boardState)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SQL))
        {
            ps.setInt(1, playerId);
            ps.setInt(2, cardType);
            ps.setString(3, boardState);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
            return true;
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "Failed to create scratch card for player: " + playerId, e);
            return false;
        }
    }

    public static boolean updateState(int playerId, String openedPositions,
                                      int openedCount, int accumulatedCount)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_SQL))
        {
            ps.setString(1, openedPositions);
            ps.setInt(2, openedCount);
            ps.setInt(3, accumulatedCount);
            ps.setInt(4, playerId);
            ps.executeUpdate();
            return true;
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "Failed to update scratch card state for player: " + playerId, e);
            return false;
        }
    }

    public static boolean deleteState(int playerId)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_SQL))
        {
            ps.setInt(1, playerId);
            ps.executeUpdate();
            return true;
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "Failed to delete scratch card state for player: " + playerId, e);
            return false;
        }
    }
}