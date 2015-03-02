package org.sugarj.cleardep.buildlatex;

import java.io.Serializable;
import java.util.Set;

import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.output.None;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class BibtexBuilder extends Builder<BibtexBuilder.Input, None> {

  public final static BuilderFactory<Input, None, BibtexBuilder> factory = new BuilderFactory<BibtexBuilder.Input, None, BibtexBuilder>() {
    private static final long serialVersionUID = 2390540998732457948L;

    @Override
    public BibtexBuilder makeBuilder(Input input) { return new BibtexBuilder(input); }
  };
  
  public static class Input implements Serializable {
    private static final long serialVersionUID = -6065839202426934802L;
    public final RelativePath auxPath;
    public final Path srcDir;
    public final Path targetDir;
    public final BuildRequest<?, ?, ?, ?>[] injectedRequirements;
    public Input(RelativePath auxPath, Path srcDir, Path targetDir, BuildRequest<?, ?, ?, ?>[] injectedRequirements) {
      this.auxPath = auxPath;
      this.srcDir = srcDir;
      this.targetDir = targetDir;
      this.injectedRequirements = injectedRequirements;
    }
  }

  private BibtexBuilder(Input input) {
    super(input);
  }

  @Override
  protected String taskDescription() {
    return "Build bibliography for " + FileCommands.fileName(input.auxPath);
  }

  @Override
  protected Path persistentPath() {
    return input.auxPath.replaceExtension("bibdep");
  }

  @Override
  protected Stamper defaultStamper() {
    return LastModifiedStamper.instance;
  }

  @Override
  protected None build() throws Throwable {
    require(input.injectedRequirements);
    
    BibtexAuxRequirementsStamper.BibtexAuxRequirementsStamp bibtexSourceStamp = BibtexAuxRequirementsStamper.instance.stampOf(input.auxPath);
    requires(input.auxPath, BibtexAuxRequirementsStamper.instance);

    if (!FileCommands.exists(input.auxPath))
      throw new IllegalArgumentException("No bibliography built: Could not find " + input.auxPath);
    
    Set<String> bibnames = bibtexSourceStamp.bibdatas.keySet();
    
    Path srcDir = input.srcDir != null ? input.srcDir : new AbsolutePath(".");
    Path targetDir = input.targetDir != null ? input.targetDir : new AbsolutePath(".");
    
    FileCommands.createDir(targetDir);
    if (srcDir != null && targetDir != null && !srcDir.equals(targetDir))
      for (String bibname : bibnames) {
        RelativePath srcbib = new RelativePath(srcDir, bibname + ".bib");
        RelativePath buildbib = new RelativePath(targetDir, bibname + ".bib");
  
        requires(srcbib);
        FileCommands.copyFile(srcbib, buildbib);
        generates(buildbib);
      }

    new CommandExecution(false).execute(targetDir, "bibtex", FileCommands.fileName(input.auxPath));

    RelativePath bbl = FileCommands.replaceExtension(input.auxPath, "bbl");
    generates(bbl);
    
    return None.val;
  }
  
}
