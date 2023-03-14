package org.bytedeco.javacpp.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class Templates {

  static final private Pattern templatePattern = Pattern.compile("<[^<>=]*>");

  // Remove template arguments from s, taking care of nested templates, default arguments (xxx<>), operator <=>, ->, etc...
  static String strip(String s) {
      Matcher m;
      do {
          m = templatePattern.matcher(s);
          s = m.replaceFirst("");
      } while (!m.hitEnd());
      return s;
  }

  static boolean hasNone(String s) {
      return strip(s).length() == s.length();
  }

  // Split s at ::, but taking care of qualified template arguments
  static List<String> nsSplit(String s) {
      String sTemplatesMasked = s;
      for (;;) {
          Matcher m = templatePattern.matcher(sTemplatesMasked);
          if (m.find()) {
              char[] c = new char[m.end() - m.start()];
              Arrays.fill(c, '.');
              sTemplatesMasked = sTemplatesMasked.substring(0, m.start()) + new String(c) + sTemplatesMasked.substring(m.end());
          } else
              break;
      }
      ArrayList<String> comps = new ArrayList<>();
      int start = 0;
      for (;;) {
          int i = sTemplatesMasked.indexOf("::", start);
          if (i >= 0) {
              comps.add(s.substring(start, i));
              start = i + 2;
          } else
              break;
      }
      comps.add(s.substring(start));
      return comps;
  }
}
