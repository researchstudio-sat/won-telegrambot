package won.bot.telegrambot.commands;

import org.apache.commons.lang3.ArrayUtils;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commands.BotCommand;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.telegrambot.event.TelegramCreateAtomEvent;

/**
 * Created by fsuda on 15.12.2016.
 */
public class OfferBotCommand extends BotCommand {
    private EventBus bus;

    public OfferBotCommand(String commandIdentifier, String description, EventBus bus) {
        super(commandIdentifier, description);
        this.bus = bus;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        strings = ArrayUtils.add(strings, 0, "[OFFER]");
        bus.publish(new TelegramCreateAtomEvent(absSender, user, chat, strings));
    }
}