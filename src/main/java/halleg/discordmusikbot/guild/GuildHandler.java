package halleg.discordmusikbot.guild;

import halleg.discordmusikbot.MusicBot;
import halleg.discordmusikbot.buttons.ButtonManager;
import halleg.discordmusikbot.commands.CommandManager;
import halleg.discordmusikbot.player.Player;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.PermissionUtil;

import java.util.function.Consumer;

public class GuildHandler {

    public static final String REMOVE_EMOJI = "❌";
    public static final String RESUME_PAUSE_EMOJI = "⏯";
    public static final String REPEAT_EMOJI = "🔁";
    public static final String SKIP_EMOJI = "⏭";
    public static final String REMOVE_ALL_EMOJI = "❎";
    public static final String SHUFFLE_EMOJI = "\uD83D\uDD00";

    private Guild guild;

    private TextChannel output;
    private String prefix;

    private Player player;

    private MusicBot bot;

    private MessageBuilder builder;

    private CommandManager commands;
    private ButtonManager buttons;

    public GuildHandler(MusicBot musicbot, Guild guild, long channelid, String prefix) {
        this.prefix = prefix;
        this.bot = musicbot;
        this.guild = guild;
        this.output = guild.getTextChannelById(channelid);
        this.builder = new MessageBuilder(this);
        this.player = new Player(this, musicbot.getManager());
        this.commands = new CommandManager(this);
        this.buttons = new ButtonManager(this);

        if (this.output == null) {
            setChannel(guild.getTextChannels().get(0));
        } else {
            clearLastMessages(this.output);
        }
        log("initialized! outputchannel: " + this.output.getName() + " prefix: " + prefix);
    }

    public void setChannel(TextChannel channel) {
        this.output = channel;
        log("set channel to: " + channel.getName());
        sendInfoMessage("This is now the prefered channel.");
        this.bot.saveConfig(this);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        sendInfoMessage("Commands for this bot now have to start with `" + this.prefix + "`.");
        this.bot.saveConfig(this);
    }

    private void clearLastMessages(TextChannel channel) {
        /*
        if (!PermissionUtil.checkPermission(channel, this.guild.getSelfMember(), Permission.MESSAGE_HISTORY)) {
            log("not permissione to view history, cant clean up!");
            return;
        }
        log("cleaning up...");
        channel.getHistoryBefore(channel.getLatestMessageId(), 20).queue(new Consumer<MessageHistory>() {
            @Override
            public void accept(MessageHistory t) {
                for (Message message : t.getRetrievedHistory()) {

                    if (message.getAuthor().getIdLong() != message.getJDA().getSelfUser().getIdLong()) {
                        return;
                    }

                    for (MessageReaction react : message.getReactions()) {
                        if (react.isSelf()) {
                            GuildHandler.this.builder.setPlayed(message);
                        }
                    }
                }

            }
        });

        if (channel.hasLatestMessage()) {
            channel.retrieveMessageById(channel.getLatestMessageId()).queue(new Consumer<Message>() {
                @Override
                public void accept(Message message) {
                    for (MessageReaction react : message.getReactions()) {
                        if (react.isSelf()) {
                            GuildHandler.this.builder.setPlayed(message);
                        }
                    }
                }
            });
        }
        */
    }

    public void handleMessage(GuildMessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().startsWith(this.prefix)) {
            this.commands.handleCommand(event.getMessage());
        } else {
            this.player.loadAndQueueAndJoin(event.getMessage().getContentRaw(), event.getMember());
            delete(event.getMessage());
        }
    }

    public void handleReaction(final MessageReaction react, Message message, final Member member) {

        if (message.getChannel().getIdLong() != this.output.getIdLong()) {
            return;
        }

        GuildHandler.this.buttons.handleReaction(message, react, member);
    }


    public void voiceUpdate() {
        this.player.voiceUpdate();
    }

    public void delete(Message message) {
        try {
            message.delete().queue();
        } catch (InsufficientPermissionException e) {
            log("insuficciant permissions to delete message!");
            return;
        }
    }

    public void sendErrorMessage(String error) {
        queue(this.output, this.builder.buildNewErrorMessage(error), null);
    }

    public void sendInfoMessage(String message) {
        complete(this.output, this.builder.buildInfoMessage(message));
    }

    public void sendHelpMessage(MessageChannel channel) {
        complete(channel, this.builder.buildHelpMessage());
    }

    public Message complete(MessageEmbed message) {
        return complete(this.output, message);
    }

    private Message complete(MessageChannel channel, MessageEmbed message) {
        MessageAction act = send(channel, message);
        if (act != null) {
            return act.complete();
        }
        return null;
    }

    public void queue(MessageEmbed message, Consumer<Message> consuber) {
        queue(this.output, message, consuber);
    }

    private void queue(MessageChannel channel, MessageEmbed message, Consumer<Message> consuber) {
        MessageAction act = send(channel, message);
        if (act != null) {
            act.queue(consuber);
        }
    }

    private MessageAction send(MessageChannel channel, MessageEmbed message) {
        try {
            return channel.sendMessage(message);
        } catch (InsufficientPermissionException e) {
            log("insuficciant permissions to send Message!");
        } catch (NullPointerException e) {
            log("output not initialized!");
        }

        return null;
    }

    public void log(String string) {
        System.out.println("[" + this.guild.getName() + "] (" + Thread.currentThread().getId() + ") " + string);
    }

    public boolean reactionPermissionCheck() {
        boolean ret = PermissionUtil.checkPermission(this.output, this.guild.getSelfMember(),
                Permission.MESSAGE_ADD_REACTION);
        if (!ret) {
            log("missing reaction manage permission, continuing anyways...");
        }
        return ret;
    }

    public Guild getGuild() {
        return this.guild;
    }

    public Player getPlayer() {
        return this.player;
    }

    public TextChannel getChannel() {
        return this.output;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public MessageBuilder getBuilder() {
        return this.builder;
    }

    public CommandManager getCommands() {
        return this.commands;
    }

    public ButtonManager getButtons() {
        return this.buttons;
    }


}
