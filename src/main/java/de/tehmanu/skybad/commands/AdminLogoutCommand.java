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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author TehManu
 * @since 09.10.2024
 */
@AllArgsConstructor
public class AdminLogoutCommand {

    private final WorkingTimeRepository workingTimeRepository;

    @SubscribeEvent
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("adminlogout")) {
            final Member employee = event.getOptionsByType(OptionType.USER).getFirst().getAsMember();
            final Member manager = event.getMember();
            if (employee == null || manager == null) {
                return;
            }
            if (employee.getIdLong() == manager.getIdLong()) {
                event.replyEmbeds(new EmbedBuilder().setTitle("Admin-Logout fehlgeschlagen")
                                .setDescription("Bitte verwende /logout um dich auszuloggen.")
                                .setColor(Color.DARK_GRAY)
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
            if (!SkyBadBot.getInstance().getTrackedEmployees().remove(employee.getIdLong())) {
                event.replyEmbeds(new EmbedBuilder().setTitle("Admin-Logout fehlgeschlagen")
                                .setDescription("Der Mitarbeiter " + employee.getEffectiveName() + " ist nicht eingeloggt!")
                                .setColor(Color.DARK_GRAY)
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
            this.workingTimeRepository.untrackWorkingTime(employee.getIdLong(), "admin-logout").whenComplete((v, throwable) -> {
                final Guild guild = employee.getGuild();
                final Role role = guild.getRoleById(SkyBadRole.CURRENTLY_ONLINE.getRoleId());
                if (role != null) {
                    guild.removeRoleFromMember(employee, role).queue();
                }
                event.replyEmbeds(new EmbedBuilder().setTitle("Admin-Logout erfolgreich")
                                .setDescription("Der Mitarbeiter " + employee.getAsMention() + " wurde von " + manager.getEffectiveName() + " ausgeloggt!")
                                .setColor(Color.MAGENTA.darker())
                                .build())
                        .queue();
            });
        }
    }
}
