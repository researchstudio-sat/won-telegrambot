package won.bot.telegrambot.util;

import won.bot.telegrambot.enums.MessagePropertyType;

import java.util.regex.Pattern;

public class TelegramContentExtractor {
    private Pattern demandTypePattern;
    private Pattern supplyTypePattern;
    private Pattern doTogetherTypePattern;
    private Pattern critiqueTypePattern;

    // Spring setter
    public void setDemandTypePattern(final Pattern demandTypePattern) {
        this.demandTypePattern = demandTypePattern;
    }

    public void setSupplyTypePattern(final Pattern supplyTypePattern) {
        this.supplyTypePattern = supplyTypePattern;
    }

    public void setDoTogetherTypePattern(final Pattern doTogetherTypePattern) {
        this.doTogetherTypePattern = doTogetherTypePattern;
    }

    public void setCritiqueTypePattern(final Pattern critiqueTypePattern) {
        this.critiqueTypePattern = critiqueTypePattern;
    }

    public MessagePropertyType getMessageContentType(String subject) {
        if (demandTypePattern.matcher(subject).matches()) {
            return MessagePropertyType.DEMAND;
        } else if (supplyTypePattern.matcher(subject).matches()) {
            return MessagePropertyType.OFFER;
        } else if (doTogetherTypePattern.matcher(subject).matches()) {
            return MessagePropertyType.BOTH;
        } else if (critiqueTypePattern.matcher(subject).matches()) {
            return MessagePropertyType.BOTH;
        }
        return null;
    }
}
