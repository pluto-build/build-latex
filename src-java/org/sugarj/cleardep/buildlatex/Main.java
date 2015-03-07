package org.sugarj.cleardep.buildlatex;

import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class Main {

  public static void main(String[] args) {
    Path srcDir = new AbsolutePath("./src-latex");
    Path targetDir = new AbsolutePath("./bin-latex");
    Path texPath = new RelativePath(srcDir, "document.tex");
    BuildManager.build(new BuildRequest<>(LatexBuilder.factory, new LatexBuilder.Input(texPath, srcDir, targetDir, null)));
  }

}
