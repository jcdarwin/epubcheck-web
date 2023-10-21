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

package com.adobe.epubcheck.opf;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import org.w3c.epubcheck.core.references.Reference;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.messages.MessageId;
import com.adobe.epubcheck.util.DateParser;
import com.adobe.epubcheck.util.EPUBVersion;
import com.adobe.epubcheck.util.FeatureEnum;
import com.adobe.epubcheck.util.InvalidDateException;
import com.adobe.epubcheck.xml.handlers.XMLHandler;
import com.adobe.epubcheck.xml.model.XMLElement;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import io.mola.galimatias.URL;

public class OPFHandler extends XMLHandler
{

  protected static final String TEXT = "text";

  protected String pageMapId = null;
  protected EPUBLocation pageMapReferenceLocation = null;

  // Maps to OPFItem builders, to be built after parsing
  protected final Map<String, OPFItem.Builder> itemBuilders = Maps.newLinkedHashMap();
  protected final Map<URL, OPFItem.Builder> itemBuildersByURL = Maps.newLinkedHashMap();
  // A list of all spine item IDs
  private final List<String> spineIDs = new LinkedList<String>();
  // Represents the set of items in this Package Doc
  private OPFItems items = null;

  // Hashtable encryptedItems;

  private final Vector<OPFReference> refs = new Vector<OPFReference>();

  private static final HashSet<String> validRoles = new HashSet<String>();

  // This string holds the value of the <package> element's unique-identifier
  // attribute
  // that will be used to make sure that the unique-identifier references an
  // existing
  // <dc:identifier> id attribute
  private String uniqueIdent;

  // This boolean specifies whether or not there has been a <dc:identifier>
  // element
  // parsed that has an id attribute that corresponds with the
  // unique-identifier attribute
  // from the packaging element. The default value is false.
  private boolean uniqueIdentExists = false;
  // This string holds the value of the <dc:identifier> element detected
  String uid;

  private boolean opf12PackageFile = false;

  // counts the position of itemrefs in the spine
  private int spineItemCounter = 0;

  static
  {
    String[] list = { 
        "abr",
        "acp",
        "act",
        "adi",
        "adp",
        "aft",
        "anl",
        "anm",
        "ann",
        "ant",
        "ape",
        "apl",
        "app",
        "aqt",
        "arc",
        "ard",
        "arr",
        "art",
        "asg",
        "asn",
        "ato",
        "att",
        "auc",
        "aud",
        "aui",
        "aus",
        "aut",
        "bdd",
        "bjd",
        "bkd",
        "bkp",
        "blw",
        "bnd",
        "bpd",
        "brd",
        "brl",
        "bsl",
        "cas",
        "ccp",
        "chr",
        "clb",
        "cli",
        "cll",
        "clr",
        "clt",
        "cmm",
        "cmp",
        "cmt",
        "cnd",
        "cng",
        "cns",
        "coe",
        "col",
        "com",
        "con",
        "cor",
        "cos",
        "cot",
        "cou",
        "cov",
        "cpc",
        "cpe",
        "cph",
        "cpl",
        "cpt",
        "cre",
        "crp",
        "crr",
        "crt",
        "csl",
        "csp",
        "cst",
        "ctb",
        "cte",
        "ctg",
        "ctr",
        "cts",
        "ctt",
        "cur",
        "cwt",
        "dbp",
        "dfd",
        "dfe",
        "dft",
        "dgc",
        "dgg",
        "dgs",
        "dis",
        "dln",
        "dnc",
        "dnr",
        "dpc",
        "dpt",
        "drm",
        "drt",
        "dsr",
        "dst",
        "dtc",
        "dte",
        "dtm",
        "dto",
        "dub",
        "edc",
        "edm",
        "edt",
        "egr",
        "elg",
        "elt",
        "eng",
        "enj",
        "etr",
        "evp",
        "exp",
        "fac",
        "fds",
        "fld",
        "flm",
        "fmd",
        "fmk",
        "fmo",
        "fmp",
        "fnd",
        "fpy",
        "frg",
        "gis",
        "grt",
        "his",
        "hnr",
        "hst",
        "ill",
        "ilu",
        "ins",
        "inv",
        "isb",
        "itr",
        "ive",
        "ivr",
        "jud",
        "jug",
        "lbr",
        "lbt",
        "ldr",
        "led",
        "lee",
        "lel",
        "len",
        "let",
        "lgd",
        "lie",
        "lil",
        "lit",
        "lsa",
        "lse",
        "lso",
        "ltg",
        "lyr",
        "mcp",
        "mdc",
        "med",
        "mfp",
        "mfr",
        "mod",
        "mon",
        "mrb",
        "mrk",
        "msd",
        "mte",
        "mtk",
        "mus",
        "nrt",
        "opn",
        "org",
        "orm",
        "osp",
        "oth",
        "own",
        "pad",
        "pan",
        "pat",
        "pbd",
        "pbl",
        "pdr",
        "pfr",
        "pht",
        "plt",
        "pma",
        "pmn",
        "pop",
        "ppm",
        "ppt",
        "pra",
        "prc",
        "prd",
        "pre",
        "prf",
        "prg",
        "prm",
        "prn",
        "pro",
        "prp",
        "prs",
        "prt",
        "prv",
        "pta",
        "pte",
        "ptf",
        "pth",
        "ptt",
        "pup",
        "rbr",
        "rcd",
        "rce",
        "rcp",
        "rdd",
        "red",
        "ren",
        "res",
        "rev",
        "rpc",
        "rps",
        "rpt",
        "rpy",
        "rse",
        "rsg",
        "rsp",
        "rsr",
        "rst",
        "rth",
        "rtm",
        "sad",
        "sce",
        "scl",
        "scr",
        "sds",
        "sec",
        "sgd",
        "sgn",
        "sht",
        "sll",
        "sng",
        "spk",
        "spn",
        "spy",
        "srv",
        "std",
        "stg",
        "stl",
        "stm",
        "stn",
        "str",
        "tcd",
        "tch",
        "ths",
        "tld",
        "tlp",
        "trc",
        "trl",
        "tyd",
        "tyg",
        "uvp",
        "vac",
        "vdg",
        "voc",
        "wac",
        "wal",
        "wam",
        "wat",
        "wdc",
        "wde",
        "win",
        "wit",
        "wpr",
        "wst" };
    Collections.addAll(validRoles, list);
  }

