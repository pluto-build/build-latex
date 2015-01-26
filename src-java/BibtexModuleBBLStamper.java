import java.util.HashSet;
import java.util.Set;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.stamp.CollectionStamper;
import org.sugarj.cleardep.stamp.ContentHashStamper;
import org.sugarj.cleardep.stamp.ModuleStamp;
import org.sugarj.cleardep.stamp.ModuleStamper;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;


public class BibtexModuleBBLStamper implements ModuleStamper {
  
  public static BibtexModuleBBLStamper minstance = new BibtexModuleBBLStamper();
  private BibtexModuleBBLStamper() { }
  
  private CollectionStamper fileStamper = new CollectionStamper(ContentHashStamper.instance);
  
  @Override
  public ModuleStamp stampOf(CompilationUnit mod) {
    Set<Path> generatedBBLs = new HashSet<>();
    for (Path p : mod.getGeneratedFiles())
      if ("bbl".equals(FileCommands.getExtension(p)))
        generatedBBLs.add(p);
    
    return new BibtexModuleBBLStamp(fileStamper.stampOf(generatedBBLs));
  }
  
  
  public static class BibtexModuleBBLStamp implements ModuleStamp {

    private static final long serialVersionUID = -1055541424119046676L;

    private final Stamp fileStamp;
    
    public BibtexModuleBBLStamp(Stamp fileStamp) {
      this.fileStamp = fileStamp;
    }

    @Override
    public boolean equals(ModuleStamp o) {
      return o instanceof BibtexModuleBBLStamp && ((BibtexModuleBBLStamp) o).fileStamp.equals(fileStamp);
    }

    @Override
    public ModuleStamper getModuleStamper() {
      return BibtexModuleBBLStamper.minstance;
    }
    
    @Override
    public String toString() {
      return "BibtexModuleBBL(" + fileStamp + ")";
    }
  }
}
