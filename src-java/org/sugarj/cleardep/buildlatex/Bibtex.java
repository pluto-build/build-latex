package org.sugarj.cleardep.buildlatex;

import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.buildlatex.Latex.Input;
import org.sugarj.common.Exec;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleSupport;
import build.pluto.builder.FixpointCycleSupport;
import build.pluto.output.None;
import build.pluto.stamp.ValueStamp;

public class Bibtex extends Builder<Latex.Input, None> {

  public final static BuilderFactory<Input, None, Bibtex> factory = new BuilderFactory<Input, None, Bibtex>() {
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
  protected Path persistentPath() {
    if (input.targetDir != null)
      return new RelativePath(input.targetDir, "bibtex.dep");
    return new AbsolutePath("./bibtex.dep");
  }

  @Override
  protected None build() throws Throwable {
    Path srcDir = input.srcDir != null ? input.srcDir : new AbsolutePath(".");
    Path targetDir = input.targetDir != null ? input.targetDir : new AbsolutePath(".");
    String program = "bibtex";
    if (input.binaryLocation != null) {
      program = input.binaryLocation.getAbsolutePath() + "/" + program;
    }

    requireBuild(Latex.factory, input);
    
    RelativePath auxPath = new RelativePath(targetDir, input.docName + ".aux");
    if (!FileCommands.exists(auxPath))
      return None.val;
      
    ValueStamp<Pair<Map<String,String>, Set<String>>> bibtexSourceStamp = BibtexAuxStamper.instance.stampOf(auxPath);
    require(auxPath, bibtexSourceStamp);

    if (!FileCommands.exists(auxPath))
      throw new IllegalArgumentException("No bibliography built: Could not find " + auxPath);
    
    Set<String> bibnames = bibtexSourceStamp.val.a.keySet();
    
    FileCommands.createDir(targetDir);
    if (srcDir != null && targetDir != null && !srcDir.equals(targetDir))
      for (String bibname : bibnames) {
        RelativePath srcbib = new RelativePath(srcDir, bibname + ".bib");
        RelativePath buildbib = new RelativePath(targetDir, bibname + ".bib");
  
        require(srcbib);
        FileCommands.copyFile(srcbib, buildbib);
        provide(buildbib);
      }

    Exec.run(targetDir, program, input.docName);

    Path bbl = auxPath.replaceExtension("bbl");
    provide(bbl);
    
    return None.val;
  }
  
}
