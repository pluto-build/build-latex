package build.pluto.buildlatex.test.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class ComposedOutputStream extends OutputStream {

  private List<OutputStream> children;

  public ComposedOutputStream(OutputStream... streams) {
    this.children = Arrays.asList(streams);
  }

  @Override
  public void write(final int b) throws IOException {
    for (OutputStream child : this.children) {
      child.write(b);
    }
  }

}
