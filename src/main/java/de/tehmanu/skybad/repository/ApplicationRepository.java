package de.tehmanu.skybad.repository;

import de.tehmanu.skybad.SkyBadBot;
import de.tehmanu.skybad.util.Job;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author TehManu
 * @since 20.10.2024
 */
@AllArgsConstructor
public class ApplicationRepository {

    private final SkyBadBot skyBadBot;

    private final String selectAllJobs = "SELECT job_name, is_available FROM job_availability";
    private final String selectAvailableJobs = "SELECT job_name FROM job_availability WHERE is_available";
    private final String updateJobAvailability = "UPDATE job_availability SET is_available = ? WHERE job_name = ?";

    public CompletableFuture<Map<Job, Boolean>> getAllJobs() {
        return CompletableFuture.supplyAsync(() -> {
            Map<Job, Boolean> jobs = new HashMap<>();
            try (final Connection connection = this.skyBadBot.getDataSource().getConnection();
                 final PreparedStatement statement = connection.prepareStatement(this.selectAllJobs)) {
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final Job job = Job.valueOf(resultSet.getString("job_name"));
                    final boolean available = resultSet.getBoolean("is_available");
                    jobs.put(job, available);
                }
                return jobs;
            } catch (SQLException exception) {
                SkyBadBot.getLogger().warn(exception.getMessage(), exception);
                return jobs;
            }
        }, this.skyBadBot.getService());
    }

    public CompletableFuture<List<Job>> getAvailableJobs() {
        return CompletableFuture.supplyAsync(() -> {
            try (final Connection connection = this.skyBadBot.getDataSource().getConnection();
                 final PreparedStatement statement = connection.prepareStatement(this.selectAvailableJobs)) {
                final ResultSet resultSet = statement.executeQuery();
                final List<Job> jobs = new ArrayList<>();
                while (resultSet.next()) {
                    final Job job = Job.valueOf(resultSet.getString("job_name"));
                    jobs.add(job);
                }
                return jobs;
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, this.skyBadBot.getService());
    }

    public CompletableFuture<Void> updateJobAvailability(final String jobName, final boolean available) {
        return CompletableFuture.runAsync(() -> {
            try (final Connection connection = this.skyBadBot.getDataSource().getConnection();
                 final PreparedStatement statement = connection.prepareStatement(this.updateJobAvailability)) {
                statement.setBoolean(1, available);
                statement.setString(2, jobName);
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, this.skyBadBot.getService());
    }
}