  public OPFHandler(ValidationContext context)
  {
    super(context);
  }

  public String getPath()
  {
    return path;
  }

  public boolean getOpf12PackageFile()
  {
    return (opf12PackageFile);
  }

  public boolean getOpf20PackageFile()
  {
    return (!opf12PackageFile);
  }

  /**
   * Search the list of item by ID.
   * 
   * @param id
   *        the ID of the item to search
   * @return an {@link Optional} containing the item of the given ID if found,
   *           or {@link Optional#absent()}
   */
  public Optional<OPFItem> getItemById(String id)
  {
    return (items != null) ? items.getItemById(id) : Optional.<OPFItem> absent();
  }

  /**
   * Search the list of item by URL.
   * 
   * @param id
   *        the URL of the item to search
   * @return an {@link Optional} containing the item of the given URL if found,
   *           or {@link Optional#absent()}
   */
  public Optional<OPFItem> getItemByURL(URL url)
  {
    return (items != null) ? items.getItemByURL(url) : Optional.<OPFItem> absent();
  }

  /**
   * Returns an immutable list of the items in the spine. May contain duplicates
   * if several <code>itemref</code> elements point to the same item.
   * <p>
   * Returns the empty list if the items have not been parsed yet.
   * </p>
   * 
   * @return the list of items in the spine, guaranteed non-null.
   */
  public List<OPFItem> getSpineItems()
  {
    return (items != null) ? items.getSpineItems() : ImmutableList.<OPFItem> of();
  }

  /**
   * Returns an immutable list of all the items in the OPF, in document order,
   * including those represented by <code>link</code> elements pointing to local
   * resources.
   * 
   * <p>
   * Returns the empty list if the items have not been parsed yet.
   * </p>
   * 
   * @return the list of all items, guaranteed non-null.
   */
  public List<OPFItem> getItems()
  {
    return (items != null) ? items.getItems() : ImmutableList.<OPFItem> of();
  }

  public int getReferenceCount()
  {
    return refs.size();
  }

  public OPFReference getReference(int index)
  {
    return refs.elementAt(index);
  }

  /**
   * Checks to see if the unique-identifier attribute of the package element
   * references an existing DC metadata identifier element's id attribute
   *
   * @return true if there is an identifier with an id attribute that matches
   *           the value of the unique-identifier attribute of the package
   *           element. False otherwise.
   */
  public boolean checkUniqueIdentExists()
  {
    return uniqueIdentExists;
  }

