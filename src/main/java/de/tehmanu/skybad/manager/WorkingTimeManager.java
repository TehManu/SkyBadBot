package de.tehmanu.skybad.manager;

import de.tehmanu.skybad.data.SkyBadRole;
import de.tehmanu.skybad.repository.WorkingTimeRepository;
import de.tehmanu.skybad.util.TimeUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author TehManu
 * @since 20.10.2024
 */
@Getter
@AllArgsConstructor
public class WorkingTimeManager {

    private final WorkingTimeRepository workingTimeRepository;

    private final Set<Long> trackedEmployees = new HashSet<>();

    public void login(final Member member, final BiConsumer<MessageEmbed, Boolean> messages) {
        if (TimeUtil.isLoginTimeValid()) {
            final MessageEmbed loginError = new EmbedBuilder().setTitle("Login fehlgeschlagen")
                    .setDescription("Du kannst dich erst wieder ab 00:00 Uhr einloggen!")
                    .setColor(Color.DARK_GRAY)
                    .build();
            messages.accept(loginError, true);
            return;
        }
        if (!this.trackedEmployees.add(member.getIdLong())) {
            final MessageEmbed loginError = new EmbedBuilder().setTitle("Login fehlgeschlagen")
                    .setDescription("Du bist bereits eingeloggt!").setColor(Color.DARK_GRAY)
                    .build();
            messages.accept(loginError, true);
            return;
        }
        this.workingTimeRepository.trackWorkingTime(member.getIdLong(), "");
        final Guild guild = member.getGuild();
        final Role role = guild.getRoleById(SkyBadRole.CURRENTLY_ONLINE.getRoleId());
        if (role != null) {
            guild.addRoleToMember(member, role).queue();
        }
        final MessageEmbed loginSuccess = new EmbedBuilder().setTitle("Erfolgreich eingeloggt")
                .setDescription("Du hast dich nun als **" + member.getEffectiveName() + "** eingeloggt. Vergiss nicht, dich nach der Schicht auszuloggen!")
                .setColor(Color.GREEN)
                .build();
        messages.accept(loginSuccess, false);
    }

    public void logout(final Member member, final BiConsumer<MessageEmbed, Boolean> messages) {
        if (!this.trackedEmployees.remove(member.getIdLong())) {
            final MessageEmbed logoutError = new EmbedBuilder().setTitle("Logout fehlgeschlagen")
                    .setDescription("Du bist nicht eingeloggt!").setColor(Color.DARK_GRAY)
                    .build();
            messages.accept(logoutError, true);
            return;
        }
        this.workingTimeRepository.untrackWorkingTime(member.getIdLong(), "");
        final Guild guild = member.getGuild();
        final Role role = guild.getRoleById(SkyBadRole.CURRENTLY_ONLINE.getRoleId());
        if (role != null) {
            guild.removeRoleFromMember(member, role).queue();
        }
        final MessageEmbed logoutSucces = new EmbedBuilder().setTitle("Erfolgreich ausgeloggt")
                .setDescription("Du hast dich nun als **" + member.getEffectiveName() + "** ausgeloggt. Einen schönen Tag dir noch!")
                .setColor(Color.RED)
                .build();
        messages.accept(logoutSucces, false);
    }

    public void adminLogout(final Member employee, final Member manager, final BiConsumer<MessageEmbed, Boolean> messages) {
        if (employee.getIdLong() == manager.getIdLong()) {
            final MessageEmbed logoutSelfError = new EmbedBuilder().setTitle("Admin-Logout fehlgeschlagen")
                    .setDescription("Bitte verwende /logout um dich auszuloggen.")
                    .setColor(Color.DARK_GRAY)
                    .build();
            messages.accept(logoutSelfError, true);
            return;
        }
        if (!this.trackedEmployees.remove(employee.getIdLong())) {
            final MessageEmbed logoutError = new EmbedBuilder().setTitle("Admin-Logout fehlgeschlagen")
                    .setDescription("Der Mitarbeiter " + employee.getEffectiveName() + " ist nicht eingeloggt!")
                    .setColor(Color.DARK_GRAY)
                    .build();
            messages.accept(logoutError, true);
            return;
        }
        this.workingTimeRepository.untrackWorkingTime(employee.getIdLong(), "admin-logout");
        final Guild guild = employee.getGuild();
        final Role role = guild.getRoleById(SkyBadRole.CURRENTLY_ONLINE.getRoleId());
        if (role != null) {
            guild.removeRoleFromMember(employee, role).queue();
        }
        final MessageEmbed logoutSuccess = new EmbedBuilder().setTitle("Admin-Logout erfolgreich")
                .setDescription("Der Mitarbeiter " + employee.getAsMention() + " wurde von " + manager.getEffectiveName() + " ausgeloggt!")
                .setColor(Color.MAGENTA.darker())
                .build();
        messages.accept(logoutSuccess, false);
    }
}
