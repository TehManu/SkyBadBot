package de.tehmanu.skybad.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.awt.*;
import java.util.Arrays;

/**
 * @author TehManu
 * @since 18.10.2024
 */
@Getter
@AllArgsConstructor
public enum Job {

    LIFEGUARD("Bademeister", Color.BLUE, "\uD83D\uDCA6"),
    CASHIER("Kassierer", Color.MAGENTA, "\uD83D\uDCB8"),
    TECHNICIAN("Techniker", Color.BLUE.brighter(), "\uD83D\uDEE0\uFE0F"),
    BUILDING("Building", Color.GREEN, "\uD83D\uDCCF"),
    ASSISTANCE("Aushilfe", Color.CYAN, "\uD83D\uDD50"),
    CHATGUARD("Chatwächter", Color.GREEN, "\uD83D\uDCAC"),
    ;

    private final String displayName;
    private final Color color;
    private final String emoji;

    public SelectOption toSelectOption() {
        return SelectOption.of(this.displayName, this.displayName)
                .withEmoji(Emoji.fromUnicode(this.emoji))
                .withDescription("Informationen zum Bereich " + this.displayName);
    }

    public static Job valueOfDisplayName(String displayName) {
        return Arrays.stream(Job.values()).filter(job -> job.getDisplayName().equalsIgnoreCase(displayName))
                .findFirst()
                .orElse(null);
    }
}
