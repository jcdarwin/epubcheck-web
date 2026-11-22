package com.adobe.epubcheck.web;

import java.io.PrintWriter;
import java.nio.file.Path;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.api.MasterReport;
import com.adobe.epubcheck.messages.Message;
import com.adobe.epubcheck.messages.Severity;
import com.adobe.epubcheck.util.FeatureEnum;

/**
 * Writes EPUBCheck's fatal errors, errors and warnings to the response, one per line,
 * in the same format as EPUBCheck's command-line {@code WriterReportImpl}.
 *
 * Unlike {@code WriterReportImpl}, it doesn't print a blank warning when constructed or
 * print INFO lines to {@code System.out} (where they end up in Tomcat's log), and it
 * names the uploaded file rather than the server's temp file.
 */
class WebReport extends MasterReport
{
  private final PrintWriter out;
  private final String workName; // the temp file's name; EPUBCheck reports its path in several forms
  private final String displayName;

  WebReport(PrintWriter out, Path workFile, String displayName)
  {
    this.out = out;
    this.workName = workFile.getFileName().toString();
    this.displayName = displayName.isEmpty() ? "[uploaded file]" : displayName;
    setEpubFileName(this.displayName);
  }

  @Override
  protected void message(Message message, EPUBLocation location, Object... args)
  {
    String label;
    Severity severity = message.getSeverity();
    if (severity.equals(Severity.FATAL)) label = "FATAL";
    else if (severity.equals(Severity.ERROR)) label = "ERROR";
    else if (severity.equals(Severity.WARNING)) label = "WARNING";
    else return; // usage, info and suppressed messages aren't shown

    String text = message.getMessage(args);
    text = text == null ? "" : text.replaceAll("\\s+", " ");
    int line = location.getLine();
    int column = location.getColumn();
    out.println(label + "(" + message.getID() + "): " + resource(location.getPath())
        + (line <= 0 ? "" : "(" + line + (column <= 0 ? "" : "," + column) + ")") + ": " + text);
  }

  private String resource(String path)
  {
    if (path == null || path.isEmpty()) return "[top level]";
    return path.endsWith(workName) ? displayName : path;
  }

  @Override
  public void info(String resource, FeatureEnum feature, String value)
  {
  }

  @Override
  public int generate()
  {
    return 0;
  }

  @Override
  public void initialize()
  {
  }
}
