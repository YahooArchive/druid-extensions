package com.yahoo.druid.brokerui;

import org.apache.tika.Tika;

public class TikaContentTypeDetector implements ContentTypeDetector
{
  private final Tika tika = new Tika();

  @Override
  public String detect(String path)
  {
    return tika.detect(path);
  }
}