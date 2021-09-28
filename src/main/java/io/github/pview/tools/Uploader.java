package io.github.pview.tools;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;

import java.io.IOException;
import java.net.URI;

import java.nio.file.Path;

public class Uploader {
    private Uploader() {}

    public static HttpResponse upload(HttpClient c, Path file, URI uri) throws IOException {
        HttpEntity fileEntity = MultipartEntityBuilder.create()
                .addBinaryBody("files", file.toFile(), ContentType.MULTIPART_FORM_DATA, file.getFileName().toString())
                .build();

        HttpPost post = new HttpPost(uri);
        post.setEntity(fileEntity);

        return c.execute(post);
    }
}
