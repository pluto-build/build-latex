package org.sugarj.cleardep.buildlatex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildRequest;
import build.pluto.dependency.FileRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.output.None;

public class Main {

  public static void main(String[] args) throws IOException {
    if (args.length > 0)
      mainSimple();
    else 
      mainComplex();
  }

  private static void mainComplex() {
    File javaBin = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    Path root = new AbsolutePath(javaBin.getParentFile().getAbsolutePath());
    
    Path srcDir = new RelativePath(root, "src-latex");
    Path targetDir = new RelativePath(root, "bin-latex");
    
    BuildManager.build(new BuildRequest<>(Latex.factory, new Latex.Input("document", srcDir, targetDir, null)));
  }
  
  private static void mainSimple() throws IOException {
    File javaBin = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    Path root = new AbsolutePath(javaBin.getParentFile().getAbsolutePath());
    
    Path srcDir = new RelativePath(root, "src-latex-simple");
    Path targetDir = new RelativePath(root, "bin-latex-simple");
    
    BuildManager.build(new BuildRequest<>(Latex.factory, new Latex.Input("doc", srcDir, targetDir, null)));
    
    BuildUnit<Path> res = BuildManager.readResult(new BuildRequest<>(Latex.factory, new Latex.Input("doc", srcDir, targetDir, null)));
    
    List<Path> require = new ArrayList<>();
    List<Path> provide = new ArrayList<>(res.getGeneratedFiles());
    for (Requirement req : res.getRequirements())
      if (req instanceof FileRequirement && FileCommands.exists(((FileRequirement) req).path))
        require.add(((FileRequirement) req).path);
    
    System.out.println(require);
    System.out.println(provide);
    
    
    BuildUnit<None> resB = BuildManager.readResult(new BuildRequest<>(Bibtex.factory, new Latex.Input("doc", srcDir, targetDir, null)));
    
    List<Path> requireB = new ArrayList<>();
    List<Path> provideB = new ArrayList<>(resB.getGeneratedFiles());
    for (Requirement req : resB.getRequirements())
      if (req instanceof FileRequirement && FileCommands.exists(((FileRequirement) req).path))
        requireB.add(((FileRequirement) req).path);
    
    System.out.println(requireB);
    System.out.println(provideB);

  }

}
