package org.sugarj.cleardep.buildlatex;

import java.io.Serializable;
import java.util.Set;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.build.BuildRequirement;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class BibtexBuilder extends Builder<BibtexBuilder.Input, CompilationUnit> {

  public final static BuilderFactory<Input, CompilationUnit, BibtexBuilder> factory = new BuilderFactory<BibtexBuilder.Input, CompilationUnit, BibtexBuilder>() {
    private static final long serialVersionUID = 2390540998732457948L;

    @Override
    public BibtexBuilder makeBuilder(Input input, BuildManager manager) {
      return new BibtexBuilder(input, factory, manager);
    }
  };
  
  public static class Input implements Serializable {
    private static final long serialVersionUID = -6065839202426934802L;
    public final RelativePath auxPath;
    public final Path srcDir;
    public final Path targetDir;
    public final BuildRequirement<?, ?, ?, ?>[] injectedRequirements;
    public Input(RelativePath auxPath, Path srcDir, Path targetDir, BuildRequirement<?, ?, ?, ?>[] injectedRequirements) {
      this.auxPath = auxPath;
      this.srcDir = srcDir;
      this.targetDir = targetDir;
      this.injectedRequirements = injectedRequirements;
    }
  }

  private BibtexBuilder(Input input, BuilderFactory<Input, CompilationUnit, ? extends Builder<Input, CompilationUnit>> sourceFactory, BuildManager manager) {
    super(input, sourceFactory, manager);
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
  protected Class<CompilationUnit> resultClass() {
    return CompilationUnit.class;
  }

  @Override
  protected Stamper defaultStamper() {
    return LastModifiedStamper.instance;
  }

  @Override
  protected void build(CompilationUnit result) throws Throwable {
    require(input.injectedRequirements);
    
    BibtexAuxRequirementsStamper.BibtexAuxRequirementsStamp bibtexSourceStamp = BibtexAuxRequirementsStamper.instance.stampOf(input.auxPath);
    result.addSourceArtifact(input.auxPath, bibtexSourceStamp);

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
  
        result.addExternalFileDependency(srcbib);
        FileCommands.copyFile(srcbib, buildbib);
        result.addGeneratedFile(buildbib);
      }

    new CommandExecution(false).execute(targetDir, "bibtex", FileCommands.fileName(input.auxPath));

    RelativePath bbl = FileCommands.replaceExtension(input.auxPath, "bbl");
    result.addGeneratedFile(bbl);
  }
  
}
