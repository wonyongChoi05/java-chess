package chess.dao;

import chess.domain.Color;
import chess.domain.Piece;
import chess.domain.Pieces;
import chess.domain.Position;
import chess.domain.Shape;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PieceDaoImpl implements PieceDao {

    private final DBConnection dbConnection = new DBConnection();
    private final RowMapper pieceMapper = rs -> {
        String shape = rs.getString("shape");
        int rank = rs.getInt("rank");
        char file = rs.getString("file").charAt(0);

        return Piece.from(rank, file, Shape.valueOf(shape));
    };

    @Override
    public void create(final Piece piece, final Color color) {
        processQuery("INSERT INTO piece(`rank`, `file`, shape, color) VALUES(?, ?, ?, ?)",
                preparedStatement -> {
                    preparedStatement.setInt(1, piece.getRank());
                    preparedStatement.setString(2, String.valueOf(piece.getFile()));
                    preparedStatement.setString(3, piece.getShape().name());
                    preparedStatement.setString(4, color.name());
                    preparedStatement.execute();
                });
    }

    @Override
    public List<Piece> findPieceByColor(final Color color) {
        List<Pieces> pieces = new ArrayList<>();
        String query = "SELECT * FROM piece WHERE color = ?";
        processQuery(query, preparedStatement -> {
            preparedStatement.setString(1, color.name());
            ResultSet rs = preparedStatement.executeQuery();
            pieces.add(getResults(rs, pieceMapper));
        });
        return pieces.get(0).getPieces();
    }

    @Override
    public void updatePosition(Piece updatedPiece, Position from) {
        final String query = "UPDATE piece SET `rank` = ?, `file` = ? WHERE `rank` = ? AND `file` = ?";
        processQuery(query, preparedStatement -> {
            preparedStatement.setInt(1, updatedPiece.getRank());
            preparedStatement.setString(2, String.valueOf(updatedPiece.getFile()));
            preparedStatement.setInt(3, from.getRankValue());
            preparedStatement.setString(4, String.valueOf(from.getFileValue()));
            preparedStatement.execute();
        });
    }

    @Override
    public void deletePieceByColor(Piece removalPiece, Color pieceColor) {
        final String query = "DELETE FROM piece WHERE `rank` = ? AND `file` = ? AND color = ?";
        processQuery(query, preparedStatement -> {
            preparedStatement.setInt(1, removalPiece.getRank());
            preparedStatement.setString(2, String.valueOf(removalPiece.getFile()));
            preparedStatement.setString(3, pieceColor.name());
            preparedStatement.execute();
        });
    }

    public void deleteAll() {
        String query = "DELETE FROM piece";
        processQuery(query, PreparedStatement::execute);
    }

    private void processQuery(String query, QueryProcessor queryProcessor) {
        try (final Connection connection = dbConnection.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            queryProcessor.process(preparedStatement);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Pieces getResults(ResultSet rs, RowMapper rowMapper) throws SQLException {
        List<Piece> results = new ArrayList<>();
        while (rs.next()) {
            Piece result = rowMapper.run(rs);
            results.add(result);
        }
        return Pieces.from(results);
    }

}
