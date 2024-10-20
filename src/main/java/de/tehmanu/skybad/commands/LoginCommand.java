package de.tehmanu.skybad.commands;

import de.tehmanu.skybad.SkyBadBot;
import de.tehmanu.skybad.data.SkyBadRole;
import de.tehmanu.skybad.repository.WorkingTimeRepository;
import de.tehmanu.skybad.util.TimeUtil;
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
public class LoginCommand {

    private final WorkingTimeRepository workingTimeRepository;

    @SubscribeEvent
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("login")) {
            final Member employee = event.getMember();
            if (employee == null || employee.getUser().isBot()) {
                return;
            }
            if (TimeUtil.isLoginTimeValid()) {
                event.replyEmbeds(new EmbedBuilder().setTitle("Login fehlgeschlagen")
                                .setDescription("Du kannst dich erst wieder ab 00:00 Uhr einloggen!")
                                .setColor(Color.DARK_GRAY)
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
            if (!SkyBadBot.getInstance().getTrackedEmployees().add(employee.getIdLong())) {
                event.replyEmbeds(new EmbedBuilder().setTitle("Login fehlgeschlagen")
                                .setDescription("Du bist bereits eingeloggt!")
                                .setColor(Color.DARK_GRAY)
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
            this.workingTimeRepository.trackWorkingTime(employee.getIdLong(), "").whenComplete((v, throwable) -> {
                final Guild guild = employee.getGuild();
                final Role role = guild.getRoleById(SkyBadRole.CURRENTLY_ONLINE.getRoleId());
                if (role != null) {
                    guild.addRoleToMember(employee, role).queue();
                }
                event.replyEmbeds(new EmbedBuilder().setTitle("Erfolgreich eingeloggt")
                                .setDescription("Du hast dich nun als **" + employee.getEffectiveName() + "** eingeloggt. Vergiss nicht, dich nach der Schicht auszuloggen!")
                                .setColor(Color.GREEN)
                                .build())
                        .queue();
            });
        }
    }
}
