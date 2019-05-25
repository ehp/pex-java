package cz.ehp.pex;

import cz.ehp.pex.events.ImageDataEvent;
import cz.ehp.pex.events.ImageResultEvent;
import cz.ehp.pex.events.UrlEvent;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


public class ApplicationTest {
    private CloseableHttpClient httpclient;

    private Application app;

    @BeforeMethod
    public void setUp() {
        httpclient = mock(CloseableHttpClient.class);
        app = new Application(httpclient);
    }

    @Test
    public void testMainLoop() throws IOException {
        final Path inputFile = Files.createTempFile("pex-", ".txt");
        final Path outputFile = Files.createTempFile("pex-", ".csv");

        Files.write(inputFile, asList("http://test1.url", "http://test2.url", "http://test3.url"));

        when(httpclient.execute(any(), any(ResponseHandler.class))).then(new Answer() {
            public Object answer(final InvocationOnMock invocation) throws IOException, URISyntaxException {
                final Object[] args = invocation.getArguments();
                final ResponseHandler<? extends ImageDataEvent> handler = ((ResponseHandler<? extends ImageDataEvent>)args[1]);
                final HttpResponse response = mock(HttpResponse.class);
                final StatusLine statusLine = mock(StatusLine.class);
                final HttpEntity entity = mock(HttpEntity.class);

                when(response.getStatusLine()).thenReturn(statusLine);
                when(statusLine.getStatusCode()).thenReturn(200);
                when(response.getEntity()).thenReturn(entity);
                when(entity.getContent()).thenReturn(Files.newInputStream(getImagePath("FApqk3D.jpg")));

                return handler.handleResponse(response);
            }
        });

        try {
            app.run(inputFile, outputFile);

            verify(httpclient, times(3)).execute(any(HttpGet.class), any(ResponseHandler.class));
            verify(httpclient).close();
            verifyNoMoreInteractions(httpclient);

            final List<String> result = Files.readAllLines(outputFile);
            assertEquals(result.size(), 3);
            for (final String line : result) {
                final String[] values = line.split(",");
                assertEquals(values.length, 4);
                assertTrue(values[0].matches("http://test\\d\\.url"));
                assertEquals(values[1], "FFFFFF");
                assertEquals(values[2], "000000");
                assertEquals(values[3], "F3C300");
            }
        } finally {
            Files.deleteIfExists(inputFile);
            Files.deleteIfExists(outputFile);
        }
    }

    @Test
    public void testCreateReader() {
        final List<String> urlList = asList("http://i.imgur.com/FApqk3D.jpg",
                "http://i.imgur.com/TKLs9lo.jpg",
                "https://i.redd.it/d8021b5i2moy.jpg",
                "https://i.redd.it/4m5yk8gjrtzy.jpg",
                "https://i.redd.it/xae65ypfqycy.jpg",
                "http://i.imgur.com/lcEUZHv.jpg",
                "https://i.redd.it/lsuw4p2ncyny.jpg");

        final BufferedReader br = new BufferedReader(new StringReader(String.join("\n", urlList)));
        final Flux<UrlEvent> reader = app.createReader(br);

        StepVerifier.create(reader)
                .expectNextSequence(urlList.stream().map(UrlEvent::new).collect(Collectors.toList()))
                .expectComplete()
                .verify();
    }

    @Test(dataProvider = "loadImage")
    public void testLoadImage(final String url, final String imageName, final ImageDataEvent event) throws IOException {
        when(httpclient.execute(any(), any(ResponseHandler.class))).then(new Answer() {
            public Object answer(final InvocationOnMock invocation) throws IOException, URISyntaxException {
                final Object[] args = invocation.getArguments();
                final ResponseHandler<? extends ImageDataEvent> handler = ((ResponseHandler<? extends ImageDataEvent>)args[1]);
                final HttpResponse response = mock(HttpResponse.class);
                final StatusLine statusLine = mock(StatusLine.class);
                final HttpEntity entity = mock(HttpEntity.class);

                when(response.getStatusLine()).thenReturn(statusLine);
                when(statusLine.getStatusCode()).thenReturn(200);
                when(response.getEntity()).thenReturn(entity);
                when(entity.getContent()).thenReturn(Files.newInputStream(getImagePath(imageName)));

                return handler.handleResponse(response);
            }
        });

        final ImageDataEvent actual = app.loadImage(new UrlEvent(url));

        verify(httpclient).execute(any(HttpGet.class), any(ResponseHandler.class));
        verifyNoMoreInteractions(httpclient);

        assertEquals(actual.getUrl(), event.getUrl());
        assertTrue(compareImages(actual.getImage(), event.getImage()));
    }

