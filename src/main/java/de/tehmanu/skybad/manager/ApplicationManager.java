package de.tehmanu.skybad.manager;

import de.tehmanu.skybad.SkyBadBot;
import de.tehmanu.skybad.repository.ApplicationRepository;
import de.tehmanu.skybad.util.Job;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author TehManu
 * @since 18.10.2024
 */
@AllArgsConstructor
public class ApplicationManager {

    private final ApplicationRepository applicationRepository;

    @Getter
    private final Map<Job, String[]> jobs = new HashMap<>();

    public void loadJobs() {
        this.applicationRepository.getAllJobs().whenComplete((allJobs, throwable) -> {
            Map<Boolean, List<Job>> sortedJobs = allJobs.entrySet().stream()
                    .collect(Collectors.partitioningBy(Map.Entry::getValue,
                            Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

            // jobs that currently are open for applying
            String openJobs = sortedJobs.get(true).stream()
                    .sorted(Comparator.comparingInt(Enum::ordinal))
                    .map(job -> "- " + job.getDisplayName())
                    .collect(Collectors.joining("\n"));

            // jobs that are currently closed
            String closedJobs = sortedJobs.get(false).stream()
                    .sorted(Comparator.comparingInt(Enum::ordinal))
                    .map(job -> "- " + job.getDisplayName())
                    .collect(Collectors.joining("\n"));

            List<SelectOption> selectOptions = this.jobs.keySet().stream()
                    .sorted(Comparator.comparingInt(Enum::ordinal))
                    .map(Job::toSelectOption)
                    .toList();

            StringSelectMenu menu = StringSelectMenu.create("job-selection")
                    .setPlaceholder("Job Auswahl")
                    .setRequiredRange(1, 1)
                    .addOptions(selectOptions)
                    .build();

            final TextChannel channel = SkyBadBot.getInstance().getJDA().getTextChannelById(1296746742137618432L);
            if (channel != null) {
                channel.getHistory().retrievePast(1).queue(messages -> {
                    if (messages.isEmpty()) {
                        channel.sendMessageEmbeds(new EmbedBuilder().setTitle("SkyBad Aktuelle Bewerbungsphasen für die einzelnen Bereiche")
                                        .addField("✅ Geöffnet", openJobs, false)
                                        .addField("❌ Geschlossen", closedJobs, false)
                                        .addField("Informationen über unsere Bewerbungsmöglichkeiten",
                                                "Hier kannst du dich über die verschiedenen Jobs informieren und sehen, welche Bedingungen du benötigst," +
                                                " um besondere Rollen zu erhalten. Bitte wähle dafür den gewünschten Rang aus.", false)
                                        .build())
                                .addActionRow(menu)
                                .queue();
                    }
                });
            }
        });
    }

    public void loadAvailableJobs() {
        SkyBadBot.getLogger().debug("Loading available jobs...");
        this.applicationRepository.getAvailableJobs().whenComplete((availableJobs, throwable) -> {
            availableJobs.forEach(job -> jobs.put(job, this.loadRequirementsFromFile(job.getDisplayName())));

        });
    }

    private String[] loadRequirementsFromFile(final String jobTitle) {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/" + jobTitle.toLowerCase() + ".txt"))) {
            final StringBuilder builder = new StringBuilder();
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.isBlank()) {
                    builder.append("\n\n");
                }
                builder.append(line);
            }
            return builder.toString().split("\n\n");
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
