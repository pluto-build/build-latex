package build.pluto.buildlatex;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sugarj.common.Exec;
import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.util.Pair;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleSupport;
import build.pluto.builder.FixpointCycleSupport;
import build.pluto.output.Out;
import build.pluto.stamp.FileHashStamper;

public class Latex extends Builder<Latex.Input, Out<File>> {

  public final static BuilderFactory<Input, Out<File>, Latex> factory = new BuilderFactory<Input, Out<File>, Latex>() {
    private static final long serialVersionUID = 357011347823016858L;

    @Override
    public Latex makeBuilder(Input input) { return new Latex(input); }
  };

  public static class Input implements Serializable {
    private static final long serialVersionUID = -6065839202426934802L;
    public final String docName;
    public final File srcDir;
    public final File targetDir;
    public final File binaryLocation;
    public Input(String docName, File srcDir, File targetDir, File binaryLocation) {
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
    return new FixpointCycleSupport(Bibtex.factory, Latex.factory);
  }

  @Override
  protected String description() {
    return "Build PDF for " + input.docName;
  }

  @Override
  protected File persistentPath() {
    if (input.targetDir != null)
      return new File(input.targetDir, "latex.dep");
    return new File("./latex.dep");
  }

  @Override
  protected Out<File> build() throws IOException {
    File srcDir = input.srcDir != null ? input.srcDir : new File(".");
    File targetDir = input.targetDir != null ? input.targetDir : new File(".");
    String program = "pdflatex";
    if (input.binaryLocation != null)
      program = input.binaryLocation.getAbsolutePath() + "/" + program;

    requireBuild(Bibtex.factory, input);

    File tex = new File(srcDir, input.docName + ".tex");
    File aux = new File(targetDir, input.docName + ".aux");
    require(tex, FileHashStamper.instance);
    require(aux, FileHashStamper.instance);

    Files.createDirectories(targetDir.toPath());
    ExecutionResult msgs = Exec.run(srcDir, 
        program, 
        "-interaction=batchmode", 
        "-output-directory=" + targetDir.getAbsolutePath(),
        "-kpathsea-debug=4",
        input.docName + ".tex");

    Pair<List<File>, List<File>> readWriteFiles = extractAccessedFiles(msgs.errMsgs);
    for (File p : readWriteFiles.b)
      provide(p);
    for (File p : readWriteFiles.a)
      if (!p.equals(tex) && !p.equals(aux))
        require(p);
    
    return new Out<>(new File(targetDir, input.docName + "pdf"));
  }

  private Pair<List<File>, List<File>> extractAccessedFiles(String[] lines) {
    File srcDir = input.srcDir != null ? input.srcDir : new File(".");
    File targetDir = input.targetDir != null ? input.targetDir : new File(".");
    
    List<File> readPathList = new ArrayList<>();
    Set<File> readPaths = new HashSet<>();
    List<File> writePathList = new ArrayList<>();
    Set<File> writePaths = new HashSet<>();
    for (String line : lines)
      if (line.startsWith("kdebug:fopen(")) {
        int start = "kdebug:fopen(".length();
        int end = line.indexOf(',');
        File file = new File(line.substring(start, end));
        String mode = line.substring(end + 2, end + 3);
        
        boolean include = false;
        if (file.toPath().startsWith(srcDir.toPath()))
          include = true;
        else if (file.toPath().startsWith(targetDir.toPath()))
          include = true;
        else if (file.getPath().startsWith("./")) {
          file = file.toPath().relativize(file.toPath().getRoot()).toFile();
          include = true;
        }
        
        if (include && "r".equals(mode) && readPaths.add(file))
          readPathList.add(file);
        else if (include && "w".equals(mode) && writePaths.add(file))
          writePathList.add(file);
      }
    return Pair.create(readPathList, writePathList);
  }
  
}
