package cz.ehp.pex.events;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Event class - result from image processing
 */
public class ImageResultEvent extends UrlEvent {
    private final int[] colors;

    public ImageResultEvent(final String url, final int[] colors) {
        super(url);
        this.colors = colors;
    }

    public int[] getColors() {
        return colors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ImageResultEvent that = (ImageResultEvent) o;
        return Arrays.equals(colors, that.colors);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(colors);
        return result;
    }

    @Override
    public String toString() {
        return "ImageResultEvent{" +
                "url='" + getUrl() + '\'' +
                "colors=" + Arrays.stream(colors)
                .mapToObj(x -> String.format("%06X", x))
                .collect(Collectors.joining(",")) +
                '}';
    }
}
