package de.tehmanu.skybad.commands;

import de.tehmanu.skybad.repository.WorkingTimeRepository;
import de.tehmanu.skybad.util.TimeUtil;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author TehManu
 * @since 13.10.2024
 */
@AllArgsConstructor
public class LogtimeCommand {

    private final WorkingTimeRepository workingTimeRepository;

    @SubscribeEvent
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("logtime")) {
            event.deferReply(true).queue();
            final Member member = event.getMember();
            final Member employee = event.getOptionsByType(OptionType.USER).getFirst().getAsMember();
            if (member == null || employee == null || member.getUser().isBot() || employee.getUser().isBot()) {
                return;
            }
            this.workingTimeRepository.getWorkingStats(employee.getIdLong()).whenComplete((workingStats, throwable) -> {
                final String lastLogout = workingStats.getDate() == null ? "-" : LocalDateTime.of(workingStats.getDate(), workingStats.getToday()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                final String workingTime = workingStats.getDate() == null || workingStats.getDate().isEqual(LocalDate.now()) && workingStats.getWorkingTime() == 0 ? "0min" : TimeUtil.formatTime(workingStats.getWorkingTime());
                final String totalWorkingTime = TimeUtil.formatTime(workingStats.getTotalWorkingTime());
                event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Arbeitszeiten")
                                .setDescription("**Mitarbeiter:** " + employee.getEffectiveName()
                                                + "\n**Letzter Logout:** " + lastLogout
                                                + "\n**Gearbeitete Zeit (Heute):** " + workingTime
                                                + "\n**Gearbeitete Zeit (Woche):** " + totalWorkingTime)
                                .setColor(Color.CYAN)
                                .build())
                        .setEphemeral(true)
                        .queue();
            });
        }
    }
}
