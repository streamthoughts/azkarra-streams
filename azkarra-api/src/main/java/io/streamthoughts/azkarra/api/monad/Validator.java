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

import io.streamthoughts.azkarra.api.errors.Error;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Validator<T> {

    private final LinkedList<Error> errors;
    private final T value;

    public static <T> Validator<T> of(final T value) {
        Objects.requireNonNull(value, "object cannot be null");
        return new Validator<>(value);
    }

    private Validator(T value) {
        this.value = value;
        this.errors = new LinkedList<>();
    }

    public Validator<T> validates(final Predicate<? super T> predicate, final String error) {
        return validates(predicate, new Error(error));
    }

    public Validator<T> validates(final Predicate<? super T> predicate, final Error error) {
        if (!predicate.test(value)) {
            errors.add(error);
        }
        return this;
    }

    public T getOrThrow(final Function<List<Error>, Throwable> f) {
        if (isValid()) {
            return value;
        }
        Throwable t = f.apply(errors);
        if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
        } else {
            throw new RuntimeException(t);
        }
    }

    public T get() throws IllegalStateException {
        if (isValid()) {
            return value;
        }
        throw new IllegalArgumentException();
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public Optional<T> toOptional() {
        return isValid() ? Optional.of(value) : Optional.empty();
    }

    public Either<T, List<Error>> toEither() {
        return isValid() ? Either.left(value) : Either.right(errors);
    }

}
