package co.ecg.jain_sip.sip.ri.message;

import java.util.Iterator;

import co.ecg.jain_sip.sip.header.ContentDispositionHeader;
import co.ecg.jain_sip.sip.header.ContentTypeHeader;
import co.ecg.jain_sip.sip.header.Header;

public interface Content {

  public abstract void setContent(Object content);

  public abstract Object getContent();

  public abstract ContentTypeHeader getContentTypeHeader();

  public abstract ContentDispositionHeader getContentDispositionHeader();

  public abstract Iterator<Header> getExtensionHeaders();
  
  /**
   * The default packing method. This packs the content to be appended to the
   * sip message.
   * 
   */
  public abstract String toString();


}
