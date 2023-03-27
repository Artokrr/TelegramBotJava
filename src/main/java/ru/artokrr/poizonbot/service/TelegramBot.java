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
            String command = messageText.split(" ")[0];
            switch (command) {
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getUserName());
                    break;
                case "/convert":
                    double yuan, ruble;
                    String[] tokens = messageText.split(" ");
                    if (tokens.length != 2) {
                        sendMessage(chatId, "Неправильное использование. Используйте: /convert [цена в юанях с сайта Poizon]");
                    } else {
                        try {
                            yuan = Double.parseDouble(tokens[1]);
                            ruble = yuan * 1.09 * 12.25 + 1000;
                            sendMessage(chatId, String.format("Окончательная стоимость за товар будет: %.2f", ruble));
                        } catch (NumberFormatException e) {
                            sendMessage(chatId, "Неправильные входные данные, возможно вы ввели буквы вместо цифр. Используйте: /convert [цена в юанях с сайта Poizon]");
                        }
                    }
                    break;
                default:
                    sendMessage(chatId, "Не понимаю Вас. Ознакомьтесь с меню команд");
            }
        }

    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, @" + name + "!"
                + "\nЯ помогу тебе рассчитать стоимость заказа и готов ответить на твои вопросы!"
                + "\nВведи /convert и стоимость в юанях, а я рассчитаю тебе окончательную стоимость за товар в рублях";

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
