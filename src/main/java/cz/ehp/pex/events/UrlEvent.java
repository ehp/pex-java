package cz.ehp.pex.events;

import java.util.Objects;

/**
 * Event class - new image url for processing
 */
public class UrlEvent {
    private final String url;

    public UrlEvent(final String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlEvent urlEvent = (UrlEvent) o;
        return Objects.equals(url, urlEvent.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "UrlEvent{" +
                "url='" + url + '\'' +
                '}';
    }
}
