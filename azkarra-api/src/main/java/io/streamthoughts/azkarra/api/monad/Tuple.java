/*
 * Copyright 2019-2020 StreamThoughts.
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
package io.streamthoughts.azkarra.api.monad;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Tuple<L, R> {

    private L left;

    private R right;

    public static <L, R> Tuple<L, R> of(final Map.Entry<L, R> entry) {
        return new Tuple<>(entry.getKey(), entry.getValue());
    }

    public static <L, R> Tuple<L, R> of(L l, R r) {
        return new Tuple<>(l, r);
    }

    /**
     * Creates a new {@link Tuple} instance.
     *
     * @param left    the first value.
     * @param right    the second value.
     */
    public Tuple(final L left, final R right) {
        this.left = left;
        this.right = right;
    }

    public L left() {
        return left;
    }

    public R right() {
        return right;
    }

    public <LL> Tuple<LL, R> mapKey(final Function<L, LL> fn) {
        return new Tuple<>(fn.apply(left), right);
    }

    public <RR> Tuple<L, RR> mapValue(final Function<R, RR> fn) {
        return new Tuple<>(left, fn.apply(right));
    }

    public <LL, RR> Tuple<LL, RR> fold(final Function<L, LL> fl, final Function<R, RR> fr) {
        return new Tuple<>(fl.apply(left), fr.apply(right));
    }

    public <LL, RR> Tuple<LL, RR> flatMap(final Function<Tuple<L, R>, Tuple<LL, RR>> mapper) {
        return mapper.apply(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tuple)) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(left, tuple.left) &&
                Objects.equals(right, tuple.right);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + left + ", " + right + ")";
    }
}
