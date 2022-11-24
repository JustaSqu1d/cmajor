package adris.altoclef.dpc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.ui.MessagePriority;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.minecraft.client.MinecraftClient;

import java.util.EnumSet;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

/**
 * The Dpc system lets authorized players send commands to the bot to execute.
 * <p>
 * This effectively makes the bot function as a servant, or Dpc.
 * <p>
 * Authorization is defined in "altoclef_Dpc_whitelist.txt" and "altoclef_Dpc_blacklist.txt"
 * and depends on the "useDpcWhitelist" and "useDpcBlacklist" settings in "altoclef_settings.json"
 */
public class Dpc extends ListenerAdapter {

    private static final String Dpc_MESSAGE_START = "` ";
    private final AltoClef _mod;
    private final UserAuth _userAuth;
    private String _currentUser = null;
    // Utility variables for command logic
    private boolean _commandInstantRan = false;
    private boolean _commandFinished = false;

    public Dpc(AltoClef mod) {
        _mod = mod;
        _userAuth = new UserAuth(mod);

        String botToken= DpcConfig.getInstance().botToken;

        JDA jda = JDABuilder.createLight(botToken, EnumSet.noneOf(GatewayIntent.class)) // slash commands don't need any intents
                .addEventListeners(new Dpc(mod))
                .build();

        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                Commands.slash("run", "Makes the bot run a command.")
                        .addOption(STRING, "content", "A valid command with arguments", true) // you can add required options like this too
        );

        commands.queue();

        // Revoke our current user whenever a task finishes.
        EventBus.subscribe(TaskFinishedEvent.class, evt -> {
            if (_currentUser != null) {
                _currentUser = null;
            }
        });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Only accept commands from guilds
        if (event.getGuild() == null)
            return;
        if ("run".equals(event.getName())) {
            String message = event.getOption("content").getAsString();
            _mod.getDpc().processCommand(event, message);
        }
    }

    private void processCommand(SlashCommandInteractionEvent event, String message){
        event.deferReply(true).queue(); // Let the user know we received the command before doing anything else
        InteractionHook hook = event.getHook(); // This is a special webhook that allows you to send messages without having permissions in the channel and also allows ephemeral messages
        hook.setEphemeral(true); // All messages here will now be ephemeral implicitly

        Member member = event.getMember();

        boolean debug = DpcConfig.getInstance().whisperFormatDebug;

        if (debug) {
            Debug.logMessage("RECEIVED WHISPER: \"" + message + "\".");
        }

        if (_userAuth.isUserAuthorized(member)) {
            // TODO
            executeWhisper(member, message);
        } else {
            if (debug) {
                Debug.logMessage("    Rejecting: User \"" + member + "\" is not authorized.");
            }
            if (DpcConfig.getInstance().sendAuthorizationResponse) {
                hook.sendMessage("You don't have the required permissions.").queue();
                return;
            }
        }
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isUserAuthorized(Member username) {
        return _userAuth.isUserAuthorized(username);
    }

    public void onLog(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper(message, priority);
        }
    }

    public void onLogWarning(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper("[WARNING:] " + message, priority);
        }
    }

    public void tick() {
        // Nothing for now.
    }

    public String getCurrentUser() {
        return _currentUser;
    }

    public boolean hasCurrentUser() {
        return _currentUser != null;
    }

    private void executeWhisper(Member member, String message) {
        String prevUser = _currentUser;
        _commandInstantRan = true;
        _commandFinished = false;
        _currentUser = member.getId();
        sendWhisper("Command Executing: " + message, MessagePriority.TIMELY);
        String prefix = DpcConfig.getInstance().requirePrefixMsg ? _mod.getModSettings().getCommandPrefix() : "";
        AltoClef.getCommandExecutor().execute(prefix + message, () -> {
            // On finish
            sendWhisper("Command Finished: " + message, MessagePriority.TIMELY);
            if (!_commandInstantRan) {
                _currentUser = null;
            }
            _commandFinished = true;
        }, e -> {
            sendWhisper("TASK FAILED: " + e.getMessage(), MessagePriority.ASAP);
            e.printStackTrace();
            _currentUser = null;
            _commandInstantRan = false;
        });
        _commandInstantRan = false;
        // Only set the current user if we're still running.
        if (_commandFinished) {
            _currentUser = prevUser;
        }
    }

    private void sendWhisper(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper(_currentUser, message, priority);
        } else {
            Debug.logWarning("Failed to send Dpc message as there are no users present: " + message);
        }
    }

    private void sendWhisper(String username, String message, MessagePriority priority) {
        _mod.getMessageSender().enqueueWhisper(username, Dpc_MESSAGE_START + message, priority);
    }
}