  public String getUid()
  {
    return uid;
  }

  // public void setEncryptedItems(Hashtable encryptedItems) {
  // this.encryptedItems = encryptedItems;
  // }

  private static boolean isValidRole(String role)
  {
    return validRoles.contains(role) || role.startsWith("oth.");
  }

  @Override
  public void startElement()
  {
    XMLElement e = currentElement();
    String ns = e.getNamespace();

    if (ns == null || ns.equals("") || ns.equals("http://openebook.org/namespaces/oeb-package/1.0/")
        || ns.equals("http://www.idpf.org/2007/opf"))
    {
      String name = e.getName();
      if (name.equals("package"))
      {
        if (!ns.equals("http://www.idpf.org/2007/opf"))
        {
          report.message(MessageId.OPF_047, location());
          opf12PackageFile = true;
        }
        /*
         * This section checks to see the value of the unique-identifier
         * attribute and stores it in the String uniqueIdent or reports an error
         * if the unique-identifier attribute is missing or does not have a
         * value
         */
        String uniqueIdentAttr = e.getAttribute("unique-identifier");
        if (uniqueIdentAttr != null && !uniqueIdentAttr.equals(""))
        {
          uniqueIdent = uniqueIdentAttr;
        }
        else
        {
          report.message(MessageId.OPF_048, location());
        }
      }
      else if (name.equals("item"))
      {
        String id = e.getAttribute("id");
        String href = e.getAttribute("href");
        if (id != null && href != null) // checked by schema
        {
          URL url = checkURL(href);
          if (url != null)
          {
            if (context.isRemote(url))
            {
              report.info(path, FeatureEnum.REFERENCE, href);
            }
            String mimeType = e.getAttribute("media-type");
            String fallback = e.getAttribute("fallback");
            String fallbackStyle = e.getAttribute("fallback-style");

            OPFItem.Builder itemBuilder = new OPFItem.Builder().id(id).url(url).location(location())
                .container(context.container).remote(context.isRemote(url)).mimetype(mimeType)
                .fallback(fallback).fallbackStyle(fallbackStyle);

            itemBuilders.put(id.trim(), itemBuilder);
            itemBuildersByURL.put(url, itemBuilder);

            String mediaOverlay = e.getAttribute("media-overlay");
            itemBuilder.mediaOverlay(mediaOverlay);
          }
        }
      }
      else if (name.equals("reference"))
      {
        String type = e.getAttribute("type");
        String title = e.getAttribute("title");
        String href = e.getAttribute("href");
        URL url = checkURL(href);
        if (url != null)
        {
          if (context.isRemote(url))
          {
            report.info(path, FeatureEnum.REFERENCE, href);
          }

          registerReference(url, Reference.Type.GENERIC);
        }
        OPFReference ref = new OPFReference(type, title, url, location().getLine(),
            location().getColumn());
        refs.add(ref);

      }
      else if (name.equals("spine"))
      {
        String pageMap = e.getAttribute("page-map");
        if (pageMap != null)
        {
          pageMapId = pageMap;
          pageMapReferenceLocation = location()
              .context(String.format("page-map=\"%1$s\"", pageMapId));
          report.message(MessageId.OPF_062, pageMapReferenceLocation);
        }

        String idref = e.getAttribute("toc");
        if (idref != null)
        {
          if (itemBuilders.containsKey(idref.trim()))
          {
            OPFItem.Builder toc = itemBuilders.get(idref.trim());
            toc.ncx();
          }
          else
          {
            report.message(MessageId.OPF_049, location(), idref);
            report.info(null, FeatureEnum.HAS_NCX, "false");
          }
        }
        else
        {
          report.info(null, FeatureEnum.HAS_NCX, "false");
        }
      }
      else if (name.equals("itemref"))
      {
        String idref = e.getAttribute("idref");
        if (idref != null)
        {
          if (itemBuilders.containsKey(idref.trim()))
          {
            spineIDs.add(idref.trim());
            OPFItem.Builder item = itemBuilders.get(idref.trim());
            if (item != null)
            {
              item.inSpine(spineItemCounter++);
              String linear = e.getAttribute("linear");
              if (linear != null && "no".equals(linear.trim()))
              {
                item.nonlinear();
              }
            }
          }
          else
          {
            report.message(MessageId.OPF_049, location(), idref);
          }
        }
      }
      else if (name.equals("dc-metadata") || name.equals("x-metadata"))
      {
        if (!opf12PackageFile)
        {
          report.message(MessageId.OPF_049, location(), name);
        }
      }
    }
    else if (ns.equals("http://purl.org/dc/elements/1.1/"))
    {
      // in the DC metadata, when the <identifier> element is parsed, if
      // it has a non-null and non-empty id attribute value that is the
      // same as the value of the unique-identifier attribute of the
      // package element, set uniqueIdentExists = true (to make sure that
      // the unique-identifier attribute references an existing
      // <identifier> id attribute
      String name = e.getName();
      if (name.equals("identifier"))
      {
        String idAttr = e.getAttribute("id");
        if (idAttr != null && !idAttr.equals("") && idAttr.equals(uniqueIdent))
        {
          uniqueIdentExists = true;
        }
      }
      else if (name.equals("creator"))
      {
        String role = e.getAttributeNS("http://www.idpf.org/2007/opf", "role");
        if (role != null && !role.equals("") && !isValidRole(role))
        {
          report.message(MessageId.OPF_052, location(), role);
        }
      }
    }
  }

