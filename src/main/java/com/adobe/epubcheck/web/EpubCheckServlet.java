/*
 * Copyright (c) 2007 Adobe Systems Incorporated
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of
 *  this software and associated documentation files (the "Software"), to deal in
 *  the Software without restriction, including without limitation the rights to
 *  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *  the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.adobe.epubcheck.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.adobe.epubcheck.api.EpubCheck;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * Validates an uploaded EPUB (form field {@code file}) with EPUBCheck and returns the
 * report as plain text.
 */
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // spool uploads over 1 MB to disk
    maxFileSize = EpubCheckServlet.MAX_UPLOAD_BYTES,
    maxRequestSize = EpubCheckServlet.MAX_REQUEST_BYTES)
public class EpubCheckServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;

  static final long MAX_UPLOAD_BYTES = 250L * 1024 * 1024;
  static final long MAX_REQUEST_BYTES = MAX_UPLOAD_BYTES + 1024 * 1024; // room for form overhead
  static final String MAX_UPLOAD_LABEL = "250 MB";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    resp.setContentType("text/plain");
    resp.setCharacterEncoding("UTF-8");
    resp.setHeader("X-Content-Type-Options", "nosniff");

    String contentType = req.getContentType();
    if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data"))
    {
      fail(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request type");
      return;
    }
    // Reject an oversized upload from its Content-Length, before reading the body.
    if (req.getContentLengthLong() > MAX_REQUEST_BYTES)
    {
      fail(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, tooLarge());
      return;
    }

    Part book;
    try
    {
      book = req.getPart("file");
    } catch (IllegalStateException e)
    { // the upload exceeded the @MultipartConfig limits
      fail(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, tooLarge());
      return;
    }
    if (book == null || book.getSubmittedFileName() == null)
    {
      fail(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request: no epub uploaded");
      return;
    }

    String name = displayName(book);
    Path work = null;
    try
    {
      work = Files.createTempFile("epubcheck-", ".epub");
      try (InputStream in = book.getInputStream())
      {
        Files.copy(in, work, StandardCopyOption.REPLACE_EXISTING);
      }

      PrintWriter out = resp.getWriter();
      out.println("Checking " + name + " with EPUBCheck " + EpubCheck.version());
      out.println();
      WebReport report = new WebReport(out, work, name);
      int result = new EpubCheck(work.toFile(), report).doValidate();
      if (result == 0)
      {
        out.println("No errors or warnings detected");
      }
      else
      {
        out.println();
        out.println("Check finished with " + report.getFatalErrorCount() + " fatal error(s), "
            + report.getErrorCount() + " error(s) and " + report.getWarningCount()
            + " warning(s)");
      }
    } catch (Exception e)
    {
      log("Validation failed for upload " + name, e);
      if (!resp.isCommitted())
      {
        resp.resetBuffer();
        fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
      }
      else
      {
        resp.getWriter().println("Internal Server Error");
      }
    } finally
    {
      if (work != null)
      {
        Files.deleteIfExists(work);
      }
      book.delete();
    }
  }

  private static void fail(HttpServletResponse resp, int status, String message) throws IOException
  {
    resp.setStatus(status);
    resp.getWriter().println(message);
  }

  private static String tooLarge()
  {
    return "Invalid request: the file is larger than " + MAX_UPLOAD_LABEL
        + ". Please validate large files with the EPUBCheck command-line tool"
        + " (https://github.com/w3c/epubcheck).";
  }

  /** The uploaded file's own name (no path, no control characters), for messages. */
  static String displayName(Part part)
  {
    String name = part.getSubmittedFileName();
    if (name == null) return "[uploaded file]";
    name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
    name = name.replaceAll("\\p{Cntrl}", "").trim();
    if (name.isEmpty()) return "[uploaded file]";
    return name.length() > 200 ? name.substring(0, 200) : name;
  }
}
