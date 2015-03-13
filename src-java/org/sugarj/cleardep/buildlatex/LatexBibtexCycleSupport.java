package org.sugarj.cleardep.buildlatex;

import org.sugarj.cleardep.build.FixpointCycleSupport;

public class LatexBibtexCycleSupport extends FixpointCycleSupport {

  public LatexBibtexCycleSupport() {
    super( entry(Bibtex.factory, Latex.Input.class), entry(Latex.factory, Latex.Input.class));
  }

}