  public String getIdentId()
  {
    return uniqueIdent;
  }

  @Override
  public void endElement()
  {
    XMLElement e = currentElement();
    String ns = e.getNamespace();
    if (ns == null || ns.equals("") || ns.equals("http://openebook.org/namespaces/oeb-package/1.0/")
        || ns.equals("http://www.idpf.org/2007/opf"))
    {
      String name = e.getName();
      // <meta property="dcterms:modified">2012-02-27T16:38:35Z</meta>
      // <meta property="rendition:layout">pre-paginated</meta>
      if ("meta".equals(name))
      {
        String attr = e.getAttribute("property");
        if ("dcterms:modified".equals(attr))
        {
          String val = (String) e.getPrivateData(TEXT);
          report.info(null, FeatureEnum.MODIFIED_DATE, val);
        }
      }
      else if ("package".equals(name))
      {
        buildItems();
        if (pageMapId != null && !getItemById(pageMapId).isPresent())
        {
          report.message(MessageId.OPF_063, pageMapReferenceLocation, pageMapId);
        }
      }
    }
    else if (e.getNamespace().equals("http://purl.org/dc/elements/1.1/"))
    {
      String name = e.getName();
      if (name.equals("identifier"))
      {
        String idAttr = e.getAttribute("id");
        if (idAttr != null && !idAttr.equals("") && idAttr.trim().equals(uniqueIdent))
        {
          String idval = (String) e.getPrivateData(TEXT);
          // if (idval != null && ocf != null)
          // ocf.setUniqueIdentifier(idval);
          if (idval != null)
          {

            uid = idval.trim();

            report.info(null, FeatureEnum.UNIQUE_IDENT, uid);
            context.featureReport.report(FeatureEnum.UNIQUE_IDENT, location(), uid);

            // #853
            String opfSchemeAttr = e.getAttributeNS("http://www.idpf.org/2007/opf", "scheme");
            if (uid.startsWith("urn:uuid:")
                || (opfSchemeAttr != null && opfSchemeAttr.toLowerCase().equals("uuid")))
            {
              try
              {
                UUID.fromString(uid.replaceAll("urn:uuid:", ""));
              } catch (Throwable t)
              {
                report.message(MessageId.OPF_085, location(), uid);
              }
            }
          }
        }
      }
      else if (name.equals("date"))
      {
        String dateval = (String) e.getPrivateData(TEXT);
        boolean valid = true;
        String detail = null;

        if (dateval == null || "".equals(dateval))
        {
          valid = false;
          detail = "zero-length string";
        }
        else
        {
          DateParser dateParser = new DateParser();
          try
          {
            Date date = dateParser.parse(dateval.trim());
            /*
             * mg: DateParser does not enforce four-digit years, which
             * http://www.w3.org/TR/NOTE-datetime seems to want
             */
            String year = new SimpleDateFormat("yyyy").format(date);
            if (year.length() > 4)
            {
              throw new InvalidDateException(year);
            }
            report.info(null, FeatureEnum.DC_DATE, dateval);
          } catch (InvalidDateException d)
          {
            valid = false;
            detail = d.getMessage();
          }
        }

        if (!valid)
        {
          if (context.version == EPUBVersion.VERSION_3)
          {
            report.message(MessageId.OPF_053, location(), (dateval == null ? "" : dateval), detail);
          }
          else
          {
            report.message(MessageId.OPF_054, location(), (dateval == null ? "" : dateval), detail);
          }
        }
      }
      else if (name.equals("title") || name.equals("language"))
      {
        // issue 138: issue a warning if dc:title and dc:language is
        // empty for 2.0 and 2.0.1
        // note that an empty dc:identifier is checked in opf20.rng and
        // will
        // therefore be reported as an error, that may or may not be a
        // good idea.
        if ("language".equals(name))
        {
          String value = (String) e.getPrivateData(TEXT);
          if (value != null)
          {
            report.info(null, FeatureEnum.DC_LANGUAGE, value.trim());
          }
        }
        else if ("title".equals(name))
        {
          String value = (String) e.getPrivateData(TEXT);
          if (value != null)
          {
            report.info(null, FeatureEnum.DC_TITLE, value.trim());
          }
        }
        if (context.version == EPUBVersion.VERSION_2)
        {
          String value = (String) e.getPrivateData(TEXT);
          if (value == null || value.trim().length() < 1)
          {
            report.message(MessageId.OPF_055, location(), name);
          }
        }
      }
      else
      {

        Optional<String> value = Optional.fromNullable(
            Strings.emptyToNull(Strings.nullToEmpty((String) e.getPrivateData(TEXT)).trim()));

        // Check for empty metadta (USAGE) in EPUB 2
        // Empty metadata is forbidden and checked with schema in EPUB 3
        if (context.version == EPUBVersion.VERSION_2 && !value.isPresent())
        {
          report.message(MessageId.OPF_072, location(), "dc:" + name);
        }
        // Report metadata
        else if (value.isPresent())
        {
          if (name.equals("creator"))
          {
            report.info(null, FeatureEnum.DC_CREATOR, value.get());
          }
          else if (name.equals("contributor"))
          {
            report.info(null, FeatureEnum.DC_CONTRIBUTOR, value.get());
          }
          else if (name.equals("publisher"))
          {
            report.info(null, FeatureEnum.DC_PUBLISHER, value.get());
          }
          else if (name.equals("rights"))
          {
            report.info(null, FeatureEnum.DC_RIGHTS, value.get());
          }
          else if (name.equals("subject"))
          {
            report.info(null, FeatureEnum.DC_SUBJECT, value.get());
          }
          else if (name.equals("description"))
          {
            report.info(null, FeatureEnum.DC_DESCRIPTION, value.get());
          }
        }
      }
    }
  }

