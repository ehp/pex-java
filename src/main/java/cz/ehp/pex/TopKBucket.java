package cz.ehp.pex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Generic class for sorting only first n items and dropping the rest.
 */
public class TopKBucket<T> {
    private static final Logger log = LoggerFactory.getLogger(TopKBucket.class);

    private LinkedList<T> buffer;
    private final int capacity;
    private final Comparator<T> comparator;

    /**
     *
     * @param capacity Number of final items
     * @param comparator Custom comparator
     */
    public TopKBucket(final int capacity, final Comparator<T> comparator) {
        this.capacity = capacity;
        this.comparator = comparator;
        this.buffer = new LinkedList<>();
    }

    /**
     *
     * @return Sorted items list
     */
    public List<T> getBuffer() {
        return Collections.unmodifiableList(buffer);
    }

    /**
     *
     * @param newItem Add new item to sorted list
     */
    public void add(final T newItem) {
        log.debug("New item {} to insert into buffer", newItem);

        // handle special case when newItem is lower than last item
        if (buffer.size() >= capacity) {
            final T lastItem = buffer.getLast();
            final int cmpLast = comparator.compare(lastItem, newItem);
            if (cmpLast < 0) {
                log.debug("New item {} is lower than last item {}", newItem, lastItem);
                return;
            }
        }

        // handle special case when buffer is empty
        if (buffer.size() == 0) {
            log.debug("New item {} inserted into empty buffer", newItem);
            buffer.add(newItem);
            return;
        }

        // inserted new item into correct place between existing items
        final LinkedList<T> newBuffer = new LinkedList<>();
        int count = 0;
        boolean inserted = false;
        for (final T item : buffer) {
            if (count++ >= capacity) {
                break;
            }

            if (!inserted) {
                final int cmp = comparator.compare(item, newItem);
                if (cmp >= 0) {
                    log.debug("New item {} inserted at position {}", newItem, count);
                    newBuffer.add(newItem);
                    count++;
                    inserted = true;
                }
            }

            if (count <= capacity) {
                log.debug("Old item {} inserted at position {}", item, count);
                newBuffer.add(item);
            }
        }

        // handle special case when capacity is not fully filled
        if (!inserted && ++count <= capacity) {
            log.debug("New item {} inserted as last item into buffer", newItem);
            newBuffer.add(newItem);
        }

        buffer = newBuffer;
    }
}
