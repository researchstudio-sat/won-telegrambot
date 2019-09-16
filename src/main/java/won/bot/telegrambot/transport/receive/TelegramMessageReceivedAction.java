package won.bot.telegrambot.transport.receive;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.telegrambot.context.TelegramBotContextWrapper;
import won.bot.telegrambot.event.TelegramMessageReceivedEvent;
import won.bot.telegrambot.impl.WonTelegramBotHandler;
import won.bot.telegrambot.util.TelegramContentExtractor;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionModelMapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

public class TelegramMessageReceivedAction extends BaseEventBotAction {
    private TelegramContentExtractor telegramContentExtractor; // TODO: unused variable/parameter
    private WonTelegramBotHandler wonTelegramBotHandler;

    public TelegramMessageReceivedAction(EventListenerContext eventListenerContext,
                                         WonTelegramBotHandler wonTelegramBotHandler, TelegramContentExtractor telegramContentExtractor) {
        super(eventListenerContext);
        this.wonTelegramBotHandler = wonTelegramBotHandler;
        this.telegramContentExtractor = telegramContentExtractor;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventBus bus = getEventListenerContext().getEventBus();
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof TelegramMessageReceivedEvent
                        && ctx.getBotContextWrapper() instanceof TelegramBotContextWrapper) {
            TelegramBotContextWrapper botContextWrapper = (TelegramBotContextWrapper) ctx.getBotContextWrapper();
            Update update = ((TelegramMessageReceivedEvent) event).getUpdate();
            Message message = update.getMessage();
            CallbackQuery callbackQuery = update.getCallbackQuery();
            if (message != null && message.isCommand()) {
                wonTelegramBotHandler.getCommandRegistry().executeCommand(wonTelegramBotHandler, message);
            } else if (callbackQuery != null && update.hasCallbackQuery()) {
                message = callbackQuery.getMessage();
                String data = callbackQuery.getData();
                WonURI correspondingURI = botContextWrapper.getWonURIForMessageId(message.getMessageId());
                AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());
                switch (correspondingURI.getType()) {
                    case ATOM:
                        break;
                    case CONNECTION:
                        if ("0".equals(data)) { // CLOSE CONNECTION
                            Dataset connectionRDF = getEventListenerContext().getLinkedDataSource()
                                            .getDataForResource(correspondingURI.getUri());
                            Connection con = RdfUtils.findFirst(connectionRDF,
                                            x -> new ConnectionModelMapper().fromModel(x));
                            bus.publish(new CloseCommandEvent(con));
                            answerCallbackQuery.setText("Closed Connection");
                        } else if ("1".equals(data)) { // ACCEPT CONNECTION
                            Dataset connectionRDF = getEventListenerContext().getLinkedDataSource()
                                            .getDataForResource(correspondingURI.getUri());
                            URI targetAtom = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF,
                                            correspondingURI.getUri());
                            URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF,
                                            correspondingURI.getUri());
                            bus.publish(new ConnectCommandEvent(localAtom, targetAtom));
                            answerCallbackQuery.setText("Opened Connection");
                        }
                        break;
                }
                wonTelegramBotHandler.answerCallbackQuery(answerCallbackQuery);
                EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
                editMessageReplyMarkup.setMessageId(message.getMessageId());
                editMessageReplyMarkup.setChatId(message.getChatId());
                editMessageReplyMarkup.setReplyMarkup(null);
                wonTelegramBotHandler.editMessageReplyMarkup(editMessageReplyMarkup);
            } else if (message != null && message.isReply() && message.hasText()) {
                WonURI correspondingURI = botContextWrapper
                                .getWonURIForMessageId(message.getReplyToMessage().getMessageId());
                Dataset connectionRDF = getEventListenerContext().getLinkedDataSource()
                                .getDataForResource(correspondingURI.getUri());
                Connection con = RdfUtils.findFirst(connectionRDF, x -> new ConnectionModelMapper().fromModel(x));
                Model messageModel = WonRdfUtils.MessageUtils.textMessage(message.getText());
                bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
            }
        }
    }
}
