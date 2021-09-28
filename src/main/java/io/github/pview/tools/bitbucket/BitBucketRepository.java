package io.github.pview.tools.bitbucket;

import picocli.CommandLine;

import java.net.URI;
import java.util.regex.Pattern;

public class BitBucketRepository {
    private static final URI bitBucketBaseURI = URI.create("https://bitbucket.org");
    private final String owner;
    private final String name;

    private final URI uri;

    public BitBucketRepository(String owner, String name) {
        if (owner.contains("/") || name.contains("/")) {
            throw new IllegalArgumentException("Repository cannot contain a / character");
        }
        this.owner = owner;
        this.name = name;

        uri = bitBucketBaseURI.resolve(toString());
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BitBucketRepository that = (BitBucketRepository) o;

        if (!getOwner().equals(that.getOwner())) return false;
        if (!getName().equals(that.getName())) return false;
        return getUri().equals(that.getUri());
    }

    @Override
    public int hashCode() {
        int result = getOwner().hashCode();
        result = 31 * result + getName().hashCode();
        result = 31 * result + getUri().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getOwner() + "/" + getName();
    }

    public static final class Converter implements CommandLine.ITypeConverter<BitBucketRepository> {
        private final Pattern pattern = Pattern.compile("https://bitbucket\\.org/(.*)/(.*)");


        @Override
        public BitBucketRepository convert(String value) throws Exception {
            final var m = pattern.matcher(value);

            return new BitBucketRepository(m.group(1), m.group(2));
        }
    }
}
