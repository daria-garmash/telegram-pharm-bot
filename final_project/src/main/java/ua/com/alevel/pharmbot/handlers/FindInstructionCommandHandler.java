package ua.com.alevel.pharmbot.handlers;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ua.com.alevel.pharmbot.bot.state.PharmBotState;
import ua.com.alevel.pharmbot.cache.UserDataCache;
import ua.com.alevel.pharmbot.service.MedicineInstructionService;
import ua.com.alevel.pharmbot.service.ReplyMessageService;
import ua.com.alevel.templates.MessageTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class FindInstructionCommandHandler implements MessageHandler {

    private final UserDataCache cache;
    private final MedicineInstructionService instructionService;
    private final ReplyMessageService replyService;


    public FindInstructionCommandHandler(UserDataCache cache,
                                         MedicineInstructionService instructionService,
                                         ReplyMessageService replyService) {
        this.cache = cache;
        this.instructionService = instructionService;
        this.replyService = replyService;
    }

    @Override
    public SendMessage handle(Message message) {
        Long chatId = message.getChatId();
        if(cache.getUsersCurrentBotState(chatId).equals(PharmBotState.INSTRUCTION_SEARCH)) {
            cache.setUsersCurrentBotState(chatId, PharmBotState.ASK_NEEDED_MEDICINE_NAME);
        }
        return process(message);
    }

    private SendMessage process(Message message) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        String answer = message.getText();
        Long chatId = message.getChatId();
        PharmBotState state = cache.getUsersCurrentBotState(chatId);
        SendMessage reply = replyService.getWarningReplyMessage(chatId, MessageTemplates.NOT_FOUND_MESSAGE);
        String name;


        if (state.equals(PharmBotState.ASK_NEEDED_MEDICINE_NAME)) {
            reply = replyService.getReplyMessage(chatId, MessageTemplates.ASK_MEDS_NAME_MESSAGE);
            cache.setUsersCurrentBotState(chatId, PharmBotState.ASK_MEDICINE_FORM_NAME);
            return reply;
        }
        if (state.equals(PharmBotState.ASK_MEDICINE_FORM_NAME)) {
            name = answer;
            Set<String> forms = instructionService.findAllExistingForms(name);
            if (forms == null || forms.isEmpty()) {
                reply = replyService.getWarningReplyMessage(chatId, MessageTemplates.NOT_FOUND_MESSAGE);
                cache.setUsersCurrentBotState(chatId, PharmBotState.SHOW_MAIN_MENU);
                return reply;
            } else  {
                List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

                for (String f : forms) {
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    row.add(new InlineKeyboardButton().setText(f).setCallbackData(f + " " + name));
                    keyboard.add(row);
                }
                markup.setKeyboard(keyboard);
                cache.setUsersCurrentBotState(chatId, PharmBotState.MEDICINE_FORM_NAME_RECEIVED);
                reply = replyService.getReplyMessageWithMarkup(chatId, MessageTemplates.ASK_MEDS_FORM_MESSAGE, markup);
            }
        }

        return reply;
    }

    @Override
    public PharmBotState getName() {
        return PharmBotState.INSTRUCTION_SEARCH;
    }
}
