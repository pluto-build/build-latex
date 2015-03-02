package org.sugarj.cleardep.buildlatex;

import java.io.Serializable;

import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.output.None;
import org.sugarj.cleardep.stamp.ContentStamper;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class LatexBuilder extends Builder<LatexBuilder.Input, None> {

  public final static BuilderFactory<Input, None, LatexBuilder> factory = new BuilderFactory<Input, None, LatexBuilder>() {
    private static final long serialVersionUID = 357011347823016858L;

    @Override
    public LatexBuilder makeBuilder(Input input) { return new LatexBuilder(input); }
  };

  public static class Input implements Serializable {
    private static final long serialVersionUID = -6065839202426934802L;
    public final RelativePath texPath;
    public final Path srcDir;
    public final Path targetDir;
    public final BuildRequest<?, ?, ?, ?>[] injectedRequirements;
    public Input(RelativePath auxPath, Path srcDir, Path targetDir, BuildRequest<?, ?, ?, ?>[] injectedRequirements) {
      this.texPath = auxPath;
      this.srcDir = srcDir;
      this.targetDir = targetDir;
      this.injectedRequirements = injectedRequirements;
    }
  }

  private LatexBuilder(Input input) {
    super(input);
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
  protected Stamper defaultStamper() {
    return LastModifiedStamper.instance;
  }

  @Override
  protected None build() throws Throwable {
    require(input.injectedRequirements);
    
    Path srcDir = input.srcDir != null ? input.srcDir : new AbsolutePath(".");
    Path targetDir = input.targetDir != null ? input.targetDir : new AbsolutePath(".");
    
    RelativePath auxPath = FileCommands.replaceExtension(new RelativePath(targetDir, input.texPath.getRelativePath()), "aux");
    requires(input.texPath);
    requires(auxPath, ContentStamper.instance.stampOf(auxPath));

    if (FileCommands.exists(auxPath))
      require(BibtexBuilder.factory, new BibtexBuilder.Input(auxPath, srcDir, targetDir, null));

    RelativePath bbl = FileCommands.replaceExtension(auxPath, "bbl");
    requires(bbl);
    
    FileCommands.createDir(targetDir);
    new CommandExecution(false).execute(srcDir, "pdflatex", "-interaction=batchmode", "-output-directory=../" + FileCommands.fileName(targetDir) + "/", FileCommands.dropDirectory(input.texPath));

    RelativePath pdfPath = FileCommands.replaceExtension(new RelativePath(targetDir, input.texPath.getRelativePath()), "pdf");
    generates(auxPath);
    generates(pdfPath);
    
    return None.val;
  }
  
}
