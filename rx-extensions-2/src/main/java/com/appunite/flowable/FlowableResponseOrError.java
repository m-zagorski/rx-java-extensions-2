package com.appunite.flowable;


import com.appunite.flowable.internals.FlowableResponseOrErrorRetryWhen;

import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.annotations.Experimental;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.plugins.RxJavaPlugins;

import static com.appunite.helpers.Preconditions.checkArgument;
import static com.appunite.helpers.Preconditions.checkNotNull;
import static com.appunite.helpers.Preconditions.checkState;

public class FlowableResponseOrError<T> {
    @Nullable
    private final T data;
    @Nullable
    private final Throwable error;

    public FlowableResponseOrError(@Nullable T data, @Nullable Throwable error) {
        checkArgument(data != null ^ error != null);
        this.data = data;
        this.error = error;
    }

    @Nonnull
    public static <T> FlowableResponseOrError<T> fromError(@Nonnull Throwable throwable) {
        return new FlowableResponseOrError<>(null, checkNotNull(throwable));
    }

    @Nonnull
    public static <T> FlowableResponseOrError<T> fromData(@Nonnull T data) {
        return new FlowableResponseOrError<>(checkNotNull(data), null);
    }

    @Nonnull
    public T data() {
        checkState(data != null);
        return data;
    }

    @Nonnull
    public Throwable error() {
        checkState(error != null);
        return error;
    }

    public boolean isData() {
        return data != null;
    }

    public boolean isError() {
        return error != null;
    }

    @Nonnull
    public static <T> FlowableTransformer<T, FlowableResponseOrError<T>> toResponseOrError() {
        return new FlowableTransformer<T, FlowableResponseOrError<T>>() {
            @Override
            public Publisher<FlowableResponseOrError<T>> apply(Flowable<T> upstream) {
                return upstream
                        .map(new Function<T, FlowableResponseOrError<T>>() {
                            @Override
                            public FlowableResponseOrError<T> apply(T t) throws Exception {
                                return fromData(t);
                            }
                        })
                        .onErrorResumeNext(new Function<Throwable, Publisher<? extends FlowableResponseOrError<T>>>() {
                            @Override
                            public Publisher<? extends FlowableResponseOrError<T>> apply(Throwable throwable) throws Exception {
                                return Flowable.just(FlowableResponseOrError.<T>fromError(throwable));
                            }
                        });
            }
        };
    }

    @Nonnull
    @Experimental
    public static <T> FlowableTransformer<T, FlowableResponseOrError<T>> toResponseOrErrorWithExponentialBackoff(@Nonnull Scheduler scheduler) {
        return toResponseOrErrorWithExponentialBackoff(scheduler, 1);
    }

    @Nonnull
    @Experimental
    public static <T> FlowableTransformer<T, FlowableResponseOrError<T>> toResponseOrErrorWithExponentialBackoff(@Nonnull final Scheduler scheduler,
                                                                                                                 final long startDelay) {
        return new FlowableTransformer<T, FlowableResponseOrError<T>>() {
            @Override
            public Publisher<FlowableResponseOrError<T>> apply(Flowable<T> upstream) {
                return RxJavaPlugins.onAssembly(new FlowableResponseOrErrorRetryWhen<>(upstream, new Function<Flowable<Object>, Publisher<?>>() {
                    @Override
                    public Publisher<?> apply(Flowable<Object> objectFlowable) throws Exception {
                        return objectFlowable
                                .scan(startDelay, new BiFunction<Long, Object, Long>() {
                                    @Override
                                    public Long apply(Long value, Object o) throws Exception {
                                        return value * 2;
                                    }
                                })
                                .switchMap(new Function<Long, Publisher<?>>() {
                                    @Override
                                    public Publisher<?> apply(Long time) throws Exception {
                                        return Flowable.timer(time, TimeUnit.SECONDS, scheduler);
                                    }
                                });
                    }
                }));
            }
        };
    }

    @Nonnull
    public static <T, K> FlowableTransformer<FlowableResponseOrError<T>, FlowableResponseOrError<K>> map(@Nonnull final Function<T, K> func) {
        return new FlowableTransformer<FlowableResponseOrError<T>, FlowableResponseOrError<K>>() {
            @Override
            public Publisher<FlowableResponseOrError<K>> apply(Flowable<FlowableResponseOrError<T>> upstream) {
                return upstream
                        .map(new Function<FlowableResponseOrError<T>, FlowableResponseOrError<K>>() {
                            @Override
                            public FlowableResponseOrError<K> apply(FlowableResponseOrError<T> response) throws Exception {
                                return response.isError()
                                        ? FlowableResponseOrError.<K>fromError(response.error())
                                        : FlowableResponseOrError.fromData(func.apply(response.data()));
                            }
                        });
            }
        };
    }

