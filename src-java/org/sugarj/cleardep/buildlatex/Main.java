package org.sugarj.cleardep.buildlatex;

import java.io.File;

import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class Main {

  public static void main(String[] args) {
    File javaBin = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    Path root = new AbsolutePath(javaBin.getParentFile().getAbsolutePath());
    
    Path srcDir = new RelativePath(root, "src-latex");
    Path targetDir = new RelativePath(root, "bin-latex");
    Path texPath = new RelativePath(srcDir, "document.tex");
    BuildManager.build(new BuildRequest<>(LatexBuilder.factory, new LatexBuilder.Input(texPath, srcDir, targetDir, null)));
  }

}
