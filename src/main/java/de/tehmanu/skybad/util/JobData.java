package de.tehmanu.skybad.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * @author TehManu
 * @since 18.10.2024
 */
@Setter
@Getter
@AllArgsConstructor
public class JobData {

    private String[] requirements;
    private boolean open;

    public MessageEmbed parseEmbed(final String jobTitle) {
        EmbedBuilder builder = new EmbedBuilder();
        for (int i = 0; i < this.requirements.length; i += 2) {
            builder.addField(this.requirements[i], this.requirements[i + 1], false);
        }
        return builder.build();
    }
}
