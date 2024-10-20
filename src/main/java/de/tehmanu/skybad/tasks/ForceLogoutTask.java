package de.tehmanu.skybad.tasks;

import de.tehmanu.skybad.SkyBadBot;
import de.tehmanu.skybad.data.SkyBadRole;
import de.tehmanu.skybad.repository.WorkingTimeRepository;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.util.Iterator;
import java.util.TimerTask;

/**
 * @author TehManu
 * @since 16.10.2024
 */
@AllArgsConstructor
public class ForceLogoutTask extends TimerTask {

    private final WorkingTimeRepository workingTimeRepository;

    @Override
    public void run() {
        for (Iterator<Long> iterator = SkyBadBot.getInstance().getTrackedEmployees().iterator(); iterator.hasNext(); ) {
            final long userId = iterator.next();
            SkyBadBot.getLogger().debug("processing: {}", userId);
            this.workingTimeRepository.untrackWorkingTime(userId, "force-logout");
            final Guild guild = SkyBadBot.getInstance().getJDA().getGuildById(SkyBadBot.GUILD_ID);
            if (guild != null) {
                guild.retrieveMemberById(userId).queue(member -> {
                    final Role role = guild.getRoleById(SkyBadRole.CURRENTLY_ONLINE.getRoleId());
                    if (role != null) {
                        guild.removeRoleFromMember(member, role).queue();
                    }
                    final TextChannel channel = guild.getTextChannelById(1293526864219865091L);
                    if (channel != null) {
                        channel.sendMessageEmbeds(new EmbedBuilder().setTitle("Force-Logout erfolgreich")
                                .setDescription("Der Mitarbeiter **" + member.getEffectiveName() + "** wurde vom System ausgeloggt.")
                                .setColor(Color.ORANGE)
                                .build()
                        ).queue();
                    }
                    member.getUser().openPrivateChannel()
                            .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(new EmbedBuilder()
                                    .setTitle("SkyBad × Force-Logout erfolgreich")
                                    .setDescription("Du wurdest vom System ausgeloggt, da dieser Arbeitstag nun zu ende ist.")
                                    .build())
                            ).queue();
                });
            }
            iterator.remove();
        }
        cancel();
    }
}
