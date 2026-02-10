/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.TripModificationsFormat;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.GtfsTripModificationsFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class GtfsTripModificationsFetcherImpl implements GtfsTripModificationsFetcher {

    private static final Logger _log = LoggerFactory.getLogger(GtfsTripModificationsFetcher.class);

    private final URI _tripModificationsUri;
    private HttpClient _httpClient;
    private final boolean _isFile;
    private final Path _filePath;
    private final TripModificationsFormat _tripModificationsFormat;

    public GtfsTripModificationsFetcherImpl(String gtfsTripModificationsUrl, HttpClient httpClient) throws URISyntaxException {
        this(gtfsTripModificationsUrl);
        _httpClient = httpClient;
    }

    public GtfsTripModificationsFetcherImpl(String gtfsTripModificationsUrl) throws URISyntaxException {
        _tripModificationsUri = parseURI(gtfsTripModificationsUrl);
        String scheme = _tripModificationsUri.getScheme();
        _isFile = scheme == null || "file".equalsIgnoreCase(scheme);
        _filePath = _isFile ? toFilePath(_tripModificationsUri, gtfsTripModificationsUrl) : null;
        _tripModificationsFormat = determineTripModificationsFormat(_tripModificationsUri);
        _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    private URI parseURI(String value) throws URISyntaxException {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("gtfsTripModificationsUrl must not be null/blank");
        }

        if (value.contains("://")) {
            return new URI(value);
        }

        // Defaults to file path
        return Path.of(value).toUri();
    }

    private Path toFilePath(URI uri, String originalValue) {
        // Handles explicit file paths
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return Path.of(uri);
        }

        // If no scheme then defaults to file path
        if (uri.getScheme() == null) {
            return Path.of(originalValue);
        }

        throw new IllegalArgumentException("Unsupported URI scheme for file access: " + uri.getScheme());
    }

    private TripModificationsFormat determineTripModificationsFormat(URI tripModificationsUri) {
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(tripModificationsUri)
                .build()
                .getQueryParams();

        return "json".equalsIgnoreCase(params.getFirst("format"))
                ? TripModificationsFormat.JSON
                : TripModificationsFormat.PROTOBUF;
    }

    @Override
    public TripModificationsFormat getTripModificationsFormat() {
        return _tripModificationsFormat;
    }

    @Override
    public byte[] fetchFeed() throws IOException, InterruptedException {
        _log.info("Fetching GTFS Trip Modifications from {}", _tripModificationsUri);

        if (_isFile) {
            return fetchFromFile();
        } else {
            return fetchFromHttp();
        }
    }

    private byte[] fetchFromHttp() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(_tripModificationsUri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<byte[]> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP error: " + response.statusCode());
        }

        byte[] data = response.body();
        _log.debug("Downloaded {} bytes", data.length);

        return data;
    }

    private byte[] fetchFromFile() throws IOException {
        return Files.readAllBytes(_filePath);
    }
}
