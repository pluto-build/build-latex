package build.pluto.buildlatex.test.build;

import static build.pluto.buildlatex.test.build.LatexBuildTest.Tool.BIBTEX;
import static build.pluto.buildlatex.test.build.LatexBuildTest.Tool.LATEX;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.sugarj.common.FileCommands;

import build.pluto.builder.factory.BuilderFactory;
import build.pluto.buildlatex.Bibtex;
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

  protected static enum Tool {
    LATEX,
    BIBTEX
  }

  private void assertOrder(Tool... order) {
    List<BuilderFactory<?, ?, ?>> executed = manager.getExecutedTools();
    assertEquals("Wrong number of executed tools", order.length, executed.size());
    
    for (int i = 0; i < order.length; i++) {
      Tool tool = order[i];
      BuilderFactory<?, ?, ?> fact = executed.get(i);
      
      if (tool == LATEX)
        assertEquals("Wrong order of executed tools", Latex.factory, fact);
      else if (tool == BIBTEX)
        assertEquals("Wrong order of executed tools", Bibtex.factory, fact);
      
    }
  }

  private void assertRebuildDoesNothing() throws IOException {
    build();
    assertOrder();
  }

  private static void replaceInFile(File file, String find, String replace) throws IOException {
    String content = FileCommands.readFileAsString(file);

    content = content.replaceAll(Pattern.quote(find), Matcher.quoteReplacement(replace));
    FileCommands.writeToFile(file, content);
  }

  private void replaceInLatexFile(String find, String replace) throws IOException {
    replaceInFile(texFile, find, replace);
  }

  private void replaceInBibFile(String find, String replace) throws IOException {
    replaceInFile(bibFile, find, replace);
  }

  private TrackingBuildManager manager;
  private TrackingBuildManager build() throws IOException {
    // Do the build
    manager = new TrackingBuildManager();
    manager.require(Latex.factory, new Latex.Input("document", dir, dir, null));
    return manager;
  }

  @Test
  public void testBuildClean() throws IOException {
    build();
    assertOrder(LATEX, BIBTEX, LATEX, LATEX);
  }

  @Test(expected = AssertionError.class)
  public void testAssertOrderFailsIfWrong() throws IOException {
    build();
    assertOrder(LATEX, BIBTEX, BIBTEX);
  }

  @Test
  public void testCleanRebuildDoesNothing() throws IOException {
    build();
    assertRebuildDoesNothing();
  }

  @Test
  public void testRebuildSimpleTextChange() throws IOException {
    build();
    replaceInLatexFile("for testing", "to test");
    build();
    assertOrder(LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testRemoveCitationFromTex() throws IOException {
    build();
    replaceInLatexFile("~\\cite{article1}", "");
    build();
    assertOrder(LATEX, BIBTEX, LATEX, LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testAddCitationToTex() throws IOException {
    build();
    replaceInLatexFile("~\\cite{misc1}", "~\\cite{misc1} and ~\\cite{inproceedings1}");
    build();
    assertOrder(LATEX, BIBTEX, LATEX, LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testDuplicateCitationInText() throws IOException {
    build();
    replaceInLatexFile("~\\cite{misc1}", "~\\cite{misc1} and ~\\cite{misc1}");
    build();
    // Require Latex two times, because after the first run the citations get
    // duplicated
    // in the aux file, but this is not visible for bitex
    assertOrder(LATEX, LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testChangeUsedCitationInBib() throws IOException {
    build();
    replaceInBibFile("Misc1", "Misc 1");
    build();
    assertOrder(BIBTEX, LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testChangeUnusedCitationInBib() throws IOException {
    build();
    replaceInBibFile("Inpreceedings1", "Misc 1");
    build();
    assertOrder(BIBTEX);
    assertRebuildDoesNothing();
  }

}