  @Override
  public void characters(char[] chars, int start, int len)
  {

    XMLElement e = currentElement();
    String name = e.getName();
    String ns = e.getNamespace();
    boolean keepValue = ("http://www.idpf.org/2007/opf".equals(ns) && "meta".equals(name))
        || "http://purl.org/dc/elements/1.1/".equals(ns);
    if (keepValue)
    {
      String val = (String) e.getPrivateData(TEXT);
      String text = new String(chars, start, len);
      if (val == null)
      {
        val = text;
      }
      else
      {
        val = val + text;
      }
      e.setPrivateData(TEXT, val);
    }
  }

  // Build the final items from the item builders
  private void buildItems()
  {
    Preconditions.checkState(items == null);

    items = OPFItems.build(itemBuilders, spineIDs, context);

    for (OPFItem item : items.getItems())
    {
      reportItem(item);
    }
  }

  /**
   * Report features or messages for a given item.
   * 
   * @param item
   *        the item to report.
   */
  protected void reportItem(OPFItem item)
  {
    report.info(item.getPath(), FeatureEnum.UNIQUE_IDENT, item.getId());
    if (item.isInSpine())
    {
      report.info(item.getPath(), FeatureEnum.IS_SPINEITEM, "true");
      report.info(item.getPath(), FeatureEnum.IS_LINEAR, String.valueOf(item.isLinear()));
    }
    if (item.isNcx())
    {
      report.info(item.getPath(), FeatureEnum.HAS_NCX, "true");
      if (!item.getMimeType().equals("application/x-dtbncx+xml"))
      {
        report.message(MessageId.OPF_050, item.getLocation());
      }
    }
  }
}
