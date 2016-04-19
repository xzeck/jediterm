package com.jediterm.terminal;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jediterm.terminal.model.CharBuffer;
import com.jediterm.terminal.util.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of substring search based on Rabin-Karp algorithm
 *
 * @author traff
 */
public class SubstringFinder {
  private final String myPattern;
  private final int myPatternHash;
  private int myCurrentHash;
  private int myCurrentLength;
  private final ArrayList<TextToken> myTokens = Lists.newArrayList();
  private int myFirstIndex;
  private int myPower = 0;

  private final FindResult myResult = new FindResult();
  private boolean myIgnoreCase;


  public SubstringFinder(String pattern, boolean ignoreCase) {
    myIgnoreCase = ignoreCase;
    myPattern = ignoreCase ? pattern.toLowerCase() : pattern;
    myPatternHash = myPattern.hashCode();
  }


  public void nextChar(int x, int y, CharBuffer characters, int index) {
    if (myTokens.size() == 0 || myTokens.get(myTokens.size() - 1).buf != characters) {
      myTokens.add(new TextToken(x, y, characters));
    }

    if (myCurrentLength == myPattern.length()) {
      myCurrentHash -= hashCodeForChar(myTokens.get(0).buf.charAt(myFirstIndex));
      if (myFirstIndex + 1 == myTokens.get(0).buf.length()) {
        myFirstIndex = 0;
        myTokens.remove(0);
      } else {
        myFirstIndex += 1;
      }
    } else {
      myCurrentLength += 1;
      if (myPower == 0) {
        myPower = 1;
      } else {
        myPower *= 31;
      }
    }

    myCurrentHash = 31 * myCurrentHash + charHash(characters.charAt(index));

    if (myCurrentLength == myPattern.length() &&
            myCurrentHash == myPatternHash) {
      FindResult.FindItem item = new FindResult.FindItem(myTokens, myFirstIndex, index);
      if (myPattern.equals(myIgnoreCase ? item.toString().toLowerCase() : item.toString())) {
        myResult.patternMatched(myTokens, myFirstIndex, index);
        myCurrentHash = 0;
        myCurrentLength = 0;
        myPower = 0;
        myTokens.clear();
        if (index + 1 < characters.length()) {
          myFirstIndex = index + 1;
          myTokens.add(new TextToken(x, y, characters));
        } else {
          myFirstIndex = 0;
        }
      }
    }
  }

  private int charHash(char c) {
    return myIgnoreCase ? Character.toLowerCase(c) : c;
  }


  private int hashCodeForChar(char charAt) {
    return myPower * charHash(charAt);
  }

  public FindResult getResult() {
    return myResult;
  }

  public static class FindResult {
    private final List<FindItem> items = Lists.newArrayList();
    private final Map<CharBuffer, List<Pair<Integer, Integer>>> ranges = Maps.newHashMap();
    private int currentFindItem = 0;

    public List<Pair<Integer, Integer>> getRanges(CharBuffer characters) {
      return ranges.get(characters);
    }

    public static class FindItem {
      final ArrayList<TextToken> tokens;
      final int firstIndex;
      final int lastIndex;

      private FindItem(ArrayList<TextToken> tokens, int firstIndex, int lastIndex) {
        this.tokens = Lists.newArrayList(tokens);
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
      }

      public String toString() {
        StringBuilder b = new StringBuilder();

        if (tokens.size() > 1) {
          Pair<Integer, Integer> range = Pair.create(firstIndex, tokens.get(0).buf.length());
          b.append(tokens.get(0).buf.subBuffer(range));
        } else {
          Pair<Integer, Integer> range = Pair.create(firstIndex, lastIndex + 1);
          b.append(tokens.get(0).buf.subBuffer(range));
        }

        for (int i = 1; i < tokens.size() - 1; i++) {
          b.append(tokens.get(i));
        }

        if (tokens.size() > 1) {
          Pair<Integer, Integer> range = Pair.create(0, lastIndex + 1);
          b.append(tokens.get(tokens.size() - 1).buf.subBuffer(range));
        }

        return b.toString();
      }

      public Point getStart() {
        return new Point(tokens.get(0).x + firstIndex, tokens.get(0).y);
      }

      public Point getEnd() {
        return new Point(tokens.get(tokens.size() - 1).x + lastIndex, tokens.get(tokens.size() - 1).y);
      }
    }

    public void patternMatched(ArrayList<TextToken> tokens, int firstIndex, int lastIndex) {
      if (tokens.size() > 1) {
        Pair<Integer, Integer> range = Pair.create(firstIndex, tokens.get(0).buf.length());
        put(tokens.get(0).buf, range);
      } else {
        Pair<Integer, Integer> range = Pair.create(firstIndex, lastIndex + 1);
        put(tokens.get(0).buf, range);
      }

      for (int i = 1; i < tokens.size() - 1; i++) {
        put(tokens.get(i).buf, Pair.create(0, tokens.get(i).buf.length()));
      }

      if (tokens.size() > 1) {
        Pair<Integer, Integer> range = Pair.create(0, lastIndex + 1);
        put(tokens.get(tokens.size() - 1).buf, range);
      }

      items.add(new FindItem(tokens, firstIndex, lastIndex));

    }

    private void put(CharBuffer characters, Pair<Integer, Integer> range) {
      if (ranges.containsKey(characters)) {
        ranges.get(characters).add(range);
      } else {
        ranges.put(characters, Lists.newArrayList(range));
      }
    }

    public List<FindItem> getItems() {
      return items;
    }

    public FindItem nextFindItem() {
      if (currentFindItem == 0) {
        currentFindItem = items.size() - 1;
      } else {
        currentFindItem--;
      }

      if (currentFindItem >= 0 && currentFindItem <= items.size()) {
        return items.get(currentFindItem);
      } else {
        return null;
      }
    }
  }


  private static class TextToken {
    final CharBuffer buf;
    final int x;
    final int y;

    private TextToken(int x, int y, CharBuffer buf) {
      this.x = x;
      this.y = y;
      this.buf = buf;
    }
  }
}
