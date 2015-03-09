package org.sugarj.cleardep.buildlatex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.build.CycleSupport;
import org.sugarj.cleardep.output.None;
import org.sugarj.cleardep.stamp.ContentStamper;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;

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
    public final AbsolutePath binaryLocation;
    public Input(Path texPath, Path srcDir, Path targetDir, AbsolutePath binaryLocation) {
      this.texPath = texPath;
      this.srcDir = srcDir;
      this.targetDir = targetDir;
      this.binaryLocation = binaryLocation;
    }
  }

  private LatexBuilder(Input input) {
    super(input);
  }
  
  @Override
  protected CycleSupport getCycleSupport() {
    return new LatexBibtexCycleSupport();
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
    Path srcDir = input.srcDir != null ? input.srcDir : new AbsolutePath(".");
    Path targetDir = input.targetDir != null ? input.targetDir : new AbsolutePath(".");
    
    RelativePath texPath = FileCommands.getRelativePath(srcDir, input.texPath);
    if (texPath == null)
      throw new IllegalArgumentException("Builder requires tex file to be within the source directory.");
    
    require(LatexBuilder.factory, input);
    
    RelativePath auxPath = FileCommands.replaceExtension(new RelativePath(targetDir, texPath.getRelativePath()), "aux");
    if (FileCommands.exists(auxPath)) {
      require(BibtexBuilder.factory, new BibtexBuilder.Input(input.texPath, auxPath, srcDir, targetDir, input.binaryLocation));
    }
    

    FileCommands.createDir(targetDir);
    String program = "pdflatex";
    if (input.binaryLocation != null) {
      program = input.binaryLocation.getAbsolutePath() + "/" + program;
    }
    String[][] msgs = new CommandExecution(true).execute(srcDir, 
        program, 
        "-interaction=batchmode", 
        "-output-directory=" + targetDir.getAbsolutePath(),
        "-kpathsea-debug=4",
        FileCommands.dropDirectory(input.texPath));

    Pair<List<Path>, List<Path>> readWriteFiles = extractAccessedFiles(msgs[1]);
    for (Path p : readWriteFiles.b)
      if (!FileCommands.getExtension(p).equals("log"))
        generates(p);
    for (Path p : readWriteFiles.a)
      requires(p);
    
    return None.val;
  }

  private Pair<List<Path>, List<Path>> extractAccessedFiles(String[] lines) {
    Path srcDir = input.srcDir != null ? input.srcDir : new AbsolutePath(".");
    Path targetDir = input.targetDir != null ? input.targetDir : new AbsolutePath(".");
    
    List<Path> readPathList = new ArrayList<>();
    Set<Path> readPaths = new HashSet<>();
    List<Path> writePathList = new ArrayList<>();
    Set<Path> writePaths = new HashSet<>();
    for (String line : lines)
      if (line.startsWith("kdebug:fopen(")) {
        int start = "kdebug:fopen(".length();
        int end = line.indexOf(',');
        String file = line.substring(start, end);
        String mode = line.substring(end + 2, end + 3);
        RelativePath rel;
        if (AbsolutePath.acceptable(file) && !file.startsWith(".")) {
          Path p = new AbsolutePath(file);
          rel = FileCommands.getRelativePath(srcDir, p);
          if (rel == null)
            rel = FileCommands.getRelativePath(targetDir, p);
        }
        else {
          if (file.startsWith("./"))
            file = file.substring(2);
          rel = new RelativePath(srcDir, file);
        }
        
        if (FileCommands.exists(rel) && "r".equals(mode) && readPaths.add(rel))
          readPathList.add(rel);
        else if ("w".equals(mode) && writePaths.add(rel))
          writePathList.add(rel);
      }
    return Pair.create(readPathList, writePathList);
  }
  
}
