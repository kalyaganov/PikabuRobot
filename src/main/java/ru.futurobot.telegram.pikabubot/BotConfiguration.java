package ru.futurobot.telegram.pikabubot;

import java.util.Properties;

/**
 * Created by Alexey on 17.12.16.
 * Futurobot
 */
public class BotConfiguration implements Configuration {
  private String token;
  private String name;

  public BotConfiguration() {
  }

  public BotConfiguration(Properties properties) {
    this.token = properties.getProperty("bot.token");
    this.name = properties.getProperty("bot.name");
  }

  public String getName() {
    return name;
  }

  public String getToken() {
    return token;
  }
}
