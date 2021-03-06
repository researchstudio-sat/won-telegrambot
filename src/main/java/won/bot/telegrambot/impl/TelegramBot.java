package won.bot.telegrambot.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.CloseBevahiour;
import won.bot.framework.eventbot.behaviour.ConnectBehaviour;
import won.bot.framework.eventbot.behaviour.ConnectionMessageBehaviour;
import won.bot.framework.eventbot.bus.EventBus;

import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.telegrambot.event.SendHelpEvent;
import won.bot.telegrambot.event.TelegramCreateAtomEvent;
import won.bot.telegrambot.event.TelegramMessageReceivedEvent;
import won.bot.telegrambot.transport.receive.TelegramMessageReceivedAction;
import won.bot.telegrambot.transport.send.*;
import won.bot.telegrambot.util.TelegramContentExtractor;
import won.bot.telegrambot.util.TelegramMessageGenerator;

import java.lang.invoke.MethodHandles;

/**
 * This Bot checks the Telegram-Messages sent to a given telegram-bot and
 * creates Atoms that represent the sent messages Created by fsuda on
 * 14.12.2016.
 */
public class TelegramBot extends EventBot {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String botName;
    private String token;
    private EventBus bus;
    private WonTelegramBotHandler wonTelegramBotHandler;
    @Autowired
    private TelegramContentExtractor telegramContentExtractor;
    @Autowired
    private TelegramMessageGenerator telegramMessageGenerator;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        telegramMessageGenerator.setEventListenerContext(ctx);
        bus = getEventBus();
        // Initiate Telegram Bot Handler
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            wonTelegramBotHandler = new WonTelegramBotHandler(bus, telegramMessageGenerator, botName, token);
            logger.debug("botName: " + wonTelegramBotHandler.getBotUsername());
            logger.debug("botTokn: " + wonTelegramBotHandler.getBotToken());
            telegramBotsApi.registerBot(wonTelegramBotHandler);
            BotBehaviour connectBehaviour = new ConnectBehaviour(ctx);
            connectBehaviour.activate();
            BotBehaviour closeBehaviour = new CloseBevahiour(ctx);
            closeBehaviour.activate();
            BotBehaviour connectionMessageBehaviour = new ConnectionMessageBehaviour(ctx);
            connectionMessageBehaviour.activate();
            // Telegram initiated Events
            bus.subscribe(TelegramMessageReceivedEvent.class, new ActionOnEventListener(ctx, "TelegramMessageReceived",
                            new TelegramMessageReceivedAction(ctx, wonTelegramBotHandler, telegramContentExtractor)));
            bus.subscribe(SendHelpEvent.class, new ActionOnEventListener(ctx, "TelegramHelpAction",
                            new TelegramHelpAction(ctx, wonTelegramBotHandler)));
            bus.subscribe(TelegramCreateAtomEvent.class, new ActionOnEventListener(ctx, "TelegramCreateAction",
                            new TelegramCreateAction(ctx, wonTelegramBotHandler, telegramContentExtractor)));
            // WON initiated Events
            bus.subscribe(AtomHintFromMatcherEvent.class, new ActionOnEventListener(ctx, "HintReceived",
                            new Hint2TelegramAction(ctx, wonTelegramBotHandler)));
            bus.subscribe(ConnectFromOtherAtomEvent.class, new ActionOnEventListener(ctx, "ConnectReceived",
                            new Connect2TelegramAction(ctx, wonTelegramBotHandler)));
            bus.subscribe(MessageFromOtherAtomEvent.class, new ActionOnEventListener(ctx, "ReceivedTextMessage",
                            new Message2TelegramAction(ctx, wonTelegramBotHandler)));
        } catch (TelegramApiRequestException e) {
            logger.error(e.getMessage());
        }
    }

    public void setBotName(final String botName) {
        this.botName = botName;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public void setTelegramContentExtractor(TelegramContentExtractor telegramContentExtractor) {
        this.telegramContentExtractor = telegramContentExtractor;
    }

    public void setTelegramMessageGenerator(TelegramMessageGenerator telegramMessageGenerator) {
        this.telegramMessageGenerator = telegramMessageGenerator;
    }
}
