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
package io.streamthoughts.azkarra.api.monad;

import io.streamthoughts.azkarra.api.errors.ExecutionException;
import io.streamthoughts.azkarra.api.time.Time;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public interface Try<V> {

    static <V, E extends Throwable> Retriable<V> retriable(final CheckedSupplier<V, E> supplier, final Retry retry) {
        final long started = Time.SYSTEM.milliseconds();

        int numberOfFailedAttempts = 0;
        try {
            long timeout;
            Try<V> failable;
            do {
                failable = failable(supplier);
                if (failable.isSuccess() || !retry.isRetriable(failable.getThrowable())) {
                    return new Retriable<>(numberOfFailedAttempts, failable);
                }
                timeout = Math.abs(retry.stopAfterMs() - (Time.SYSTEM.milliseconds() - started));
                long waitMs = Math.min(timeout, retry.fixedWaitMs());
                Thread.sleep(waitMs);
                numberOfFailedAttempts++;
            } while (numberOfFailedAttempts < retry.maxAttempts() && timeout > 0);
            return new Retriable<>(numberOfFailedAttempts, failable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Retriable<>(numberOfFailedAttempts, Try.failure(e));
        }
    }

    static <V, E extends Throwable> Try<V> failable(final CheckedSupplier<V, E> supplier) {
       try {
           return success(supplier.get());
       } catch (Throwable t) {
           return failure(t);
       }
    }

    static <V> Try<V> success(V v) {
        return new Success<>(v);
    }

    static <V> Try<V> failure(Throwable t) {
        return new Failure<>(t);
    }

    V get();

    boolean isSuccess();

    boolean isFailure();

    Throwable getThrowable();

    <B> Try<B> map(final Function<? super V, ? extends B> mapper);

    <B> Try<B> flatMap(final Function<? super V, Try<B>> mapper);

    Try<V> recover(final Function<? super Throwable, Try<V>> f);

    <U> Try<U> transform(final Function<V, Try<U>> s, final Function<Throwable, Try<U>> f);

    default Optional<V> toOptional() {
        return isFailure() ? Optional.empty() : Optional.of(get());
    }

    class Retriable<V> implements Try<V> {

        private final int numberOfFailedAttempts;

        private final Try<V> lastAttempt;

        Retriable(int numberOfFailedAttempts, final Try<V> lastAttempt) {
            this.numberOfFailedAttempts = numberOfFailedAttempts;
            this.lastAttempt = lastAttempt;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V get() {
            return lastAttempt.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSuccess() {
            return lastAttempt.isSuccess();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isFailure() {
            return lastAttempt.isFailure();
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <B> Try<B> map(final Function<? super V, ? extends B> mapper) {
            return new Retriable<>(numberOfFailedAttempts, lastAttempt.map(mapper));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <B> Try<B> flatMap(final Function<? super V, Try<B>> mapper) {
            return new Retriable<>(numberOfFailedAttempts, lastAttempt.flatMap(mapper));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Try<V> recover(final Function<? super Throwable, Try<V>> f) {
            return lastAttempt.recover(f);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <U> Try<U> transform(final Function<V, Try<U>> s, final Function<Throwable, Try<U>> f) {
            return lastAttempt.transform(s, f);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Retriable{" +
                    "numberOfFailedAttempts=" + numberOfFailedAttempts +
                    ", lastAttempt=" + lastAttempt +
                    '}';
        }
    }

    class Failure<V> implements Try<V> {

        private final Throwable exception;

        Failure(final Throwable exception) {
            Objects.requireNonNull(exception, "exception can't be null");
            this.exception = exception;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V get() {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException)exception;
            }
            throw new ExecutionException(exception);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSuccess() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isFailure() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Throwable getThrowable() {
            return exception;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <B> Try<B> map(Function<? super V, ? extends B> mapper) {
            Objects.requireNonNull(mapper, "mapper can't be null");
            return new Try.Failure<>(exception);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <B> Try<B> flatMap(Function<? super V, Try< B>> mapper) {
            Objects.requireNonNull(mapper, "mapper can't be null");
            return new Try.Failure<>(exception);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Try<V> recover(Function<? super Throwable, Try<V>> fn) {
            try {
                return fn.apply(exception);
            } catch (Throwable t) {
                return failure(t);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <U> Try<U> transform(final Function<V, Try<U>> s, final Function<Throwable, Try<U>> f) {
            return f.apply(exception);
        }
    }

    class Success<V> implements Try<V> {

        private V value;

        Success(final V value) {
            Objects.requireNonNull(value);
            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V get() {
            return value;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSuccess() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isFailure() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Throwable getThrowable() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <B> Try<B> map(Function<? super V, ? extends B> mapper) {
            Objects.requireNonNull(mapper, "mapper can't be null");
            try {
                return Try.success(mapper.apply(value));
            } catch (Throwable t) {
                return Try.failure(t);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <B> Try<B> flatMap(Function<? super V, Try<B>> mapper) {
            Objects.requireNonNull(mapper, "mapper can't be null");
            try {
                return mapper.apply(get());
            } catch (Throwable t) {
                return new Failure<>(t);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Try<V> recover(Function<? super Throwable, Try<V>> f) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <U> Try<U> transform(final Function<V, Try<U>> s, final Function<Throwable, Try<U>> f) {
            return s.apply(value);
        }
    }
}
