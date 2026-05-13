package com.auth.repository;

import com.auth.model.User;
import com.auth.model.UserRole;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setAccountNonExpired(rs.getBoolean("account_non_expired"));
        user.setAccountNonLocked(rs.getBoolean("account_non_locked"));
        user.setCredentialsNonExpired(rs.getBoolean("credentials_non_expired"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toInstant());
        }

        return user;
    };

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public UserRepository(@Qualifier("authDataSource") DataSource authDataSource) {
        this.jdbcTemplate = new JdbcTemplate(authDataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(authDataSource);
    }

    public Optional<User> findById(Long id) {
        return queryForOptional("SELECT * FROM users WHERE id = ?", id);
    }

    public Optional<User> findByUsername(String username) {
        return queryForOptional("SELECT * FROM users WHERE username = ?", username);
    }

    public Optional<User> findByEmail(String email) {
        return queryForOptional("SELECT * FROM users WHERE email = ?", email);
    }

    public Optional<User> findByUsernameOrEmail(String username, String email) {
        return queryForOptional("SELECT * FROM users WHERE username = ? OR email = ?", username, email);
    }

    public boolean existsByUsername(String username) {
        return exists("SELECT COUNT(*) FROM users WHERE username = ?", username);
    }

    public boolean existsByEmail(String email) {
        return exists("SELECT COUNT(*) FROM users WHERE email = ?", email);
    }

    public List<User> findByRole(UserRole role) {
        return jdbcTemplate.query(
                "SELECT * FROM users WHERE role = ? ORDER BY created_at DESC",
                USER_ROW_MAPPER,
                role.name()
        );
    }

    public List<User> findByEnabled(Boolean enabled) {
        return jdbcTemplate.query(
                "SELECT * FROM users WHERE enabled = ? ORDER BY created_at DESC",
                USER_ROW_MAPPER,
                enabled
        );
    }

    public long countByRole(UserRole role) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = ?",
                Long.class,
                role.name()
        );
        return count == null ? 0L : count;
    }

    public List<User> findUsersCreatedAfter(Instant timestamp) {
        return jdbcTemplate.query(
                "SELECT * FROM users WHERE created_at > ? ORDER BY created_at DESC",
                USER_ROW_MAPPER,
                Timestamp.from(timestamp)
        );
    }

    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        }

        update(user);
        return user;
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    private User insert(User user) {
        String sql = """
                INSERT INTO users (
                    username,
                    password_hash,
                    email,
                    role,
                    enabled,
                    account_non_expired,
                    account_non_locked,
                    credentials_non_expired
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getRole().name());
            statement.setBoolean(5, Boolean.TRUE.equals(user.getEnabled()));
            statement.setBoolean(6, Boolean.TRUE.equals(user.getAccountNonExpired()));
            statement.setBoolean(7, Boolean.TRUE.equals(user.getAccountNonLocked()));
            statement.setBoolean(8, Boolean.TRUE.equals(user.getCredentialsNonExpired()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        } else if (keyHolder.getKeys() != null && keyHolder.getKeys().get("id") instanceof Number generatedId) {
            user.setId(generatedId.longValue());
        }
        return user;
    }

    private void update(User user) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("username", user.getUsername())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("email", user.getEmail())
                .addValue("role", user.getRole().name())
                .addValue("enabled", user.getEnabled())
                .addValue("accountNonExpired", user.getAccountNonExpired())
                .addValue("accountNonLocked", user.getAccountNonLocked())
                .addValue("credentialsNonExpired", user.getCredentialsNonExpired());

        namedParameterJdbcTemplate.update("""
                UPDATE users
                SET username = :username,
                    password_hash = :passwordHash,
                    email = :email,
                    role = :role,
                    enabled = :enabled,
                    account_non_expired = :accountNonExpired,
                    account_non_locked = :accountNonLocked,
                    credentials_non_expired = :credentialsNonExpired,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, params);
    }

    private Optional<User> queryForOptional(String sql, Object... args) {
        List<User> users = jdbcTemplate.query(sql, USER_ROW_MAPPER, args);
        return users.stream().findFirst();
    }

    private boolean exists(String sql, Object arg) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, arg);
        return count != null && count > 0;
    }
}
