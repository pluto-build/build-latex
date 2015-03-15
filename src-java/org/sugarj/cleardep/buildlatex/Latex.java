package org.sugarj.cleardep.buildlatex;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.build.CycleSupport;
import org.sugarj.cleardep.stamp.FileHashStamper;
import org.sugarj.common.Exec;
import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;

public class Latex extends Builder<Latex.Input, Path> {

  public final static BuilderFactory<Input, Path, Latex> factory = new BuilderFactory<Input, Path, Latex>() {
    private static final long serialVersionUID = 357011347823016858L;

    @Override
    public Latex makeBuilder(Input input) { return new Latex(input); }
  };

  public static class Input implements Serializable {
    private static final long serialVersionUID = -6065839202426934802L;
    public final String docName;
    public final Path srcDir;
    public final Path targetDir;
    public final AbsolutePath binaryLocation;
    public Input(String docName, Path srcDir, Path targetDir, AbsolutePath binaryLocation) {
      this.docName = docName;
      this.srcDir = srcDir;
      this.targetDir = targetDir;
      this.binaryLocation = binaryLocation;
    }
  }

  private Latex(Input input) {
    super(input);
  }
  
  @Override
  protected CycleSupport getCycleSupport() {
    return new LatexBibtexCycleSupport();
  }

  @Override
  protected String taskDescription() {
    return "Build PDF for " + input.docName;
  }

  @Override
  protected Path persistentPath() {
    if (input.targetDir != null)
      return new RelativePath(input.targetDir, "latex.dep");
    return new AbsolutePath("./latex.dep");
  }

  @Override
  protected Path build() throws IOException {
    Path srcDir = input.srcDir != null ? input.srcDir : new AbsolutePath(".");
    Path targetDir = input.targetDir != null ? input.targetDir : new AbsolutePath(".");
    String program = "pdflatex";
    if (input.binaryLocation != null)
      program = input.binaryLocation.getAbsolutePath() + "/" + program;

    requireBuild(Bibtex.factory, input);
    // because of self-reference via *.aux file
    requireBuild(Latex.factory, input);

    RelativePath tex = new RelativePath(srcDir, input.docName + ".tex");
    RelativePath aux = new RelativePath(targetDir, input.docName + ".aux");
    RelativePath bbl = new RelativePath(targetDir, input.docName + ".bbl");
    require(tex, FileHashStamper.instance);
    require(aux, FileHashStamper.instance);
    require(bbl, FileHashStamper.instance);

    FileCommands.createDir(targetDir);
    ExecutionResult msgs = Exec.run(srcDir, 
        program, 
        "-interaction=batchmode", 
        "-output-directory=" + targetDir.getAbsolutePath(),
        "-kpathsea-debug=4",
        input.docName + ".tex");

    Pair<List<Path>, List<Path>> readWriteFiles = extractAccessedFiles(msgs.errMsgs);
    for (Path p : readWriteFiles.b)
      if (!FileCommands.getExtension(p).equals("log"))
        generate(p);
    for (Path p : readWriteFiles.a)
      require(p);
    
    return new RelativePath(targetDir, input.docName + "pdf");
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
