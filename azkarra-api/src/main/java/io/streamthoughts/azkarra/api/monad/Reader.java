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

import java.util.function.Function;

public class Reader<A, B> implements Function<A, B> {

    private Function<A, B> function;

    public static <S, A> Reader<S, A> of(final Function<S, A> function) {
        return new Reader<>(function);
    }

    private Reader(final Function<A, B> function) {
        this.function = function;
    }

    public <BB> Reader<A, BB> map(final Function<? super B, ? extends BB> f) {
        return new Reader<>((A a) -> f.apply(apply(a)));
    }

    public <BB> Reader<A, BB> flatMap(final Function<? super B, Reader<A, ? extends BB>> f) {
        return new Reader<>((A a) -> f.apply(apply(a)).apply(a));
    }

    @Override
    public B apply(final A store) {
        return function.apply(store);
    }
}
