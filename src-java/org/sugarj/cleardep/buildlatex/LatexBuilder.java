package org.sugarj.cleardep.buildlatex;

import java.io.Serializable;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.build.BuildRequirement;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.stamp.ContentStamper;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class LatexBuilder extends Builder<LatexBuilder.Input, CompilationUnit> {

  public static class Input implements Serializable {
    private static final long serialVersionUID = -6065839202426934802L;
    public final RelativePath texPath;
    public final Path srcDir;
    public final Path targetDir;
    public final BuildRequirement<?, ?, ?, ?>[] injectedRequirements;
    public Input(RelativePath auxPath, Path srcDir, Path targetDir, BuildRequirement<?, ?, ?, ?>[] injectedRequirements) {
      this.texPath = auxPath;
      this.srcDir = srcDir;
      this.targetDir = targetDir;
      this.injectedRequirements = injectedRequirements;
    }
  }

  private LatexBuilder(Input input, BuilderFactory<Input, CompilationUnit, ? extends Builder<Input, CompilationUnit>> sourceFactory, BuildManager manager) {
    super(input, sourceFactory, manager);
  }

  @Override
  protected String taskDescription() {
    return "Build bibliography for " + FileCommands.fileName(input.texPath);
  }

  @Override
  protected Path persistentPath() {
    return input.texPath.replaceExtension("bibdep");
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
    
    Path srcDir = input.srcDir != null ? input.srcDir : new AbsolutePath(".");
    Path targetDir = input.targetDir != null ? input.targetDir : new AbsolutePath(".");
    
    RelativePath auxPath = FileCommands.replaceExtension(new RelativePath(targetDir, input.texPath.getRelativePath()), "aux");
    result.addSourceArtifact(input.texPath);
    result.addSourceArtifact(auxPath, ContentStamper.instance.stampOf(auxPath));

    if (FileCommands.exists(auxPath))
      require(BibtexBuilder.factory, new BibtexBuilder.Input(auxPath, srcDir, targetDir, null));
    
    FileCommands.createDir(targetDir);
    new CommandExecution(false).execute(srcDir, "pdflatex", "-interaction=batchmode", "-output-directory=../" + FileCommands.fileName(targetDir) + "/", FileCommands.dropDirectory(input.texPath));

    RelativePath pdfPath = FileCommands.replaceExtension(new RelativePath(targetDir, input.texPath.getRelativePath()), "pdf");
    result.addGeneratedFile(auxPath);
    result.addGeneratedFile(pdfPath);
  }
  
}
