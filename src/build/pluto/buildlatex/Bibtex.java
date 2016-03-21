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
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.buildlatex.Latex.Input;
import build.pluto.output.IgnoreOutputStamper;
import build.pluto.output.Out;
import build.pluto.output.OutputPersisted;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.Stamper;
import build.pluto.stamp.ValueStamp;

public class Bibtex extends Builder<Latex.Input, Out<File>> {

  public final static BuilderFactory<Input, Out<File>, Bibtex> factory = BuilderFactoryFactory.of(Bibtex.class, Latex.Input.class);

  public Bibtex(Input input) {
    super(input);
  }

  @Override
  protected CycleHandlerFactory getCycleSupport() {
    return Latex.latexBibtexCycleSupport;
  }

  @Override
  protected String description(Latex.Input input) {
    return "Build bibliography for " + input.docName;
  }

  @Override
  public File persistentPath(Latex.Input input) {
    if (input.targetDir != null)
      return new File(input.targetDir, "bibtex.dep");
    return new File("./bibtex.dep");
  }

  @Override
  protected Stamper defaultStamper() {
    return FileContentStamper.instance;
  }

  @Override
  protected Out<File> build(Latex.Input input) throws Throwable {
    File srcDir = input.srcDir != null ? input.srcDir : new File(".");
    File targetDir = input.targetDir != null ? input.targetDir : new File(".");
    String program = "bibtex";
    if (input.binaryLocation != null) {
      program = input.binaryLocation.getAbsolutePath() + "/" + program;
    }

    requireBuild(Latex.factory, input, IgnoreOutputStamper.instance);

    File auxPath = new File(targetDir, input.docName + ".aux");
    ValueStamp<Pair<Map<String, String>, Set<String>>> bibtexSourceStamp = BibtexAuxStamper.instance.stampOf(auxPath);
    require(auxPath, bibtexSourceStamp);

    if (!Files.exists(auxPath.toPath())) {
      report("No Aux file at " + auxPath + " found");
      return OutputPersisted.of(null);
    }

    report("Bibtex for " + input.docName);

    Set<String> bibnames = bibtexSourceStamp.val.a.keySet();

    Files.createDirectories(targetDir.toPath());
    if (srcDir != null && targetDir != null)
      for (String bibname : bibnames) {
        File srcbib = new File(srcDir, bibname + ".bib");
        if (!srcDir.equals(targetDir)) {
          File buildbib = new File(targetDir, bibname + ".bib");
          Files.copy(srcbib.toPath(), buildbib.toPath(), StandardCopyOption.REPLACE_EXISTING);
          provide(buildbib);
        }
        require(srcbib);

      }

    Exec.run(false, targetDir, program, input.docName);

    File bbl = FileCommands.replaceExtension(auxPath.toPath(), "bbl").toFile();
    provide(bbl);

    return OutputPersisted.of(bbl);
  }

}
