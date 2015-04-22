package build.pluto.buildlatex;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

import org.sugarj.common.Exec;
import org.sugarj.common.FileCommands;
import org.sugarj.common.util.Pair;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleSupport;
import build.pluto.builder.FixpointCycleSupport;
import build.pluto.buildlatex.Latex.Input;
import build.pluto.output.Out;
import build.pluto.stamp.ValueStamp;

public class Bibtex extends Builder<Latex.Input, Out<File>> {

  public final static BuilderFactory<Input, Out<File>, Bibtex> factory = new BuilderFactory<Input, Out<File>, Bibtex>() {
    private static final long serialVersionUID = 2390540998732457948L;

    @Override
    public Bibtex makeBuilder(Input input) { return new Bibtex(input); }
  };
  
  private Bibtex(Input input) {
    super(input);
  }
  
  @Override
  protected CycleSupport getCycleSupport() {
    return new FixpointCycleSupport(Bibtex.factory, Latex.factory);
  }

  @Override
  protected String description() {
    return "Build bibliography for " + input.docName;
  }

  @Override
  protected File persistentPath() {
    if (input.targetDir != null)
      return new File(input.targetDir, "bibtex.dep");
    return new File("./bibtex.dep");
  }

  @Override
  protected Out<File> build() throws Throwable {
    File srcDir = input.srcDir != null ? input.srcDir : new File(".");
    File targetDir = input.targetDir != null ? input.targetDir : new File(".");
    String program = "bibtex";
    if (input.binaryLocation != null) {
      program = input.binaryLocation.getAbsolutePath() + "/" + program;
    }

    requireBuild(Latex.factory, input);
    
    File auxPath = new File(targetDir, input.docName + ".aux");
    if (!Files.exists(auxPath.toPath()))
      return new Out<>(null);
      
    ValueStamp<Pair<Map<String,String>, Set<String>>> bibtexSourceStamp = BibtexAuxStamper.instance.stampOf(auxPath);
    require(auxPath, bibtexSourceStamp);

    if (!Files.exists(auxPath.toPath()))
      throw new IllegalArgumentException("No bibliography built: Could not find " + auxPath);
    
    Set<String> bibnames = bibtexSourceStamp.val.a.keySet();

    Files.createDirectories(targetDir.toPath());
    if (srcDir != null && targetDir != null && !srcDir.equals(targetDir))
      for (String bibname : bibnames) {
        File srcbib = new File(srcDir, bibname + ".bib");
        File buildbib = new File(targetDir, bibname + ".bib");
  
        require(srcbib);
        Files.copy(srcbib.toPath(), buildbib.toPath(), StandardCopyOption.REPLACE_EXISTING);
        provide(buildbib);
      }

    Exec.run(targetDir, program, input.docName);

    File bbl = FileCommands.replaceExtension(auxPath, "bbl");
    provide(bbl);
    
    return new Out<>(bbl);
  }
  
}
