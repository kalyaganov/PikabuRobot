package ru.futurobot.telegram.pikabubot;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.val;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.telegram.telegrambots.api.methods.ActionType;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendChatAction;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 * Created by Alexey on 08.04.17.
 * Futurobot
 */
public class PikabuBot extends TelegramLongPollingBot {

  private static String COMMAND_START = "/start";
  private static String COMMAND_TOP = "/top";
  private static String COMMAND_RANDOM = "/random";
  private static String COMMAND_SEARCH = "/search";
  private static String COMMAND_TAG = "/tag";

  private BotConfiguration configuration;
  private OkHttpClient okHttpClient;
  private Random random = new Random(System.currentTimeMillis());

  public PikabuBot(BotConfiguration botConfiguration) throws IOException {
    this.configuration = botConfiguration;
    this.okHttpClient = new OkHttpClient.Builder()
        .readTimeout(3, TimeUnit.SECONDS)
        .cache(new Cache(File.createTempFile("temp", "file"), 10 * 1024 * 1024))
        .addInterceptor(chain -> {
          Request request = chain.request().newBuilder()
              .addHeader("User-Agent",
                  "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
              .build();

          return chain.proceed(request);
        })
        .build();
  }

  @Override public void onUpdateReceived(Update update) {
    if (update.hasMessage()) {
      //Store message and user
      Message message = update.getMessage();
      //Handle message
      if (message.hasText()) {
        handleTextMessage(message);
      }
    }
  }

  private void handleTextMessage(Message message) {
    boolean isGroup = message.isGroupMessage() || message.isSuperGroupMessage();
    if (isCommand(isGroup, message.getText(), COMMAND_START)) {
      handleStartCommand(message);
    } else if (isCommand(isGroup, message.getText(), COMMAND_TOP)) {
      handleTopCommand(message, Utils.getCommandPayload(message.getText(), COMMAND_TOP));
    } else if (isCommand(isGroup, message.getText(), COMMAND_RANDOM)) {
      handleRandomCommand(message, Utils.getCommandPayload(message.getText(), COMMAND_RANDOM));
    } else if (isCommand(isGroup, message.getText(), COMMAND_SEARCH)) {
      handleSearchCommand(message, Utils.getCommandPayload(message.getText(), COMMAND_SEARCH));
    } else if (isCommand(isGroup, message.getText(), COMMAND_TAG)) {
      handleTagCommand(message, Utils.getCommandPayload(message.getText(), COMMAND_TAG));
    }
  }

  private void handleStartCommand(Message message) {
    SendMessage sendMessage = new SendMessage();
    sendMessage.setChatId(message.getChatId());
    sendMessage.setText(
        "*Список команд бота:*\n" +
            "/start - команды бота\n" +
            "/top - три лучших поста на данный момент\n" +
            "/random - случайный пост\n" +
            "/search _<текст>_ - случайный найденный пост\n" +
            "/tag _<имя>_ - случайный пост с тегом _<имя>_\n\n" +
            "Добавляйтесь в наш уютный [чатик](https://t.me/pikabu_chat)\n" +
            "Исходники можно найти на [Github](https://github.com/futurobot/PikabuRobot)");
    sendMessage.setParseMode(ParseMode.MARKDOWN);
    try {
      sendMessage(sendMessage);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private void handleTopCommand(Message message, String payload) {
    sendTyping(message.getChatId());

    int postsCount = 3;
    if (Utils.isInteger(payload)) {
      postsCount = Integer.parseInt(payload);
    }
    postsCount = Math.max(0, Math.min(postsCount, 5));
    try {
      Response
          response =
          okHttpClient.newCall(new Request.Builder().url("http://pikabu.ru/hot").build()).execute();
      if (!response.isSuccessful()) {
        return;
      }
      val stories = Jsoup.parse(response.body().string()).select("div.stories").first()
          .select("a.story__title-link").stream().limit(postsCount)
          .map(element -> new StoryDescription(element.text(), element.attr("href")))
          .collect(Collectors.toList());

      for (val story : stories) {
        val sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(
            String.format(Locale.ROOT, "%s\n%s", story.getTitle(), story.getLink()));
        sendMessage(sendMessage);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private void handleRandomCommand(Message message, String payload) {
    sendTyping(message.getChatId());

    String[] partitions = new String[] {"/hot", "/best", "/best/week", "/best/month", "/new"};

    String url =
        String.format(Locale.ROOT, "http://pikabu.ru/%s?page=%d", Utils.getRandomItem(partitions),
            random.nextInt(30));
    try {
      val story = parseSingleRandomPost(url);
      if (story == null) {
        sendMessage(new SendMessage().setChatId(message.getChatId())
            .setReplyToMessageId(message.getMessageId())
            .setText("Не получилось...")
            .setParseMode(ParseMode.HTML));
      } else {
        val sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(
            String.format(Locale.ROOT, "%s\n%s", story.getTitle(), story.getLink()));
        sendMessage(sendMessage);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleTagCommand(Message message, String payload) {
    if (Utils.textIsEmpty(payload)) {
      val sendMessage = new SendMessage();
      sendMessage.setChatId(message.getChatId());
      sendMessage.setReplyToMessageId(message.getMessageId());
      sendMessage.setText(
          "Не так. Надо вот так, например: /tag@" + configuration.getName() + " <b>сиськи</b>");
      sendMessage.setParseMode(ParseMode.HTML);
      try {
        sendMessage(sendMessage);
      } catch (TelegramApiException e) {
        e.printStackTrace();
      }
    } else {
      sendTyping(message.getChatId());
      String url =
          String.format(Locale.ROOT, "http://pikabu.ru/search.php?t=%s", payload);
      try {
        val story = parseSingleRandomPost(url);
        if (story == null) {
          sendMessage(new SendMessage().setChatId(message.getChatId())
              .setReplyToMessageId(message.getMessageId())
              .setText("Не нашел постов с тегом <b>" + payload + "</b>")
              .setParseMode(ParseMode.HTML));
        } else {
          val sendMessage = new SendMessage();
          sendMessage.setChatId(message.getChatId());
          sendMessage.setText(
              String.format(Locale.ROOT, "%s\n%s", story.getTitle(), story.getLink()));
          sendMessage(sendMessage);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void handleSearchCommand(Message message, String payload) {
    if (Utils.textIsEmpty(payload)) {
      val sendMessage = new SendMessage();
      sendMessage.setChatId(message.getChatId());
      sendMessage.setReplyToMessageId(message.getMessageId());
      sendMessage.setText(
          "Не так. Надо вот так, например: /search@" + configuration.getName() + " <b>котики</b>");
      sendMessage.setParseMode(ParseMode.HTML);
      try {
        sendMessage(sendMessage);
      } catch (TelegramApiException e) {
        e.printStackTrace();
      }
    } else {
      sendTyping(message.getChatId());
      String url =
          String.format(Locale.ROOT, "http://pikabu.ru/search.php?q=%s", payload);
      try {
        val story = parseSingleRandomPost(url);
        if (story == null) {
          sendMessage(new SendMessage().setChatId(message.getChatId())
              .setReplyToMessageId(message.getMessageId())
              .setText("Не нашел постов с запросом <b>" + payload + "</b>")
              .setParseMode(ParseMode.HTML));
        } else {
          val sendMessage = new SendMessage();
          sendMessage.setChatId(message.getChatId());
          sendMessage.setText(
              String.format(Locale.ROOT, "%s\n%s", story.getTitle(), story.getLink()));
          sendMessage(sendMessage);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private StoryDescription parseSingleRandomPost(String url) throws Exception {
    Response response = okHttpClient.newCall(new Request.Builder().url(url).build()).execute();
    if (!response.isSuccessful()) {
      throw new Exception("Network problem");
    }
    val elements = Jsoup.parse(response.body().string()).select("div.stories").first()
        .select("a.story__title-link");
    if (elements.size() == 0) {
      return null;
    }
    val element = Utils.getRandomItem(elements);
    StoryDescription story = new StoryDescription(element.text(), element.attr("href"));
    return story;
  }

  private boolean isCommand(boolean isGroup, String text, String command) {
    String cmd = isGroup ? String.format("%s@%s", command, getBotUsername()) : command;
    return text.toLowerCase().startsWith(cmd);
  }

  private void sendTyping(long chatId) {
    try {
      sendChatAction(new SendChatAction().setChatId(chatId).setAction(ActionType.TYPING));
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  @Override public String getBotUsername() {
    return configuration.getName();
  }

  @Override public String getBotToken() {
    return configuration.getToken();
  }
}
