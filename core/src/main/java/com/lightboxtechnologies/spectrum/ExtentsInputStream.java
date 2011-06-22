package com.lightboxtechnologies.spectrum;

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.FSDataInputStream;

public class ExtentsInputStream extends InputStream {

  protected final FSDataInputStream in;

  protected final Iterator<Map<String,Object>> ext_iter;

  protected Map<String,?> cur_extent = null;
  protected long cur_pos;
  protected long cur_end;

  public ExtentsInputStream(FSDataInputStream in,
                            List<Map<String,Object>> extents) {
    this.in = in;
    this.ext_iter = extents.iterator();
  }
 
  protected boolean prepareExtent() {
    // prepare next extent
    if (cur_extent == null || cur_pos == cur_end) {
      if (ext_iter.hasNext()) {
        cur_extent = ext_iter.next();

        final long length = ((Number) cur_extent.get("len")).longValue();

        cur_pos = ((Number) cur_extent.get("addr")).longValue();
        cur_end = cur_pos + length;

System.err.println("len == " + length + ", cur_pos == " + cur_pos + ", cur_end == " + cur_end);
      }
      else {
        return false;
      }
    }

    return true;
  }

  @Override
  public int read() throws IOException {
    final byte[] b = new byte[1];

    // keep trying until we read one byte or hit EOF
    int rlen;
    do {
      rlen = read(b, 0, 1);
    } while (rlen == 0);

    return rlen == -1 ? -1 : b[0];
  }

  @Override
  public int read(byte[] buf, int off, int len) throws IOException {
    if (!prepareExtent()) {
      return -1;
    }

    // NB: cur_end - cur_pos might be larger than 2^31-1, so we must
    // check that it doesn't overflow an int.
    int rlen = Math.min(len,
      (int) Math.min(cur_end - cur_pos, Integer.MAX_VALUE));

    rlen = in.read(cur_pos, buf, off, rlen);
    cur_pos += rlen;

System.err.println("off == " + off + ", len == " + len + ", rlen == " + rlen);

    return rlen;
  }

/*
  @Override
  public long skip(long n) throws IOException {
    
  }
*/

/*
  @Override
  public int available() throws IOException {
    if (!prepareExtent()) {
      return 0;
    }

    return Math.min(in.available(),
      (int) Math.min(cur_end - cur_pos, Integer.MAX_VALUE));
  }
*/

/*
  @Override
  public void close() throws IOException {
  }
*/
}
