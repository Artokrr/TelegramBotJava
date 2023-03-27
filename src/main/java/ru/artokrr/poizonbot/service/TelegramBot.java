package ru.artokrr.poizonbot.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.artokrr.poizonbot.config.BotConfig;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;

    @Autowired
    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Начать работу с ботом"));
        listOfCommands.add(new BotCommand("/convert", "Узнать цену за товар"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bots command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String[] tokens = messageText.split("\\s+");
            if (messageText.matches("^/start\\s*$")) {
                startCommandReceived(chatId, update.getMessage().getChat().getUserName());
            }
            if (messageText.matches("^/convert\\s*$")) {
                sendMessage(chatId, "Введите цену товара в юанях с сайта Poizon.");
            }
            if (messageText.matches("^/convert\\s+\\d+(\\.\\d+)?\\s*$")) {
                try {
                    double yuan = Double.parseDouble(tokens[1]);
                    double ruble = yuan * 1.09 * 12.25 + 1000;
                    sendMessage(chatId, String.format("Окончательная стоимость заказа будет: %.2f", ruble));
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "Неправильные входные данные, возможно вы ввели буквы вместо цифр. " +
                            "Используйте: /convert, а затем следующим сообщением напишите цену в юанях с сайта Poizon");
                }
            }
            if (messageText.matches("^\\s*\\d+(\\.\\d+)?\\s*$")) {
                try {
                    double yuan = Double.parseDouble(messageText);
                    double ruble = yuan * 1.09 * 12.25 + 1000;
                    sendMessage(chatId, String.format("Окончательная стоимость заказа будет: %.2f", ruble));
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "Неправильные входные данные, возможно вы ввели буквы вместо цифр. " +
                            "Используйте: /convert, а затем следующим сообщением напишите цену в юанях с сайта Poizon");
                }
            }
        }
    }


    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, @" + name + "!"
                + "\nЯ помогу тебе рассчитать стоимость заказа и готов ответить на твои вопросы!"
                + "\nВведи /convert и стоимость в юанях, а я рассчитаю тебе окончательную стоимость заказа в рублях";

        log.info("Replied to user @" + name);

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());

        }
    }
}
