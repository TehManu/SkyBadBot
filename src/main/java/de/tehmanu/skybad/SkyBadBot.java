package de.tehmanu.skybad;

import de.tehmanu.skybad.commands.*;
import de.tehmanu.skybad.datasource.PostgreSQLDataSource;
import de.tehmanu.skybad.listener.ApplicationListener;
import de.tehmanu.skybad.manager.ApplicationManager;
import de.tehmanu.skybad.repository.ApplicationRepository;
import de.tehmanu.skybad.repository.WorkingTimeRepository;
import de.tehmanu.skybad.tasks.FileChangeTask;
import de.tehmanu.skybad.tasks.ForceLogoutTask;
import de.tehmanu.skybad.tasks.InactivityTask;
import de.tehmanu.skybad.util.Job;
import de.tehmanu.skybad.util.JobData;
import de.tehmanu.skybad.util.TimeUtil;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author TehManu
 * @since 09.10.2024
 */
@Getter
public class SkyBadBot {

    public static final long GUILD_ID = 1293526864219865088L;

    @Getter
    private static SkyBadBot instance;
    @Getter
    private static Logger logger;

    private final JDA JDA;
    private final PostgreSQLDataSource dataSource;
    private final WorkingTimeRepository workingTimeRepository;
    private final ApplicationRepository applicationRepository;

    private final ApplicationManager applicationManager;

    private final Map<Job, JobData> availableJobs = new HashMap<>();
    private final Set<Long> trackedEmployees = new HashSet<>();
    private final ExecutorService service = Executors.newFixedThreadPool(4);

    static void startup(String[] args) throws InterruptedException {
        new SkyBadBot(args);
    }

    private SkyBadBot(String[] args) throws InterruptedException {
        SkyBadBot.instance = this;
        SkyBadBot.logger = LoggerFactory.getLogger(SkyBadBot.class);
        this.JDA = JDABuilder.createDefault(args[0])
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();

        this.JDA.awaitReady();
        this.JDA.setEventManager(new AnnotatedEventManager());

        this.JDA.updateCommands().addCommands(commandList()).queue();

        this.dataSource = new PostgreSQLDataSource();
        this.workingTimeRepository = new WorkingTimeRepository(this.dataSource, this.service);
        this.applicationRepository = new ApplicationRepository(this);

        this.applicationManager = new ApplicationManager(this.applicationRepository);
        this.applicationManager.loadAvailableJobs();
        this.applicationManager.loadJobs();

        final InactivityTask inactivityTask = new InactivityTask(this.workingTimeRepository);
        final ForceLogoutTask forceLogoutTask = new ForceLogoutTask(this.workingTimeRepository);

        Timer timer = new Timer(true);
        timer.schedule(inactivityTask, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));
        timer.schedule(forceLogoutTask, TimeUtil.convertToDate("00:00:00"));

        this.service.submit(new FileChangeTask());

        this.JDA.addEventListener(new LoginCommand(this.workingTimeRepository));
        this.JDA.addEventListener(new LogoutCommand(this.workingTimeRepository));
        this.JDA.addEventListener(new AdminLogoutCommand(this.workingTimeRepository));
        this.JDA.addEventListener(new LogtimeCommand(this.workingTimeRepository));
        this.JDA.addEventListener(new ApplyCommand());
        this.JDA.addEventListener(new ApplicationListener());
        this.JDA.addEventListener(inactivityTask);
    }

    private List<CommandData> commandList() {
        List<CommandData> commands = new ArrayList<>();

        commands.add(Commands.slash("login", "Beginnt, die Arbeitszeit zu zählen.").setGuildOnly(true));
        commands.add(Commands.slash("logout", "Beendet, die Arbeitszeit zu zählen.").setGuildOnly(true));
        commands.add(Commands.slash("adminlogout", "Logout an employee.")
                .addOption(OptionType.USER, "user", "User to logout", true)
                .setGuildOnly(true)
        );
        commands.add(Commands.slash("logtime", "Zeigt die Arbeitszeit")
                .addOption(OptionType.USER, "employee", "Mitarbeiter", true)
                .setGuildOnly(true)
        );
        return commands;
    }
}
