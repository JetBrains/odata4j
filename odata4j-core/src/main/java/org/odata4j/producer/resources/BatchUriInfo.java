package org.odata4j.producer.resources;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

public class BatchUriInfo implements UriInfo {

    private final URI requestUri;
    private final UriInfo baseInfo;

    public BatchUriInfo(final URI requestUri, final UriInfo baseInfo) {
        this.requestUri = requestUri;
        this.baseInfo = baseInfo;
    }

    @Override
    public String getPath() {
        return getPath(true);
    }

    @Override
    public String getPath(boolean decode) {
        return decode ? this.requestUri.getPath() : this.requestUri.getRawPath();
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return null;
    }

    @Override
    public List<PathSegment> getPathSegments(boolean b) {
        return null;
    }

    @Override
    public URI getRequestUri() {
        return this.requestUri;
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return this.baseInfo.getRequestUriBuilder().uri(this.requestUri);
    }

    @Override
    public URI getAbsolutePath() {
        return null;
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return null;
    }

    @Override
    public URI getBaseUri() {
        return this.baseInfo.getBaseUri();
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return this.baseInfo.getBaseUriBuilder();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean b) {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return getQueryParameters(true);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return splitQuery(decode ? this.requestUri.getQuery() : this.requestUri.getRawQuery());
    }

    @Override
    public List<String> getMatchedURIs() {
        return null;
    }

    @Override
    public List<String> getMatchedURIs(boolean b) {
        return null;
    }

    @Override
    public List<Object> getMatchedResources() {
        return null;
    }

    private static HeaderMap splitQuery(String query) {
        final HeaderMap map = new HeaderMap();
        if (query == null || query.length() == 0) {
            return map;
        }

        for (String param : query.split("&")) {
            String[] values = param.split("=");
            if (values.length != 2) continue;
            map.add(values[0], values[1]);
        }

        return map;
    }
}
