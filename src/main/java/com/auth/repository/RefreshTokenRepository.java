package com.auth.repository;

import com.auth.model.RefreshTokenRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {

    private static final RowMapper<RefreshTokenRecord> ROW_MAPPER = (rs, rowNum) -> {
        RefreshTokenRecord record = new RefreshTokenRecord();
        record.setId(rs.getLong("id"));
        record.setUserId(rs.getLong("user_id"));
        record.setTokenHash(rs.getString("token_hash"));

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            record.setExpiresAt(expiresAt.toInstant());
        }

        Timestamp revokedAt = rs.getTimestamp("revoked_at");
        if (revokedAt != null) {
            record.setRevokedAt(revokedAt.toInstant());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toInstant());
        }
        return record;
    };

    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenRepository(@Qualifier("authDataSource") DataSource authDataSource) {
        this.jdbcTemplate = new JdbcTemplate(authDataSource);
    }

    public RefreshTokenRecord save(Long userId, String tokenHash, Instant expiresAt) {
        String sql = """
                INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setLong(1, userId);
            statement.setString(2, tokenHash);
            statement.setTimestamp(3, Timestamp.from(expiresAt));
            return statement;
        }, keyHolder);

        RefreshTokenRecord record = new RefreshTokenRecord();
        Number key = keyHolder.getKey();
        if (key != null) {
            record.setId(key.longValue());
        }
        record.setUserId(userId);
        record.setTokenHash(tokenHash);
        record.setExpiresAt(expiresAt);
        return record;
    }

    public Optional<RefreshTokenRecord> findActiveByUserIdAndTokenHash(Long userId, String tokenHash, Instant now) {
        List<RefreshTokenRecord> records = jdbcTemplate.query(
                """
                SELECT * FROM refresh_tokens
                WHERE user_id = ?
                  AND token_hash = ?
                  AND revoked_at IS NULL
                  AND expires_at > ?
                ORDER BY created_at DESC
                """,
                ROW_MAPPER,
                userId,
                tokenHash,
                Timestamp.from(now)
        );
        return records.stream().findFirst();
    }

    public void revokeByTokenHash(String tokenHash) {
        jdbcTemplate.update(
                """
                UPDATE refresh_tokens
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE token_hash = ? AND revoked_at IS NULL
                """,
                tokenHash
        );
    }
}
