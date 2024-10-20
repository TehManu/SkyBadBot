package de.tehmanu.skybad.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author TehManu
 * @since 10.10.2024
 */
@Getter
public class PostgreSQLDataSource {

    private final HikariConfig config;
    private final HikariDataSource dataSource;

    public PostgreSQLDataSource() {
        this.config = new HikariConfig("/skybad.properties");
        this.dataSource = new HikariDataSource(this.config);
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }
}
