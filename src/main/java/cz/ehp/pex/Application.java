package cz.ehp.pex;

import cz.ehp.pex.events.ImageDataEvent;
import cz.ehp.pex.events.ImageResultEvent;
import cz.ehp.pex.events.UrlEvent;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final int COLORS = 3;

    private final CloseableHttpClient httpclient;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Run again with input and output file as arguments.");
            return;
        }

        final Path inputFile = Paths.get(args[0]);
        final Path outputFile = Paths.get(args[1]);

        new Application(createHttpClient()).run(inputFile, outputFile);
    }

    private static CloseableHttpClient createHttpClient() {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);

        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    public Application(final CloseableHttpClient httpclient) {
        this.httpclient = httpclient;
    }

    public void run(final Path inputFile, final Path outputFile) {
        try (final BufferedReader br = Files.newBufferedReader(inputFile);
             final BufferedWriter bw = Files.newBufferedWriter(outputFile)) {
            createReader(br)
                    .parallel()
                    .runOn(Schedulers.parallel())
                    .map(this::loadImage)
                    .filter(ImageDataEvent::isNotEmpty)
                    .map(this::processImage)
                    .map(this::convertEvent)
                    .sequential()
                    .publishOn(Schedulers.single())
                    .doOnNext(line -> {
                        try {
                            bw.write(line);
                            bw.flush();
                        } catch (final IOException e) {
                            throw Exceptions.propagate(e);
                        }
                    })
                    .doOnError(e -> {
                        log.error("General exception", e);
                    })
                    .blockLast();
        } catch (final IOException e) {
            log.error("File IO exception", e);
        } finally {
            try {
                httpclient.close();
            } catch (final IOException e) {
                // ignore this exception
                log.warn("HTTP client close exception", e);
            }
        }
    }

    protected Flux<UrlEvent> createReader(final BufferedReader br) {
        return Flux.generate(new Consumer<SynchronousSink<UrlEvent>>() {
            @Override
            public void accept(final SynchronousSink<UrlEvent> sink) {
                try {
                    final String line = br.readLine();
                    if (line == null) {
                        log.debug("No more input data");
                        sink.complete();
                    } else {
                        log.debug("Input line: {}", line);
                        sink.next(new UrlEvent(line));
                    }
                } catch (final IOException e) {
                    sink.error(e);
                }
            }
        });
    }

    protected ImageDataEvent loadImage(final UrlEvent event) {
        final String url = event.getUrl();
        log.info("Downloading {}", url);
        final HttpGet httpget = new HttpGet(url);
        try {
            return httpclient.execute(httpget, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    final HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        try (final BufferedInputStream bis = new BufferedInputStream(entity.getContent())) {
                            final BufferedImage image = ImageIO.read(bis);
                            return new ImageDataEvent(url, image);
                        }
                    } else {
                        log.error("Empty response from url {}", url);
                    }
                } else {
                    log.error("Unexpected response status {} for url {}", status, url);
                }
                return new ImageDataEvent(url, null);
            });
        } catch (final IOException e) {
            throw Exceptions.propagate(e);
        }

    }

    protected ImageResultEvent processImage(final ImageDataEvent data) {
        final Map<Integer, Integer> colors = new HashMap<>();
        final BufferedImage image = data.getImage();
        log.debug("Image at url {} has size {}x{}", data.getUrl(), image.getWidth(), image.getHeight());

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                colors.compute(image.getRGB(x + image.getMinX(), y + image.getMinY()) & 0xffffff,
                        (k, v) -> v == null ? 1 : v + 1);
            }
        }

        final TopKBucket<Map.Entry<Integer, Integer>> topk = new TopKBucket<>(COLORS, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(final Map.Entry<Integer, Integer> o1, final Map.Entry<Integer, Integer> o2) {
                return -o1.getValue().compareTo(o2.getValue());
            }
        });
        colors.entrySet().forEach(topk::add);

        return new ImageResultEvent(data.getUrl(),
                topk.getBuffer().stream().mapToInt(Map.Entry::getKey).toArray());
    }

    protected String convertEvent(final ImageResultEvent event) {
        log.debug("Converting event for url {}", event.getUrl());
        final StringBuilder sb = new StringBuilder();
        sb.append(event.getUrl() + ",");
        sb.append(Arrays.stream(event.getColors())
                .mapToObj(x -> String.format("%06X", x))
                .collect(Collectors.joining(",")));
        sb.append("\n");

        return sb.toString();
    }
}
