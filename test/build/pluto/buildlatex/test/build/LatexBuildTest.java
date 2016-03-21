package build.pluto.buildlatex.test.build;

import static build.pluto.buildlatex.test.build.LatexBuildTest.Tool.BIBTEX;
import static build.pluto.buildlatex.test.build.LatexBuildTest.Tool.LATEX;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleHandler;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.buildlatex.Bibtex;
import build.pluto.buildlatex.Latex;
import build.pluto.output.Output;
import build.pluto.output.OutputPersisted;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.util.IReporting;
import build.pluto.util.LogReporting;

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
    Tool[] executedTools = new Tool[executed.size()];
    for (int i = 0; i < executedTools.length; i++)
      if (executed.get(i) == Latex.factory)
        executedTools[i] = LATEX;
      else if (executed.get(i) == Bibtex.factory)
        executedTools[i] = BIBTEX;
    
    assertArrayEquals("Wrong order of executed tools, was " + Arrays.toString(executedTools) + ", expected " + Arrays.toString(order), order, executedTools);
  }

  private void assertRebuildDoesNothing() throws Throwable {
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

  private List<BuilderFactory<?, ?, ?>> executed;
  private void build() throws Throwable {
    executed = new ArrayList<>();
    // Do the build
    IReporting reporting = new LogReporting() {
      private boolean log = false;
      @Override
      public void startBuildCycle(BuildCycle cycle, CycleHandler cycleSupport) {
        log = true;
        super.startBuildCycle(cycle, cycleSupport);
      }
      @Override
      public <O extends Output> void startedBuilder(BuildRequest<?, O, ?, ?> req, Builder<?, ?> b, BuildUnit<O> oldUnit, Set<BuildReason> reasons) {
        if (log)
          executed.add(req.factory);
        super.startedBuilder(req, b, oldUnit, reasons);
      }
      @Override
      public <O extends Output> void finishedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
        // ignore bibtex call when no aux file was available
        if (req.factory == Bibtex.factory && unit.getBuildResult().equals(OutputPersisted.of(null)))
          executed.remove(executed.size() - 1);
        super.finishedBuilder(req, unit);
      }
    };
    BuildManagers.build(new BuildRequest<>(Latex.factory, new Latex.Input("document", dir, dir, null)), reporting);
  }

  @Test
  public void testBuildClean() throws Throwable {
    build();
    assertOrder(LATEX, BIBTEX, LATEX, LATEX);
  }

  @Test(expected = AssertionError.class)
  public void testAssertOrderFailsIfWrong() throws Throwable {
    build();
    assertOrder(LATEX, BIBTEX, BIBTEX);
  }

  @Test
  public void testCleanRebuildDoesNothing() throws Throwable {
    build();
    assertRebuildDoesNothing();
  }

  @Test
  public void testRebuildSimpleTextChange() throws Throwable {
    build();
    replaceInLatexFile("for testing", "to test");
    build();
    assertOrder(LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testRemoveCitationFromTex() throws Throwable {
    build();
    replaceInLatexFile("~\\cite{article1}", "");
    build();
    assertOrder(LATEX, BIBTEX, LATEX, LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testAddCitationToTex() throws Throwable {
    build();
    replaceInLatexFile("~\\cite{misc1}", "~\\cite{misc1} and ~\\cite{inproceedings1}");
    build();
    assertOrder(LATEX, BIBTEX, LATEX, LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testDuplicateCitationInText() throws Throwable {
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
  public void testChangeUsedCitationInBib() throws Throwable {
    build();
    replaceInBibFile("Misc1", "Misc 1");
    build();
    assertOrder(BIBTEX, LATEX);
    assertRebuildDoesNothing();
  }

  @Test
  public void testChangeUnusedCitationInBib() throws Throwable {
    build();
    replaceInBibFile("Inpreceedings1", "Misc 1");
    build();
    assertOrder(BIBTEX);
    assertRebuildDoesNothing();
  }

}
