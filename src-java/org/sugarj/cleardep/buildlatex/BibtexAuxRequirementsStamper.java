package org.sugarj.cleardep.buildlatex;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.cleardep.stamp.ValueStamp;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;
import org.sugarj.common.util.Pair;


public class BibtexAuxRequirementsStamper implements Stamper {
  
  private static final long serialVersionUID = 5441596559010960001L;

  public static BibtexAuxRequirementsStamper instance = new BibtexAuxRequirementsStamper();
  private BibtexAuxRequirementsStamper() { }
  
  @Override
  public ValueStamp<Pair<Map<String, String>, Set<String>>> stampOf(Path p) {
    String content;
    try {
      content = FileCommands.readFileAsString(p);
    } catch (FileNotFoundException e) {
      return new ValueStamp<>(this, null);
    } catch (IOException e) {
      e.printStackTrace();
      return new ValueStamp<>(this, null);
    }
    
    String currentStyle = null;
    Map<String, String> bibdata = new HashMap<>();
    Set<String> citations = new HashSet<>();
    
    for (String line : content.split("\n"))
      if (line.startsWith("\\bibstyle{")) {
        int start = "\\bibstyle{".length();
        int end = line.indexOf('}');
        currentStyle = line.substring(start, end);
      }
      else if (line.startsWith("\\bibdata{")) {
        int start = "\\bibdata{".length();
        int end = line.indexOf('}');
        bibdata.put(line.substring(start, end), currentStyle);
      }
      else if (line.startsWith("\\citation{")) {
        int start = "\\citation{".length();
        int end = line.indexOf('}');
        String cits = line.substring(start, end);
        for (String citation : cits.split(","))
          citations.add(citation);
      }
    
    return new ValueStamp<>(this, Pair.create(bibdata, citations));
  }
}
