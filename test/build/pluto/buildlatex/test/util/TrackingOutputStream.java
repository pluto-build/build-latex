package build.pluto.buildlatex.test.util;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class TrackingOutputStream extends OutputStream {

  private ComposedOutputStream stream;
  private ByteArrayOutputStream logStream;
  private OutputStream capturedStream;

  public TrackingOutputStream(OutputStream stream) {
    this.capturedStream = stream;
    this.logStream = new ByteArrayOutputStream();
    this.stream = new ComposedOutputStream(this.capturedStream, this.logStream);
  }

  @Override
  public void write(int b) throws IOException {
    this.stream.write(b);
  }

  public String getContent() {
    String content = this.logStream.toString();
    this.logStream.reset();
    return content;
  }


}
