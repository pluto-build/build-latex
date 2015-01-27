import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.CompilationUnit.State;
import org.sugarj.cleardep.SimpleCompilationUnit;
import org.sugarj.cleardep.SimpleMode;
import org.sugarj.cleardep.stamp.ContentStamper;
import org.sugarj.cleardep.stamp.GeneratedFilesModuleStamper;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.ModuleStamper;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.CommandExecution.ExecutionError;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class Build {

  public static CommandExecution runner = new CommandExecution(false);
  public static Stamper stamper = LastModifiedStamper.instance;

  public static ModuleStamper bblStamper = new GeneratedFilesModuleStamper(LastModifiedStamper.instance, "bbl");

  public static SimpleMode mode = new SimpleMode();

  public static RelativePath rel(String p) {
    return new RelativePath(new AbsolutePath("."), p);
  }

  public static RelativePath rel(Path p1, String p2) {
    return new RelativePath(p1, p2);
  }

  public static Map<RelativePath, Stamp> sourceMap(RelativePath sourceFile) {
    return Collections.singletonMap(sourceFile, stamper.stampOf(sourceFile));
  }

  private static RelativePath build = rel("bin-latex");
  private static RelativePath src = rel("src-latex");

  public static void main(String[] args) throws IOException {
    RelativePath tex = rel(src, "document.tex");

    if (args.length > 0 && "clean".equals(args[0]))
      clean();
    else
      build(tex);
  }

  private static void clean() throws IOException {
    log("Clean", "Cleaning files in " + build);
    FileCommands.delete(build);
  }

  private static void build(RelativePath tex) throws IOException {
    RelativePath dep = FileCommands.replaceExtension(rel(build, tex.getRelativePath()), "dep");

    while (SimpleCompilationUnit.readConsistent(stamper, mode, null, dep) == null) {
      CompilationUnit result = SimpleCompilationUnit.create(stamper, mode, null, dep);
      buildTex(tex, result);
    }
  }

  /**
   * @return true if build was required.
   * @throws IOException
   */
  private static void buildTex(RelativePath tex, CompilationUnit result) throws IOException {
    log("tex", "Build tex " + tex);

    RelativePath aux = FileCommands.replaceExtension(rel(build, tex.getRelativePath()), "aux");
    result.addSourceArtifact(tex);
    result.addSourceArtifact(aux, ContentStamper.instance.stampOf(aux));

    RelativePath bibdep = FileCommands.replaceExtension(rel(build, tex.getRelativePath()), "bibdep");
    CompilationUnit bibresult = SimpleCompilationUnit.readConsistent(stamper, mode, null, bibdep);
    if (bibresult == null) {
      bibresult = SimpleCompilationUnit.create(stamper, mode, null, bibdep);
      buildBib(aux, bibresult);
    }
    
    result.addModuleDependency(bibresult, bblStamper.stampOf(bibresult));
    
    FileCommands.createDir(build);
    try {
      runner.execute(src, "pdflatex", "-interaction=batchmode", "-output-directory=../" + FileCommands.fileName(build) + "/", FileCommands.dropDirectory(tex));
      result.setState(State.SUCCESS);
    } catch (ExecutionError e) {
      System.err.println(e.getMessage());
      result.setState(State.FAILURE);
    }

    RelativePath pdf = FileCommands.replaceExtension(rel(build, tex.getRelativePath()), "pdf");
    result.addGeneratedFile(aux);
    result.addGeneratedFile(pdf);

    result.write();
    logCompilationUnit(result);
  }

  private static void buildBib(RelativePath aux, CompilationUnit result) throws IOException {
    log("bibtex", "Build bib for " + aux);

    BibtexAuxRequirementsStamper.BibtexAuxRequirementsStamp bibtexSourceStamp = BibtexAuxRequirementsStamper.instance.stampOf(aux);
    result.addSourceArtifact(aux, bibtexSourceStamp);

    if (!FileCommands.exists(aux)) {
      log("bibtex", "No bibliography built: Could not find " + aux);
      result.setState(State.SUCCESS);
      result.write();
      logCompilationUnit(result);
      return;
    }

    Set<String> bibnames = bibtexSourceStamp.bibdatas.keySet();
    for (String bibname : bibnames) {
      RelativePath srcbib = rel(src, bibname + ".bib");
      RelativePath buildbib = rel(build, bibname + ".bib");

      result.addExternalFileDependency(srcbib);
      FileCommands.copyFile(srcbib, buildbib);
      result.addGeneratedFile(buildbib);
    }

    FileCommands.createDir(build);
    try {
      runner.execute(build, "bibtex", FileCommands.fileName(aux));
      result.setState(State.SUCCESS);
    } catch (ExecutionError e) {
      System.err.println(e.getMessage());
      result.setState(State.FAILURE);
    }

    RelativePath bbl = FileCommands.replaceExtension(aux, "bbl");
    result.addGeneratedFile(bbl);

    result.write();
    logCompilationUnit(result);
  }

  private static void logCompilationUnit(CompilationUnit result) {
    // System.out.println("Required modules: " +
    // result.getModuleDependencies());
    // System.out.println("Required modules (circular): " +
    // result.getCircularModuleDependencies());
    // System.out.println("Required files: " +
    // result.getExternalFileDependencies());
    // System.out.println("Generated files: " + result.getGeneratedFiles());
  }

  private static void log(String prefix, String s) {
    System.out.println("[" + prefix + "] " + s);
  }

}
