package build.pluto.buildlatex.test.build;

import static build.pluto.buildlatex.test.build.LatexBuildTest.Tool.BIBTEX;
import static build.pluto.buildlatex.test.build.LatexBuildTest.Tool.LATEX;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import build.pluto.buildlatex.Latex;
import build.pluto.buildlatex.test.util.TrackingOutputStream;
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

  private static TrackingOutputStream log;
  private static PrintStream oldOut;

  @BeforeClass
  public static void installCapturingSystemOut() {
    oldOut = System.out;
    log = new TrackingOutputStream(System.out);
    System.setOut(new PrintStream(log));
  }

  @AfterClass
  public static void resetSystemOut() {
    System.setOut(oldOut);
  }

  protected static enum Tool {

    LATEX {

      @Override
      protected String getLog() {
        return "Compile Latex";
      }

    },
    BIBTEX {

      @Override
      protected String getLog() {
        return "Bibtex for";
      }

    };
    protected abstract String getLog();
  }

  private void assertOrder(Tool... order) {
    String logContent = log.getContent();

    // Get indices
    List<Tool> detectedTools = new ArrayList<>();

    int firstToolIndex;
    do {
      firstToolIndex = -1;
      Tool firstTool = null;
      for (Tool tool : Tool.values()) {
        int index = logContent.indexOf(tool.getLog());
        if (index != -1 && (firstToolIndex == -1 || index < firstToolIndex)) {
          firstToolIndex = index;
          firstTool = tool;
        }
      }
      if (firstToolIndex != -1) {
        detectedTools.add(firstTool);
        logContent = logContent.substring(firstToolIndex + firstTool.getLog().length());
      }
    } while (firstToolIndex != -1);

    assertEquals("Wrong order of executed tools", Arrays.asList(order), detectedTools);

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

  private TrackingBuildManager build() throws IOException {
    // Forget previous log
    log.getContent();
    // Do the build
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.require(Latex.factory, new Latex.Input("document", dir, dir, new File("/opt/local/bin/")));
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
