import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;


public class BibtexAuxRequirementsStamper implements Stamper {
  
  private static final long serialVersionUID = 5441596559010960001L;

  public static BibtexAuxRequirementsStamper instance = new BibtexAuxRequirementsStamper();
  private BibtexAuxRequirementsStamper() { }
  
  @Override
  public BibtexAuxRequirementsStamp stampOf(Path p) {
    String content;
    try {
      content = FileCommands.readFileAsString(p);
    } catch (FileNotFoundException e) {
      return new BibtexAuxRequirementsStamp(null, null);
    } catch (IOException e) {
      e.printStackTrace();
      return new BibtexAuxRequirementsStamp(null, null);
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
      
    
    return new BibtexAuxRequirementsStamp(bibdata, citations);
  }
  
  
  public static class BibtexAuxRequirementsStamp implements Stamp {

    private static final long serialVersionUID = -1055541424119046676L;

    /**
     * Maps name of bibliography to citation style.
     */
    public final Map<String, String> bibdatas;
    /**
     * Set of cited entries.
     */
    public final Set<String> citations;
    
    public BibtexAuxRequirementsStamp(Map<String, String> bibdatas, Set<String> citations) {
      this.bibdatas = bibdatas;
      this.citations = citations;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof BibtexAuxRequirementsStamp) {
        Map<String, String> obibdatas = ((BibtexAuxRequirementsStamp) o).bibdatas;
        Set<String> ocitations = ((BibtexAuxRequirementsStamp) o).citations;
        boolean eqBibdatas = bibdatas == null && obibdatas == null || bibdatas != null && bibdatas.equals(obibdatas);
        boolean eqCitations = citations == null && ocitations == null || citations != null && citations.equals(ocitations);
        return eqBibdatas && eqCitations;
      }
      return false;
    }

    @Override
    public Stamper getStamper() {
      return BibtexAuxRequirementsStamper.instance;
    }
    
    @Override
    public String toString() {
      return "BibtexSource(" + bibdatas + ", " + citations + ")";
    }
  }
}
