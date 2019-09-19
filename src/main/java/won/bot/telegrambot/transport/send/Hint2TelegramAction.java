package won.bot.telegrambot.transport.send;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.BotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.telegrambot.context.TelegramBotContextWrapper;
import won.bot.telegrambot.enums.UriType;
import won.bot.telegrambot.impl.WonTelegramBotHandler;
import won.bot.telegrambot.util.model.WonURI;
import won.protocol.message.WonMessage;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Hint2TelegramAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    WonTelegramBotHandler wonTelegramBotHandler;

    public Hint2TelegramAction(EventListenerContext ctx, WonTelegramBotHandler wonTelegramBotHandler) {
        super(ctx);
        this.wonTelegramBotHandler = wonTelegramBotHandler;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if ((event instanceof AtomHintFromMatcherEvent || event instanceof SocketHintFromMatcherEvent)
                        && ctx.getBotContextWrapper() instanceof TelegramBotContextWrapper) {
            TelegramBotContextWrapper botContextWrapper = (TelegramBotContextWrapper) ctx.getBotContextWrapper();
            WonMessage wonMessage = ((MessageEvent) event).getWonMessage();
            Optional<URI> yourAtomUri = BotActionUtils.getRecipientAtomURIFromHintEvent(event,
                            getEventListenerContext().getLinkedDataSource());
            Optional<URI> targetAtomUri = BotActionUtils.getTargetAtomURIFromHintEvent(event,
                            getEventListenerContext().getLinkedDataSource());
            if (!(yourAtomUri.isPresent())) {
                logger.info("could not extract recipient atom URI from hint event {}", event);
                return;
            }
            if (!(targetAtomUri.isPresent())) {
                logger.info("could not extract target atom URI from hint event {}", event);
                return;
            }
            Long chatId = botContextWrapper.getChatIdForURI(yourAtomUri.get());
            if (chatId == null) {
                logger.error("No chatId found for the specified atomUri");
                return;
            }
            try {
                Message message = wonTelegramBotHandler.sendMessage(wonTelegramBotHandler.getTelegramMessageGenerator()
                                .getHintMessage(chatId, targetAtomUri.get(), yourAtomUri.get()));
                if (event instanceof SocketHintFromMatcherEvent) {
                    botContextWrapper.addMessageIdWonURIRelation(message.getMessageId(),
                                    new WonURI(wonMessage.getRecipientURI(), UriType.CONNECTION));
                }
            } catch (TelegramApiException te) {
                logger.error(te.getMessage());
            }
        }
    }
}
