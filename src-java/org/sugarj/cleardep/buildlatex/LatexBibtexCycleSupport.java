package org.sugarj.cleardep.buildlatex;

import org.sugarj.cleardep.build.FixpointCycleSupport;

public class LatexBibtexCycleSupport extends FixpointCycleSupport {

  public LatexBibtexCycleSupport() {
    super(Bibtex.factory, Latex.factory);
  }

}
