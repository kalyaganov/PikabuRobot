package ru.futurobot.telegram.pikabubot;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.jsoup.select.Elements;

/**
 * Created by Alexey on 27.11.16.
 * Futurobot
 */
public class Utils {
  private static Random randomizer;

  static {
    randomizer = new Random(System.currentTimeMillis());
  }

  private Utils() {
  }

  public static <T> T getRandomItem(T[] array) {
    if (array.length == 1) {
      return array[0];
    } else {
      return array[randomizer.nextInt(array.length - 1)];
    }
  }

  public static String readArgument(String[] args, String propertyName) {
    Iterator<String> argsIterator = Arrays.asList(args).iterator();
    while (argsIterator.hasNext()) {
      String arg = argsIterator.next();
      if (arg.startsWith("--" + propertyName + "=")) {
        return arg.substring(("--" + propertyName + "=").length());
      } else if (arg.equals("-" + propertyName)) {
        if (argsIterator.hasNext()) {
          return argsIterator.next();
        }
      }
    }
    return "";
  }

  public static boolean textIsEmpty(String text) {
    return text == null || text.equals("");
  }

  public static boolean isInteger(String string) {
    try {
      Integer.parseInt(string);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static String getCommandPayload(String text, String command) {
    String[] args = text.split(" ");
    if(args.length > 1){
      return text.substring(args[0].length() + 1);
    }
    return "";
  }

  public static <T> T getRandomItem(List<T> list) {
    if (list.size()== 1) {
      return list.get(0);
    } else {
      return list.get(randomizer.nextInt(list.size() - 1));
    }
  }
}
