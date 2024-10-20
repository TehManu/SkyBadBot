package de.tehmanu.skybad.listener;

import de.tehmanu.skybad.SkyBadBot;
import de.tehmanu.skybad.util.Job;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author TehManu
 * @since 20.10.2024
 */
public class ApplicationListener {

    private final List<String> tokens = new ArrayList<>();

    @SubscribeEvent
    public void onStringSelectionInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("job-selection")) {
            String jobTitle = event.getValues().getFirst();
            Map<Job, String[]> jobs = SkyBadBot.getInstance().getApplicationManager().getJobs();

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(jobTitle + " -> Informationen zum Rang")
                    .setDescription("Du möchtest " + jobTitle + " bei uns werden? Hier findest du alle Informationen darüber!")
                    .setColor(Job.valueOfDisplayName(jobTitle).getColor());

            String[] blocks = jobs.get(Job.valueOfDisplayName(jobTitle));
            for (int i = 0; i < blocks.length; i += 2) {
                embed.addField(blocks[i], blocks[i + 1].translateEscapes(), false);
            }

            event.replyEmbeds(embed.build())
                    .addActionRow(Button.success("apply", "Bewerben"))
                    .setEphemeral(true)
                    .queue();
            event.editSelectMenu(event.getSelectMenu()).queue();
        }
    }

    @SubscribeEvent
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("apply")) {
            tokens.add(event.getToken());
            TextInput introduce = TextInput.create("introduce", "Bitte stelle dich einmal kurz vor", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Vorname, Minecraft-Name, Alter, Hobbys, Interessen und spätere Berufswünsche")
                    .build();

            TextInput abilities = TextInput.create("2", "Was sind deine Stärken und Schwächen?", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Bitte nenne mindestens zwei Stärken & eine Schwäche! Hierzu zählen keine Ingame-Stärken/Schwächen.")
                    .build();

            Modal applyForm = Modal.create("application-form", "Bewerbung")
                    .addComponents(ActionRow.of(introduce), ActionRow.of(abilities))
                    .build();
            event.replyModal(applyForm).queue();
        }
    }

    @SubscribeEvent
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("application-form")) {
            InteractionHook.from(event.getJDA(), tokens.getFirst()).editOriginalComponents().queue();
            tokens.clear();
            final MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Deine Bewerbung wurde erfolgreich an uns versendet!")
                    .setDescription("Wir werden sie bearbeiten und dich dann benachrichtigen, ob und warum deine Bewerbung angenommen oder abgelehnt wurde. \n" +
                                    "Die Bearbeitung deiner Bewerbung kann einige Zeit dauern und erfordert möglicherweise Absprachen. Bitte habe daher etwas Geduld. Innerhalb von sieben Tagen solltest du eine Antwort erhalten.")
                    .build();

            event.replyEmbeds(embed).setEphemeral(true).queue();
            TextChannel channel = event.getGuild().getTextChannelById(1293526864219865091L);

            int embedSize = Math.max(1, event.getValues().size() / 4000);

            System.out.println(embedSize);

            SkyBadBot.getInstance().getJDA().retrieveUserById(831519576646877224L).queue(user -> {
                MessageEmbed firstEmbed = new EmbedBuilder().setTitle("Bademeister Bewerbung")
                        .setAuthor(user.getEffectiveName(), Paths.get("discord://discord.com/users/" + user.getId()).toString(), user.getEffectiveAvatarUrl())
                        .setColor(Color.YELLOW)
                        .setDescription("von " + event.getUser().getAsMention()
                                        + "\n" +
                                        "\n**Bitte stelle dich einmal kurz vor (Vorname, Minecraft-Name, Alter, Hobbys, Interessen und spätere Berufswünsche)**" +
                                        "\n" + event.getValues().getFirst().getAsString())
                        .build();
                MessageEmbed secondEmbed = new EmbedBuilder()
                        .setColor(Color.YELLOW)
                        .setDescription("**Was sind deine Stärken und Schwächen? (Bitte nenne mindestens zwei Stärken & eine Schwäche! Hierzu zählen keine Ingame-Stärken/Schwächen.)**" +
                                        "\n" + event.getValues().getLast().getAsString())
                        .build();

                channel.sendMessageEmbeds(firstEmbed, secondEmbed).queue();

//                channel.sendMessageEmbeds(new EmbedBuilder().setTitle("Bademeister Bewerbung")
//                                .setDescription("von " + event.getUser().getAsMention())
//                                .setColor(Color.GREEN)
//                                .addField("", "test", false)
//                                .addField("Bitte stelle dich einmal kurz vor (Vorname, Minecraft-Name, Alter, Hobbys, Interessen und spätere Berufswünsche)", event.getValue("introduce").getAsString(), false)
//                                .addField("Was sind deine Stärken und Schwächen? (Bitte nenne mindestens zwei Stärken & eine Schwäche! Hierzu zählen keine Ingame-Stärken/Schwächen.)", event.getValue("introduce").getAsString(), false)
//                                .setFooter("Angenommen von Hufflepuff2002", user.getAvatarUrl())
//                                .build())
//                        .addActionRow(Button.success("accept", "Annehmen"), Button.danger("deny", "Ablehnen"))
//                        .queue();
            });
        }
    }
}
