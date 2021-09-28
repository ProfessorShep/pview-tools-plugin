package io.github.pview.tools.bitbucket;

import io.github.pview.tools.Uploader;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class BitBucketDownloadUploader implements AutoCloseable {
    private static final String API_ENDPOINT_FORMAT_STRING = "https://api.bitbucket.org/2.0/repositories/%s/%s/downloads";
    private final CloseableHttpClient client;

    private final URI apiEndpoint;


    public BitBucketDownloadUploader(String user, char[] pass, BitBucketRepository repo) {
        final var auth = new BasicCredentialsProvider();
        auth.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(user, pass));

        client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(auth)
                .build();

        this.apiEndpoint = URI.create(String.format(API_ENDPOINT_FORMAT_STRING, repo.getOwner(), repo.getName()));
    }

    public void upload(Path path) throws IOException {
        Uploader.upload(client, path, apiEndpoint);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
