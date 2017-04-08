package ru.futurobot.telegram.pikabubot;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

/**
 * Created by Alexey on 08.04.17.
 * Futurobot
 */
public class Application {

  public static void main(String[] args) throws IOException, TelegramApiRequestException {
    Properties properties = readProperties(args);
    ApiContextInitializer.init();
    PikabuBot bot = new PikabuBot(new BotConfiguration(properties));
    TelegramBotsApi api = new TelegramBotsApi();
    api.registerBot(bot);
  }

  private static Properties readProperties(String[] args) throws IOException {
    Properties properties = new Properties();
    String propertyFile = Utils.readArgument(args, "properties");
    InputStream inputStream;
    if (propertyFile == null || propertyFile.equals("")) {
      //load from classpath
      String filename = "application.properties";
      inputStream = Application.class.getClassLoader().getResourceAsStream(filename);
      if (inputStream == null) {
        throw new RuntimeException("Unable to load property file from classpath " + filename);
      }
    } else {
      inputStream = new FileInputStream(propertyFile);
    }

    try {
      properties.load(inputStream);
    } finally {
      inputStream.close();
    }

    return properties;
  }
}
