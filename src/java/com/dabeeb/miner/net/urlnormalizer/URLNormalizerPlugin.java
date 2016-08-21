package com.dabeeb.miner.net.urlnormalizer;

import java.net.MalformedURLException;

import com.dabeeb.miner.plugin.Plugin;

/** Interface used to convert URLs to normal form and optionally perform substitutions */
public interface URLNormalizerPlugin extends Plugin {
  
  /* Interface for URL normalization */
  public String normalize(String urlString, String scope) throws MalformedURLException;

}
