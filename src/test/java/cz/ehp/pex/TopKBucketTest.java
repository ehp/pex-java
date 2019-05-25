package cz.ehp.pex;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;

public class TopKBucketTest {
    private TopKBucket<Integer> topk;

    @BeforeMethod
    public void setUp() {
        topk = new TopKBucket(3, Comparator.naturalOrder());
    }


    @Test
    public void testOrdered() {
        for (int i = 1; i < 10; i++) {
            topk.add(i);
        }

        assertOrder();
    }

    @Test
    public void testReverseOrder() {
        for (int i = 10; i >= 1; i--) {
            topk.add(i);
        }

        assertOrder();
    }

    @Test
    public void testRandomOrder() {
        final List<Integer> randomInts = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
        Collections.shuffle(randomInts);

        for (final Integer i : randomInts) {
            topk.add(i);
        }

        assertOrder();
    }

    private void assertOrder() {
        final List<Integer> buffer = topk.getBuffer();

        assertEquals(buffer.size(), 3);
        assertEquals(buffer.get(0).intValue(), 1);
        assertEquals(buffer.get(1).intValue(), 2);
        assertEquals(buffer.get(2).intValue(), 3);
    }
}
