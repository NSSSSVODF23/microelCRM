package com.microel.trackerbackend.misc.accounting;

import javax.servlet.http.HttpServletResponse;

public interface TDocument {
    String getName();
    String getMimeType();
    byte[] getContent();
    long getSize();
    void sendByResponse(HttpServletResponse response);
}
