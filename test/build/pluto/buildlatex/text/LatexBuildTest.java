package build.pluto.buildlatex.text;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.sugarj.common.Log;

import build.pluto.buildlatex.Latex;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;

public class LatexBuildTest extends ScopedBuildTest {

  @ScopedPath("document.tex")
  private File texFile;

  @ScopedPath("bib.bib")
  private File bibFile;

  @ScopedPath("")
  private File dir;

  private TrackingBuildManager build() throws IOException {
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.require(Latex.factory, new Latex.Input("document", dir, dir, new File("/opt/local/bin/")));
    return manager;
  }

  @Test
  public void testBuildClean() throws IOException {
    Log.log.setLoggingLevel(Log.IMPORT);
    build();
  }

}