    @Nonnull
    public static <T, K> FlowableTransformer<FlowableResponseOrError<T>, FlowableResponseOrError<K>> flatMap(@Nonnull final Function<T, Flowable<FlowableResponseOrError<K>>> func) {
        return new FlowableTransformer<FlowableResponseOrError<T>, FlowableResponseOrError<K>>() {
            @Override
            public Publisher<FlowableResponseOrError<K>> apply(Flowable<FlowableResponseOrError<T>> upstream) {
                return upstream
                        .flatMap(new Function<FlowableResponseOrError<T>, Publisher<FlowableResponseOrError<K>>>() {
                            @Override
                            public Publisher<FlowableResponseOrError<K>> apply(FlowableResponseOrError<T> response) throws Exception {
                                return response.isError()
                                        ? Flowable.just(FlowableResponseOrError.<K>fromError(response.error()))
                                        : func.apply(response.data());
                            }
                        });
            }
        };
    }

    @Nonnull
    public static <T, K> FlowableTransformer<FlowableResponseOrError<T>, FlowableResponseOrError<K>> switchMap(@Nonnull final Function<T, Flowable<FlowableResponseOrError<K>>> func) {
        return new FlowableTransformer<FlowableResponseOrError<T>, FlowableResponseOrError<K>>() {
            @Override
            public Publisher<FlowableResponseOrError<K>> apply(Flowable<FlowableResponseOrError<T>> upstream) {
                return upstream
                        .switchMap(new Function<FlowableResponseOrError<T>, Publisher<? extends FlowableResponseOrError<K>>>() {
                            @Override
                            public Publisher<? extends FlowableResponseOrError<K>> apply(FlowableResponseOrError<T> response) throws Exception {
                                return response.isError()
                                        ? Flowable.just(FlowableResponseOrError.<K>fromError(response.error()))
                                        : func.apply(response.data());
                            }
                        });
            }
        };
    }

    @Nonnull
    public static <T> FlowableTransformer<FlowableResponseOrError<T>, T> onlySuccess() {
        return new FlowableTransformer<FlowableResponseOrError<T>, T>() {
            @Override
            public Publisher<T> apply(Flowable<FlowableResponseOrError<T>> upstream) {
                return upstream
                        .filter(new Predicate<FlowableResponseOrError<T>>() {
                            @Override
                            public boolean test(FlowableResponseOrError<T> responseOrError) throws Exception {
                                return responseOrError.isData();
                            }
                        })
                        .map(new Function<FlowableResponseOrError<T>, T>() {
                            @Override
                            public T apply(FlowableResponseOrError<T> responseOrError) throws Exception {
                                return responseOrError.data();
                            }
                        });
            }
        };
    }

    @Nonnull
    public static <T> FlowableTransformer<FlowableResponseOrError<T>, Throwable> onlyError() {
        return new FlowableTransformer<FlowableResponseOrError<T>, Throwable>() {
            @Override
            public Publisher<Throwable> apply(Flowable<FlowableResponseOrError<T>> upstream) {
                return upstream
                        .filter(new Predicate<FlowableResponseOrError<T>>() {
                            @Override
                            public boolean test(FlowableResponseOrError<T> responseOrError) throws Exception {
                                return responseOrError.isError();
                            }
                        })
                        .map(new Function<FlowableResponseOrError<T>, Throwable>() {
                            @Override
                            public Throwable apply(FlowableResponseOrError<T> responseOrError) throws Exception {
                                return responseOrError.error();
                            }
                        });
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowableResponseOrError)) return false;

        final FlowableResponseOrError<?> that = (FlowableResponseOrError<?>) o;

        return data != null ? data.equals(that.data) : that.data == null && (error != null ? error.equals(that.error) : that.error == null);

    }

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (error != null ? error.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FlowableResponseOrError{");
        sb.append("data=").append(data);
        sb.append(", error=").append(error);
        sb.append('}');
        return sb.toString();
    }
}
