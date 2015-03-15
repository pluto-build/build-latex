package org.sugarj.cleardep.buildlatex;

import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.build.CycleSupport;
import org.sugarj.cleardep.buildlatex.Latex.Input;
import org.sugarj.cleardep.output.None;
import org.sugarj.cleardep.stamp.ValueStamp;
import org.sugarj.common.Exec;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;

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
    return new LatexBibtexCycleSupport();
  }

  @Override
  protected String taskDescription() {
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
    
    requireBuild(Latex.factory, input);
    
    Path srcDir = input.srcDir != null ? input.srcDir : new AbsolutePath(".");
    Path targetDir = input.targetDir != null ? input.targetDir : new AbsolutePath(".");

    RelativePath auxPath = new RelativePath(targetDir, input.docName + ".aux");
    ValueStamp<Pair<Map<String,String>, Set<String>>> bibtexSourceStamp = BibtexAuxRequirementsStamper.instance.stampOf(auxPath);
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
        generate(buildbib);
      }

    String program = "bibtex";
    if (input.binaryLocation != null) {
      program = input.binaryLocation.getAbsolutePath() + "/" + program;
    }
    Exec.run(targetDir, program, input.docName);

    Path bbl = auxPath.replaceExtension("bbl");
    generate(bbl);
    
    return None.val;
  }
  
}
