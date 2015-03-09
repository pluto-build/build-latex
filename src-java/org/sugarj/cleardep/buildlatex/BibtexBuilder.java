package org.sugarj.cleardep.buildlatex;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.build.CycleSupport;
import org.sugarj.cleardep.output.None;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.cleardep.stamp.ValueStamp;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;

public class BibtexBuilder extends Builder<BibtexBuilder.Input, None> {

  public final static BuilderFactory<Input, None, BibtexBuilder> factory = new BuilderFactory<BibtexBuilder.Input, None, BibtexBuilder>() {
    private static final long serialVersionUID = 2390540998732457948L;

    @Override
    public BibtexBuilder makeBuilder(Input input) { return new BibtexBuilder(input); }
  };
  
  public static class Input implements Serializable {
    private static final long serialVersionUID = -6065839202426934802L;
    public final Path texPath;
    public final Path auxPath;
    public final Path srcDir;
    public final Path targetDir;
    public final AbsolutePath binaryLocation;
    
    public Input(Path texPath, Path auxPath, Path srcDir, Path targetDir,AbsolutePath binaryPath) {
      this.texPath = texPath;
      this.auxPath = auxPath;
      this.srcDir = srcDir;
      this.targetDir = targetDir;
      this.binaryLocation = binaryPath;
    }
  }

  private BibtexBuilder(Input input) {
    super(input);
  }
  
  @Override
  protected CycleSupport getCycleSupport() {
    return new LatexBibtexCycleSupport();
  }

  @Override
  protected String taskDescription() {
    return "Build bibliography for " + FileCommands.dropDirectory(input.auxPath);
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
    
    require(LatexBuilder.factory, new LatexBuilder.Input(input.texPath, input.srcDir, input.targetDir, input.binaryLocation));
    
    ValueStamp<Pair<Map<String,String>, Set<String>>> bibtexSourceStamp = BibtexAuxRequirementsStamper.instance.stampOf(input.auxPath);
    requires(input.auxPath, bibtexSourceStamp);

    if (!FileCommands.exists(input.auxPath))
      throw new IllegalArgumentException("No bibliography built: Could not find " + input.auxPath);
    
    Set<String> bibnames = bibtexSourceStamp.val.a.keySet();
    
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

    String program = "bibtex";
    if (input.binaryLocation != null) {
      program = input.binaryLocation.getAbsolutePath() + "/" + program;
    }
    new CommandExecution(false).execute(targetDir, program, FileCommands.fileName(input.auxPath));

    Path bbl = input.auxPath.replaceExtension("bbl");
    generates(bbl);
    
    return None.val;
  }
  
}
