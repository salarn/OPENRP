package com.example.salar.openrp;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Created by salar on 8/4/17.
 */

@JsonObject
public class Request {

    @JsonField
    public String requestTitle;

    @JsonField
    public String requestDetail;

    @JsonField
    public String requestPeerId;

    @JsonField
    public long requestStartTime;
}

