package com.appunite.flowable;


import com.appunite.flowable.internals.FlowableCacheWithTimeout;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;

public class FlowableOperators {

    @Nonnull
    public static <T> FlowableTransformer<T, T> refresh(@Nonnull final Flowable<Object> refresher) {
        return new FlowableTransformer<T, T>() {
            @Override
            public Publisher<T> apply(final Flowable<T> upstream) {
                return refresher
                        .startWith(new Object())
                        .switchMap(new Function<Object, Publisher<? extends T>>() {
                            @Override
                            public Publisher<? extends T> apply(Object object) throws Exception {
                                return upstream;
                            }
                        });
            }
        };

    }

    @Nonnull
    public static <T> FlowableTransformer<T, T> cacheWithTimeout(@Nonnull final Scheduler scheduler) {
        return cacheWithTimeout(scheduler, 5, TimeUnit.SECONDS);
    }

    @Nonnull
    public static <T> FlowableTransformer<T, T> cacheWithTimeout(@Nonnull final Scheduler scheduler,
                                                                 final long delay,
                                                                 @Nonnull final TimeUnit timeUnit) {
        return new FlowableTransformer<T, T>() {
            @Override
            public Publisher<T> apply(Flowable<T> upstream) {
                return RxJavaPlugins.onAssembly(new FlowableCacheWithTimeout<T>(upstream.replay(1), delay, timeUnit, scheduler));
            }
        };
    }

    /**
     * Retry on error with exponential backoff.
     * This does not emit any errors further, only {@link Subscriber#onNext(Object)}
     * and {@link Subscriber#onComplete()}.
     * Default start delay is set to 1 second, so 1, 2, 4, 8, 16, 32, 64 etc
     *
     * @param scheduler on which timing should be done
     * @param <T>       upstream type
     */
    @Nonnull
    public static <T> FlowableTransformer<T, T> exponentialBackoff(@Nonnull Scheduler scheduler) {
        return exponentialBackoff(scheduler, 1);
    }

    /**
     * Retry on error with exponential backoff.
     * This does not emit any errors further, only {@link Subscriber#onNext(Object)}
     * and {@link Subscriber#onComplete()}.
     *
     * @param scheduler  on which timing should be done
     * @param startDelay time after which exponential backoff starts
     * @param <T>        upstream type
     */
    @Nonnull
    public static <T> FlowableTransformer<T, T> exponentialBackoff(@Nonnull final Scheduler scheduler,
                                                                   final int startDelay) {
        return new FlowableTransformer<T, T>() {
            @Override
            public Publisher<T> apply(Flowable<T> upstream) {
                return upstream
                        .retryWhen(new Function<Flowable<Throwable>, Publisher<?>>() {
                            @Override
                            public Publisher<?> apply(Flowable<Throwable> throwableFlowable) throws Exception {
                                return throwableFlowable
                                        .scan(startDelay, new BiFunction<Integer, Throwable, Integer>() {
                                            @Override
                                            public Integer apply(Integer integer, Throwable throwable) throws Exception {
                                                return integer * 2;
                                            }
                                        })
                                        .switchMap(new Function<Integer, Publisher<?>>() {
                                            @Override
                                            public Publisher<?> apply(Integer integer) throws Exception {
                                                return Flowable.timer(integer, TimeUnit.SECONDS, scheduler);
                                            }
                                        });
                            }
                        });
            }
        };
    }
}
