package de.tehmanu.skybad.repository;

import de.tehmanu.skybad.SkyBadBot;
import de.tehmanu.skybad.data.WorkingStats;
import de.tehmanu.skybad.datasource.PostgreSQLDataSource;
import de.tehmanu.skybad.util.ExceptionalConsumer;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * @author TehManu
 * @since 20.10.2024
 */
@AllArgsConstructor
public class WorkingTimeRepository {

    private final PostgreSQLDataSource dataSource;
    private final ExecutorService service;

    private final String insertWorkingTime = "INSERT INTO log_working_time(dc_id, login_reason) VALUES(?, ?)";
    private final String updateWorkingTime = """
            UPDATE log_working_time
            SET logout_time = current_time::time,
                log_working_time = (
                    SELECT EXTRACT(EPOCH FROM (current_time::time - login_time::time)) / 60
                ),
                logout_reason = ?
            WHERE dc_id = ?
              AND id = (
                  SELECT MAX(id)
                  FROM log_working_time
                  WHERE dc_id = ?
              )
            """;
    private final String selectTotalWorkingTime = "SELECT SUM(abs_working_time) AS totalWorkingTime FROM working_time WHERE dc_id = ?;";
    private final String selectWorkingStats = """
            SELECT lw.log_date,
                   lw.logout_time,
                   wt.abs_working_time
            FROM log_working_time lw
            LEFT JOIN working_time wt 
                ON lw.dc_id = wt.dc_id 
                AND wt.work_date = CURRENT_DATE
            WHERE lw.dc_id = ?
              AND lw.logout_time IS NOT NULL
            ORDER BY lw.log_date DESC,
                     lw.logout_time DESC
            LIMIT 1;
            """;
    private final String deleteWorkingTime = "UPDATE log_working_time SET logout_reason = ? WHERE dc_id = ? AND id = (SELECT MAX(ID) FROM log_working_time WHERE dc_id = ?)";

    private final String insertNotificationRequest = "INSERT INTO notification_requests(dc_id) VALUES (?)";
    private final String deleteNotificationRequest = "DELETE FROM notification_requests WHERE dc_id = ?";
    private final String selectNotificationRequest = "SELECT * FROM notification_requests";

    public CompletableFuture<Void> trackWorkingTime(final long userId, final String reason) {
        return CompletableFuture.runAsync(() -> {
            try (final Connection connection = this.dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try (final PreparedStatement statement = connection.prepareStatement(this.insertWorkingTime);
                     final PreparedStatement notifyRequest = connection.prepareStatement(this.insertNotificationRequest)) {
                    statement.setLong(1, userId);
                    statement.setString(2, reason);
                    statement.executeUpdate();
                    notifyRequest.setLong(1, userId);
                    notifyRequest.executeUpdate();
                    connection.commit();
                } catch (SQLException exception) {
                    SkyBadBot.getLogger().warn(exception.getMessage(), exception);
                    connection.rollback();
                }
            } catch (SQLException exception) {
                SkyBadBot.getLogger().warn(exception.getMessage(), exception);
            }
        }, this.service);
    }

    public CompletableFuture<Void> untrackWorkingTime(final long userId, final String reason) {
        return CompletableFuture.runAsync(() -> {
            try (final Connection connection = this.dataSource.getConnection()) {
                try (final PreparedStatement statement = connection.prepareStatement(this.updateWorkingTime);
                     final PreparedStatement notifyRequest = connection.prepareStatement(this.deleteNotificationRequest)) {
                    connection.setAutoCommit(false);
                    statement.setString(1, reason);
                    statement.setLong(2, userId);
                    statement.setLong(3, userId);
                    statement.executeUpdate();
                    notifyRequest.setLong(1, userId);
                    notifyRequest.executeUpdate();
                    connection.commit();
                } catch (SQLException exception) {
                    SkyBadBot.getLogger().warn(exception.getMessage(), exception);
                    connection.rollback();
                }
            } catch (SQLException exception) {
                SkyBadBot.getLogger().warn(exception.getMessage(), exception);
            }
        }, this.service);
    }

    public CompletableFuture<WorkingStats> getWorkingStats(final long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(this.selectWorkingStats)) {
                statement.setLong(1, userId);
                final ResultSet resultSet = statement.executeQuery();
                LocalDate date = null;
                LocalTime dateTime = null;
                int workingTime = 0;
                if (resultSet.next()) {
                    date = resultSet.getObject("log_date", LocalDate.class);
                    dateTime = resultSet.getObject("logout_time", LocalTime.class);
                    workingTime = resultSet.getInt("abs_working_time");
                }
                final PreparedStatement totalWorkingTimeStatement = connection.prepareStatement(this.selectTotalWorkingTime);
                totalWorkingTimeStatement.setLong(1, userId);
                final ResultSet totalWorkingTimeResult = totalWorkingTimeStatement.executeQuery();
                int totalWorkingTime = 0;
                if (totalWorkingTimeResult.next()) {
                    totalWorkingTime = totalWorkingTimeResult.getInt("totalWorkingTime");
                }
                return new WorkingStats(date, dateTime, workingTime, totalWorkingTime);
            } catch (SQLException exception) {
                SkyBadBot.getLogger().warn(exception.getMessage(), exception);
                return null;
            }
        }, this.service);
    }

    public CompletableFuture<Void> deleteWorkingTime(final long userId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(this.deleteWorkingTime)) {
                statement.setString(1, "system-logout");
                statement.setLong(2, userId);
                statement.setLong(3, userId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                SkyBadBot.getLogger().warn(exception.getMessage(), exception);
            }
        }, this.service);
    }

    public void getAllNotificationRequests(final ExceptionalConsumer<ResultSet> callback) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(this.selectNotificationRequest, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                callback.accept(statement.executeQuery());
            } catch (SQLException exception) {
                SkyBadBot.getLogger().warn(exception.getMessage(), exception);
            }
        }, this.service);
    }
}
