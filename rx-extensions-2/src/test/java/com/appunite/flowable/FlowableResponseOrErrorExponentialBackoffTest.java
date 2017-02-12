package com.appunite.flowable;


import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableResponseOrErrorExponentialBackoffTest {

    private final TestScheduler scheduler = new TestScheduler();

    @Test
    public void testSubscribeToSuccess_getNextValue() throws Exception {
        Flowable.just(5)
                .compose(FlowableResponseOrError.<Integer>toResponseOrErrorWithExponentialBackoff(scheduler))
                .test()
                .assertValue(FlowableResponseOrError.fromData(5));
    }

    @Test
    public void testSubscribeToSuccess_onCompleteCalled() throws Exception {
        Flowable.just(5)
                .compose(FlowableResponseOrError.<Integer>toResponseOrErrorWithExponentialBackoff(scheduler))
                .test()
                .assertSubscribed()
                .assertComplete();
    }

    @Test
    public void testSubscribeToError_getNextValueWithError() throws Exception {
        final Throwable exception = new IOException();
        Flowable.error(exception)
                .compose(FlowableResponseOrError.toResponseOrErrorWithExponentialBackoff(scheduler))
                .test()
                .assertValue(FlowableResponseOrError.fromError(exception));
    }

    @Test
    public void testSubscribeErrorSourceResubscribed_getNextValueTwice() throws Exception {
        final TestSubscriber<FlowableResponseOrError<Object>> subscriber = Flowable.error(new IOException())
                .compose(FlowableResponseOrError.toResponseOrErrorWithExponentialBackoff(scheduler))
                .test();

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        subscriber
                .assertValueCount(2);
    }

    @Test
    public void testSubscribeErrorSourceResoubscribed_getCorrectValuesTwice() throws Exception {
        final Throwable exception = new IOException();
        final TestSubscriber<FlowableResponseOrError<Object>> subscriber = Flowable.error(exception)
                .compose(FlowableResponseOrError.toResponseOrErrorWithExponentialBackoff(scheduler))
                .test();

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        //noinspection unchecked
        subscriber
                .assertValueCount(2)
                .assertValues(
                        FlowableResponseOrError.fromError(exception),
                        FlowableResponseOrError.fromError(exception));
    }

    @Test
    public void testAfterErrorFlowableReturnsData_correctOnNext() throws Exception {
        final AtomicBoolean returnData = new AtomicBoolean(false);
        final Throwable exception = new IOException();

        final TestSubscriber<FlowableResponseOrError<Integer>> subscriber = Flowable
                .create(new FlowableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                        if (returnData.get()) {
                            e.onNext(5);
                        } else {
                            e.onError(exception);
                        }
                    }
                }, BackpressureStrategy.LATEST)
                .compose(FlowableResponseOrError.<Integer>toResponseOrErrorWithExponentialBackoff(scheduler))
                .test();

        subscriber.assertValue(FlowableResponseOrError.<Integer>fromError(exception));

        returnData.set(true);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        //noinspection unchecked
        subscriber
                .assertValueCount(2)
                .assertValues(
                        FlowableResponseOrError.<Integer>fromError(exception),
                        FlowableResponseOrError.fromData(5));
    }

    @Test
    public void testSubscribeToErrorThreeTimes_threeValuesEmitted() throws Exception {
        final Throwable exception = new IOException();
        final TestSubscriber<FlowableResponseOrError<Object>> subscriber = Flowable.error(exception)
                .compose(FlowableResponseOrError.toResponseOrErrorWithExponentialBackoff(scheduler))
                .test();

        // 1 at start first resubscribe
        // 2 after second
        // + first value emitted right away
        scheduler.advanceTimeBy(3, TimeUnit.SECONDS);

        //noinspection unchecked
        subscriber
                .assertValueCount(3)
                .assertValues(
                        FlowableResponseOrError.fromError(exception),
                        FlowableResponseOrError.fromError(exception),
                        FlowableResponseOrError.fromError(exception));
    }

    @Test
    public void testSubscribeToErrorWith5SecStartDelay_correctStartDelayPropagated() throws Exception {
        final Throwable exception = new IOException();
        final TestSubscriber<FlowableResponseOrError<Object>> subscriber = Flowable.error(exception)
                .compose(FlowableResponseOrError.toResponseOrErrorWithExponentialBackoff(scheduler, 5))
                .test();

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS);

        subscriber
                .assertValueCount(1)
                .assertValue(FlowableResponseOrError.fromError(exception));
    }

    @Test
    public void testSubscribeToErrorWith5Sec_startDelayCorrect() throws Exception {
        final Throwable exception = new IOException();
        final TestSubscriber<FlowableResponseOrError<Object>> subscriber = Flowable.error(exception)
                .compose(FlowableResponseOrError.toResponseOrErrorWithExponentialBackoff(scheduler, 5))
                .test();

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        //noinspection unchecked
        subscriber
                .assertValueCount(2)
                .assertValues(
                        FlowableResponseOrError.fromError(exception),
                        FlowableResponseOrError.fromError(exception));
    }
}
