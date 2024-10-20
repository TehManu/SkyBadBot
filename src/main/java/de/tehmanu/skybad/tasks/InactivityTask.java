package de.tehmanu.skybad.tasks;

import de.tehmanu.skybad.SkyBadBot;
import de.tehmanu.skybad.data.SkyBadRole;
import de.tehmanu.skybad.repository.WorkingTimeRepository;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Timestamp;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @author TehManu
 * @since 15.10.2024
 */

@AllArgsConstructor
public class InactivityTask extends TimerTask {

    private final WorkingTimeRepository workingTimeRepository;

    @Override
    public void run() {
        this.workingTimeRepository.getAllNotificationRequests(resultSet -> {
            while (resultSet.next()) {
                final long userId = resultSet.getLong("dc_id");
                final long lastConfirmation = resultSet.getTimestamp("last_confirmation").getTime();
                final long lastRequest = resultSet.getTimestamp("last_request").getTime();
                if (lastRequest + TimeUnit.MINUTES.toMillis(3) < System.currentTimeMillis()) {
                    resultSet.updateTimestamp("last_request", new Timestamp(System.currentTimeMillis()));
                    resultSet.updateRow();
                    // Send notification
                    final Guild guild = SkyBadBot.getInstance().getJDA().getGuildById(SkyBadBot.GUILD_ID);
                    if (guild != null) {
                        final Member employee = guild.getMemberById(userId);
                        if (employee != null) {
                            employee.getUser().openPrivateChannel().
                                    flatMap(channel -> channel.sendMessageEmbeds(new EmbedBuilder()
                                                    .setTitle("SkyBad Hey, bist du noch da?")
                                                    .setDescription("Um deine Schicht fortzusetzen bestätige bitte, dass du nicht inaktiv bist.\n" +
                                                                    "Du hast `10 Minuten` Zeit um diese Anfrage zu bestätigen.")
                                                    .build())
                                            .addActionRow(Button.success("accept", "Ja")))
                                    .queue();
                        }
                    }
                } else if (System.currentTimeMillis() - lastRequest >= TimeUnit.MINUTES.toMillis(1)
                           && System.currentTimeMillis() - lastConfirmation > TimeUnit.MINUTES.toMillis(4)) {
                    resultSet.deleteRow();
                    // Logout
                    final Guild guild = SkyBadBot.getInstance().getJDA().getGuildById(SkyBadBot.GUILD_ID);
                    if (guild != null) {
                        final Member employee = guild.getMemberById(userId);
                        if (employee != null) {
                            final Role role = guild.getRoleById(SkyBadRole.CURRENTLY_ONLINE.getRoleId());
                            if (role != null) {
                                guild.removeRoleFromMember(employee, role).queue();
                            }
                            employee.getUser().openPrivateChannel()
                                    .flatMap(channel -> channel.editMessageComponentsById(channel.getLatestMessageIdLong())
                                            .setContent("Anfrage abgelaufen"))
                                    .queue();
                            SkyBadBot.getInstance().getTrackedEmployees().remove(userId);
                            this.workingTimeRepository.deleteWorkingTime(userId);
                            final TextChannel channel = guild.getTextChannelById(1293526864219865091L);
                            if (channel != null) {
                                channel.sendMessageEmbeds(new EmbedBuilder().setTitle("System-Logout erfolgreich️")
                                                .setDescription("Der Mitarbeiter " + employee.getAsMention() + " wurde wegen Inaktivität vom System ausgeloggt.")
                                                .setColor(Color.ORANGE)
                                                .build())
                                        .queue();
                            }
                        }
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        final User user = event.getUser();
        if (event.getComponentId().equals("accept")) {
            this.workingTimeRepository.untrackWorkingTime(user.getIdLong(), "system-save");
            this.workingTimeRepository.trackWorkingTime(user.getIdLong(), "system-save");
            event.deferEdit().queue();
            event.getHook().editOriginalComponents().setContent("Anfrage akzeptiert").queue();
        }
    }
}
