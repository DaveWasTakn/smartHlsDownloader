package org.davesEnterprise.download;

import java.util.Collection;
import java.util.Collections;

public class Segment {

    private final String url;
    private Float duration;
    private Collection<Segment> alternatives = Collections.emptyList();

    public Segment(String url) {
        this.url = url;
    }

    public Segment(String url, float duration, Collection<Segment> alternatives) {
        this.url = url;
        this.duration = duration;
        this.alternatives = alternatives;
    }

}
