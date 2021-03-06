package com.dabeeb.miner.net.urlfilter;

@SuppressWarnings("serial")
public class URLFilterException extends Exception {

  public URLFilterException() {
    super();
  }

  public URLFilterException(String message) {
    super(message);
  }

  public URLFilterException(String message, Throwable cause) {
    super(message, cause);
  }

  public URLFilterException(Throwable cause) {
    super(cause);
  }

}
