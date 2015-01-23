import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.sugarj.common.FileCommands;
import org.sugarj.common.cleardep.Stamp;
import org.sugarj.common.cleardep.Stamper;
import org.sugarj.common.path.Path;


public class BibtexSourceStamper implements Stamper {
  
  @Override
  public Stamp stampOf(Path p) {
    String content;
    try {
      content = FileCommands.readFileAsString(p);
    } catch (IOException e) {
      e.printStackTrace();
      return new BibtexSourceStamp(null);
    }
    
    Set<String> citations = new HashSet<>();
    for (String line : content.split("\n"))
      if (line.startsWith("\\citation{")) {
        int start = "\\citation{".length();
        int end = line.indexOf('}');
        String cits = line.substring(start, end);
        for (String citation : cits.split(","))
          citations.add(citation);
      }
    
    return new BibtexSourceStamp(citations);
  }

  
  public static class BibtexSourceStamp implements Stamp {

    private static final long serialVersionUID = -1055541424119046676L;

    private final Set<String> citations;
    
    public BibtexSourceStamp(Set<String> citations) {
      this.citations = citations;
    }

    @Override
    public boolean equals(Stamp o) {
      if (!(o instanceof BibtexSourceStamp))
        return false;
      
      Set<String> ocitations = ((BibtexSourceStamp) o).citations;
      return citations == null && ocitations == null 
          || citations.equals(ocitations);
    }


  }
}
