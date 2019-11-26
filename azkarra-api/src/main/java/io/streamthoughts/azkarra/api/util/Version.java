/*
 * Copyright 2019 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.azkarra.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A version class which supports the following pattern :
 *
 *  <major version>.<minor version>.<incremental version>-<qualifier>
 *
 *  Supported qualifier are : alpha, beta, snapshot, rc, release.
 */
public class Version implements Comparable<Version> {

    /**
     * Static helper for creating a new version based on the specified string.
     *
     * @param version   the version.
     * @return          a new {@link Version} instance.
     */
    public static Version parse(final String version) {

        int qualifier = version.indexOf("-");

        final String[] versions = qualifier > 0 ?
                version.substring(0, qualifier).split("\\.") :
                version.split("\\.");
        try {
            final int majorVersion = Integer.parseInt(versions[0]);
            final int minorVersion = versions.length > 1 ? Integer.parseInt(versions[1]) : 0;
            final int incrementalVersion = versions.length > 2 ? Integer.parseInt(versions[2]) : 0;

            return new Version(
                    majorVersion,
                    minorVersion,
                    incrementalVersion,
                    qualifier > 0 ? version.substring(qualifier + 1) : null
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version, cannot parse '" + version + "'");
        }
    }

    /**
     * Static helper for returning the latest version from a list of {@link Version}.
     *
     * @param versions  the list of version.
     * @return          the latest version.
     */
    public static Version getLatest(final Version...versions) {
        if (versions.length == 0) throw new IllegalArgumentException("empty list");
        return Stream.of(versions).sorted().findFirst().get();
    }

    private final int majorVersion;
    private final int minorVersion;
    private final int incrementalVersion;
    private final Qualifier qualifier;

    /**
     * Creates a new {@link Version} instance.
     *
     * @param majorVersion          the major version (must be superior or equal to 0).
     * @param minorVersion          the minor version (must be superior or equal to 0).
     * @param incrementalVersion    the incremental version (must be superior or equal to 0).
     * @param qualifier             the qualifier.
     */
    public Version(final int majorVersion,
                   final int minorVersion,
                   final int incrementalVersion,
                   final String qualifier) {
        this.majorVersion =  requirePositive(majorVersion, "major");
        this.minorVersion = requirePositive(minorVersion, "minor");
        this.incrementalVersion = requirePositive(incrementalVersion, "incremental");
        this.qualifier = qualifier != null ? new Qualifier(qualifier) : null;
    }

    private static int requirePositive(int version, final String message) {
        if (version < 0) {
            throw new IllegalArgumentException(String.format("The '%s' version must super or equal to 0", message));
        }
        return version;
    }

    public int majorVersion() {
        return majorVersion;
    }

    public int minorVersion() {
        return minorVersion;
    }

    public int incrementalVersion() {
        return incrementalVersion;
    }

    public Qualifier qualifier() {
        return qualifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version)) return false;
        Version version = (Version) o;
        return majorVersion == version.majorVersion &&
                minorVersion == version.minorVersion &&
                incrementalVersion == version.incrementalVersion &&
                Objects.equals(qualifier, version.qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(majorVersion, minorVersion, incrementalVersion, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String version =  majorVersion + "." + minorVersion + "." + incrementalVersion;
        return (qualifier != null) ? version +"-" + qualifier : version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Version that) {

        int compareMajor = Integer.compare(that.majorVersion, this.majorVersion);
        if (compareMajor != 0) {
            return compareMajor;
        }

        int compareMinor = Integer.compare(that.minorVersion, this.minorVersion);
        if (compareMinor != 0) {
            return compareMinor;
        }

        int compareIncremental = Integer.compare(that.incrementalVersion, this.incrementalVersion);
        if (compareIncremental != 0) {
            return compareIncremental;
        }

        if (that.qualifier == null && this.qualifier == null) {
            return 0;
        } else if (that.qualifier == null) {
            return 1;
        } else if (this.qualifier == null) {
            return -1;
        }

        return this.qualifier.compareTo(that.qualifier);
    }

    static final class Qualifier implements Comparable<Qualifier> {

        private static final List<String> DEFAULT_QUALIFIER_NAME;

        static {
            // order is important
            DEFAULT_QUALIFIER_NAME = new ArrayList<>();
            DEFAULT_QUALIFIER_NAME.add("ALPHA");
            DEFAULT_QUALIFIER_NAME.add("BETA");
            DEFAULT_QUALIFIER_NAME.add("SNAPSHOT");
            DEFAULT_QUALIFIER_NAME.add("RC");
            DEFAULT_QUALIFIER_NAME.add("RELEASE");
        }

        private final String qualifier;
        private final String label;
        private final int priority;
        private final int number;

        /**
         * Creates a new {@link Qualifier} instance.
         * @param qualifier the qualifier string.
         */
        Qualifier(final String qualifier) {
            Objects.requireNonNull(qualifier, "qualifier cannot be null");
            this.qualifier = qualifier;
            label = getUniformQualifier(qualifier);
            priority = DEFAULT_QUALIFIER_NAME.indexOf(label);
            if (priority < 0) {
                throw new IllegalArgumentException("Qualifier not supported '" + label + "'");
            }
            number = (label.length() < qualifier.length())  ? getQualifierNumber(qualifier) : 0;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object that) {
            if (this == that) return true;
            if (!(that instanceof Qualifier)) return false;
            return qualifier.equals(((Qualifier) that).qualifier);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(qualifier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final Qualifier that) {
            int compare = Integer.compare(that.priority, this.priority);
            return (compare != 0) ? compare : Integer.compare(that.number, this.number);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return qualifier;
        }
    }

    private static int getQualifierNumber(final String qualifier) {
        StringBuilder label = new StringBuilder();
        char[] chars = qualifier.toCharArray();
        for (char c : chars) {
            if (Character.isDigit(c)) {
                label.append(c);
            }
        }
        return Integer.parseInt(label.toString());
    }

    private static String getUniformQualifier(final String qualifier) {
        StringBuilder label = new StringBuilder();
        char[] chars = qualifier.toCharArray();
        for (char c : chars) {
            if (Character.isLetter(c)) {
                label.append(c);
            } else {
                break;
            }
        }
        return label.toString().toUpperCase();
    }
}
