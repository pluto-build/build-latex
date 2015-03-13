package org.sugarj.cleardep.buildlatex;

import org.sugarj.cleardep.build.FixpointCycleSupport;

public class LatexBibtexCycleSupport extends FixpointCycleSupport {

  public LatexBibtexCycleSupport() {
    super( entry(BibtexBuilder.factory, LatexBuilder.Input.class), entry(LatexBuilder.factory, LatexBuilder.Input.class));
  }

}
