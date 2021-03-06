package build.pluto.buildlatex;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.sugarj.common.Exec;
import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.util.Pair;

import build.pluto.builder.Builder;
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.builder.FixpointCycleHandler;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.output.Out;
import build.pluto.output.OutputPersisted;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;
import build.pluto.util.AbsoluteComparedFile;

public class Latex extends Builder<Latex.Input, Out<File>> {

  public static BuilderFactory<Input, Out<File>, Latex> factory = BuilderFactoryFactory.of(Latex.class, Input.class);
  public static final CycleHandlerFactory latexBibtexCycleSupport = FixpointCycleHandler.of(Bibtex.factory, Latex.factory);

  public static class Input implements Serializable {
    private static final long serialVersionUID = -6065839202426934802L;
    public final String docName;
    public final File srcDir;
    public final File targetDir;
    public final File binaryLocation;

    public Input(String docName, File srcDir, File targetDir, File binaryLocation) {
      this.docName = Objects.requireNonNull(docName, "Latex builder requieres docName parameter");
      this.srcDir = srcDir;
      this.targetDir = targetDir;
      this.binaryLocation = binaryLocation;
    }

  }

  public Latex(Input input) {
    super(input);
  }

  @Override
  protected CycleHandlerFactory getCycleSupport() {
    return latexBibtexCycleSupport;
  }

  @Override
  protected String description(Latex.Input input) {
    return "Build PDF for " + input.docName;
  }

  @Override
  public File persistentPath(Latex.Input input) {
    if (input.targetDir != null)
      return new File(input.targetDir, "latex.dep");
    return new File("./latex.dep");
  }

  @Override
  protected Stamper defaultStamper() {
    return LastModifiedStamper.instance;
  }

  @Override
  protected Out<File> build(Latex.Input input) throws IOException {
    File srcDir = input.srcDir != null ? input.srcDir : new File(".");
    File targetDir = input.targetDir != null ? input.targetDir : new File(".");
    String program = "pdflatex";
    if (input.binaryLocation != null)
      program = input.binaryLocation.getAbsolutePath() + "/" + program;

    Out<File> bblFileWrapper = requireBuild(Bibtex.factory, input);
    File bblFile = bblFileWrapper.val();

    report("Compile Latex " + input.docName);

    File tex = new File(srcDir, input.docName + ".tex");
    File aux = new File(targetDir, input.docName + ".aux");

    require(tex, FileHashStamper.instance);
    require(aux, FileHashStamper.instance);

    Files.createDirectories(targetDir.toPath());
    ExecutionResult msgs = Exec.run(srcDir, program, "-interaction=batchmode", "-output-directory=" + targetDir.getAbsolutePath(), "-kpathsea-debug=4", input.docName + ".tex");

    Pair<List<File>, List<File>> readWriteFiles = extractAccessedFiles(input, msgs.errMsgs);
    for (File p : readWriteFiles.b)
      provide(p);
    for (File p : readWriteFiles.a)
      if (!p.equals(tex) && !p.equals(aux))
        if (AbsoluteComparedFile.equals(p, bblFile))
          require(p, FileHashStamper.instance);
        else
          require(p);

    return OutputPersisted.of(new File(targetDir, input.docName + ".pdf"));
  }

  private Pair<List<File>, List<File>> extractAccessedFiles(Latex.Input input, String[] lines) {
    File srcDir = input.srcDir != null ? input.srcDir : new File(".");
    File targetDir = input.targetDir != null ? input.targetDir : new File(".");

    List<File> readPathList = new ArrayList<>();
    Set<Path> readPaths = new HashSet<>();
    List<File> writePathList = new ArrayList<>();
    Set<Path> writePaths = new HashSet<>();
    for (String line : lines)
      if (line.startsWith("kdebug:fopen(")) {
        int start = "kdebug:fopen(".length();
        int end = line.indexOf(',');
        Path path = new File(line.substring(start, end)).toPath();
        String mode = line.substring(end + 2, end + 3);

        boolean include = false;
        if (path.startsWith(srcDir.toPath()))
          include = true;
        else if (path.startsWith(targetDir.toPath()))
          include = true;
        else if (path.startsWith("./")) {
          path = path.subpath(1, path.getNameCount());
          path = srcDir.toPath().resolve(path);
          include = true;
        }

        if (include && "r".equals(mode) && readPaths.add(path))
          readPathList.add(path.toFile());
        else if (include && "w".equals(mode) && writePaths.add(path))
          writePathList.add(path.toFile());
      }
    return Pair.create(readPathList, writePathList);
  }

}
