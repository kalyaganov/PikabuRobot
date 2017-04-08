package ru.futurobot.telegram.pikabubot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

/**
 * Created by Alexey on 08.04.17.
 * Futurobot
 */
@Data
@AllArgsConstructor
@Value
public class StoryDescription {
  private String title;
  private String link;
}
