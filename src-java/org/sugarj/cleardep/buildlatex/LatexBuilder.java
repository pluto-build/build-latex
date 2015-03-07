package org.sugarj.cleardep.buildlatex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    public final Path texPath;
    public final Path srcDir;
    public final Path targetDir;
    public final BuildRequest<?, ?, ?, ?>[] injectedRequirements;
    public Input(Path texPath, Path srcDir, Path targetDir, BuildRequest<?, ?, ?, ?>[] injectedRequirements) {
      this.texPath = texPath;
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
    return "Build PDF from " + FileCommands.dropDirectory(input.texPath);
  }

  @Override
  protected Path persistentPath() {
    if (input.targetDir != null)
      return new RelativePath(input.targetDir, "latex.dep");
    return new AbsolutePath("./latex.dep");
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
    
    RelativePath texPath = FileCommands.getRelativePath(srcDir, input.texPath);
    if (texPath == null)
      throw new IllegalArgumentException("Builder requires tex file to be within the source directory.");
    
    RelativePath auxPath = FileCommands.replaceExtension(new RelativePath(targetDir, texPath.getRelativePath()), "aux");
    requires(input.texPath);
    requires(auxPath, ContentStamper.instance);

    if (FileCommands.exists(auxPath))
      require(BibtexBuilder.factory, new BibtexBuilder.Input(input.texPath, auxPath, srcDir, targetDir, null));

    RelativePath bbl = FileCommands.replaceExtension(auxPath, "bbl");
    requires(bbl);
    
    FileCommands.createDir(targetDir);
    String[][] msgs = new CommandExecution(true).execute(srcDir, 
        "pdflatex", 
        "-interaction=batchmode", 
        "-output-directory=" + targetDir.getAbsolutePath(),
        "-kpathsea-debug=4",
        FileCommands.dropDirectory(input.texPath));

    List<Path> requiredFiles = extractRequiredFiles(msgs[1], srcDir);
    for (Path p : requiredFiles)
      requires(p);
    
    
    RelativePath pdfPath = auxPath.replaceExtension("pdf");
    generates(auxPath);
    generates(pdfPath);
    
    return None.val;
  }

  private List<Path> extractRequiredFiles(String[] lines, Path baseDir) {
    List<Path> paths = new ArrayList<>();
    for (String line : lines)
      if (line.startsWith("kdebug:fopen(")) {
        int start = "kdebug:fopen(".length();
        int end = line.indexOf(',');
        String file = line.substring(start, end);
        if (AbsolutePath.acceptable(file) && !file.startsWith("."))
          paths.add(new AbsolutePath(file));
        else
          paths.add(new RelativePath(baseDir, file));
      }
    return paths;
  }
  
}
