package cz.ehp.pex.events;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Event class - loaded image for further processing
 */
public class ImageDataEvent extends UrlEvent {
    private final BufferedImage image;

    public ImageDataEvent(final String url, final BufferedImage image) {
        super(url);
        this.image = image;
    }

    public BufferedImage getImage() {
        return image;
    }

    public boolean isNotEmpty() {
        return image != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ImageDataEvent that = (ImageDataEvent) o;
        return image.equals(that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), image);
    }

    @Override
    public String toString() {
        return "ImageDataEvent{" +
                "url='" + getUrl() + '\'' +
                '}';
    }
}
