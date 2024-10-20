package de.tehmanu.skybad.commands;

import de.tehmanu.skybad.SkyBadBot;
import de.tehmanu.skybad.data.SkyBadRole;
import de.tehmanu.skybad.repository.WorkingTimeRepository;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author TehManu
 * @since 09.10.2024
 */
@AllArgsConstructor
public class LogoutCommand {

    private final WorkingTimeRepository workingTimeRepository;

    @SubscribeEvent
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("logout")) {
            final Member employee = event.getMember();
            if (employee == null || employee.getUser().isBot()) {
                return;
            }
            if (!SkyBadBot.getInstance().getTrackedEmployees().remove(employee.getIdLong())) {
                event.replyEmbeds(new EmbedBuilder().setTitle("Logout fehlgeschlagen")
                                .setDescription("Du bist nicht eingeloggt!")
                                .setColor(Color.DARK_GRAY)
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
            this.workingTimeRepository.untrackWorkingTime(employee.getIdLong(), "").whenComplete((v, throwable) -> {
                final Guild guild = employee.getGuild();
                final Role role = guild.getRoleById(SkyBadRole.CURRENTLY_ONLINE.getRoleId());
                if (role != null) {
                    guild.removeRoleFromMember(employee, role).queue();
                }
                event.replyEmbeds(new EmbedBuilder().setTitle("Erfolgreich ausgeloggt")
                                .setDescription("Du hast dich nun als **" + employee.getEffectiveName() + "** ausgeloggt. Einen schönen Tag dir noch!")
                                .setColor(Color.RED)
                                .build())
                        .queue();
            });
        }
    }
}