    @Test(dataProvider = "loadImage")
    public void testLoadEmptyImage(final String url, final String imageName, final ImageDataEvent event) throws IOException {
        when(httpclient.execute(any(), any(ResponseHandler.class))).then(new Answer() {
            public Object answer(final InvocationOnMock invocation) throws IOException {
                final Object[] args = invocation.getArguments();
                final ResponseHandler<? extends ImageDataEvent> handler = ((ResponseHandler<? extends ImageDataEvent>)args[1]);
                final HttpResponse response = mock(HttpResponse.class);
                final StatusLine statusLine = mock(StatusLine.class);

                when(response.getStatusLine()).thenReturn(statusLine);
                when(statusLine.getStatusCode()).thenReturn(200);
                when(response.getEntity()).thenReturn(null);

                return handler.handleResponse(response);
            }
        });

        final ImageDataEvent actual = app.loadImage(new UrlEvent(url));

        verify(httpclient).execute(any(HttpGet.class), any(ResponseHandler.class));
        verifyNoMoreInteractions(httpclient);

        assertEquals(actual.getUrl(), event.getUrl());
        assertNull(actual.getImage());
    }

    @Test(dataProvider = "loadImage")
    public void testLoadErrorImage(final String url, final String imageName, final ImageDataEvent event) throws IOException {
        when(httpclient.execute(any(), any(ResponseHandler.class))).then(new Answer() {
            public Object answer(final InvocationOnMock invocation) throws IOException {
                final Object[] args = invocation.getArguments();
                final ResponseHandler<? extends ImageDataEvent> handler = ((ResponseHandler<? extends ImageDataEvent>)args[1]);
                final HttpResponse response = mock(HttpResponse.class);
                final StatusLine statusLine = mock(StatusLine.class);

                when(response.getStatusLine()).thenReturn(statusLine);
                when(statusLine.getStatusCode()).thenReturn(500);

                return handler.handleResponse(response);
            }
        });

        final ImageDataEvent actual = app.loadImage(new UrlEvent(url));

        verify(httpclient).execute(any(HttpGet.class), any(ResponseHandler.class));
        verifyNoMoreInteractions(httpclient);

        assertEquals(actual.getUrl(), event.getUrl());
        assertNull(actual.getImage());
    }

    @DataProvider(name = "loadImage")
    public Object[][] loadImageProvider() throws IOException, URISyntaxException {
        return new Object[][]{
                {"http://test.url", "FApqk3D.jpg", new ImageDataEvent("http://test.url", loadImage("FApqk3D.jpg"))},
                {"http://other.url", "ihczg3pmle3z.jpg", new ImageDataEvent("http://other.url", loadImage("ihczg3pmle3z.jpg"))}
        };
    }

    @Test(dataProvider = "processImage")
    public void testProcessImage(final ImageDataEvent data, final ImageResultEvent result) {
        assertEquals(app.processImage(data), result);
    }

    @DataProvider(name = "processImage")
    public Object[][] processImageProvider() throws IOException, URISyntaxException {
        return new Object[][]{
                {new ImageDataEvent("http://test.url", loadImage("FApqk3D.jpg")), new ImageResultEvent("http://test.url", new int[]{0xFFFFFF, 0, 0xF3C300})},
                {new ImageDataEvent("http://other.url", loadImage("ihczg3pmle3z.jpg")), new ImageResultEvent("http://other.url", new int[]{0xB0B9A8, 0xA3AC9B, 0xAFB8A7})}
        };
    }

    @Test(dataProvider = "convertEvent")
    public void testConvertEvent(final ImageResultEvent event, final String line) {
        assertEquals(app.convertEvent(event), line);
    }

    @DataProvider(name = "convertEvent")
    public Object[][] convertEventProvider() {
        return new Object[][]{
                {new ImageResultEvent("http://test.url", new int[]{1, 2, 3}), "http://test.url,000001,000002,000003\n"},
                {new ImageResultEvent("http://other.url", new int[]{0xffffff, 0xff0000, 0xff}), "http://other.url,FFFFFF,FF0000,0000FF\n"}
        };
    }

    private Path getImagePath(final String name) throws URISyntaxException {
        return Paths.get(getClass().getResource("/images").toURI()).resolve(name);
    }

    private BufferedImage loadImage(final String name) throws IOException, URISyntaxException {
        final Path path = getImagePath(name);

        try (final BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path))) {
            return ImageIO.read(bis);
        }
    }

    private boolean compareImages(final BufferedImage a, final BufferedImage b) {
        // compare sizes
        final int width = a.getWidth();
        final int height = a.getHeight();
        if (width != b.getWidth() || height != b.getHeight()) {
            return false;
        }

        // compare pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (a.getRGB(x + a.getMinX(), y + a.getMinY()) != b.getRGB(x + b.getMinX(), y + b.getMinY())) {
                    return false;
                }
            }
        }

        return true;
    }
}
