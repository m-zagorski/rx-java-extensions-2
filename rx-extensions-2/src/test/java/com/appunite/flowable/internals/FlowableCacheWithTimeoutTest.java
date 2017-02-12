package com.appunite.flowable.internals;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableCacheWithTimeoutTest {

    private TestScheduler scheduler = new TestScheduler();
    private PublishProcessor<String> source = PublishProcessor.create();
    private Flowable<String> cachedFlowable;

    @Before
    public void setUp() throws Exception {
        cachedFlowable = new FlowableCacheWithTimeout<>(source.replay(1), 5, TimeUnit.SECONDS, scheduler);
    }

    @Test
    public void testFirstSubscription_dataIsReturned() throws Exception {
        final TestSubscriber<String> subscriber = cachedFlowable.test();

        source.onNext("cygan");

        subscriber
                .assertValue("cygan");
    }

    @Test
    public void testSecondSubscriptionBeforeDelay_dataIsReturned() throws Exception {
        final TestSubscriber<String> subscriber = cachedFlowable.test();

        source.onNext("cygan");

        subscriber
                .assertValue("cygan");

        subscriber.dispose();
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        cachedFlowable
                .test()
                .assertValue("cygan");
    }

    @Test
    public void testSecondSubscriptionAfterDelay_dataIsNotReturned() throws Exception {
        final TestSubscriber<String> subscriber = cachedFlowable.test();

        source.onNext("cygan");

        subscriber
                .assertValue("cygan");

        subscriber.dispose();
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        cachedFlowable
                .test()
                .assertNoValues();
    }

    @Test
    public void testNewSubscriptionAfterDelay_dataIsReturned() throws Exception {
        final TestSubscriber<String> subscriber = cachedFlowable.test();
        source.onNext("cygan");

        subscriber
                .assertValue("cygan");

        subscriber.dispose();
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        final TestSubscriber<String> subscriber1 = cachedFlowable.test();
        source.onNext("zbigniew");

        subscriber1
                .assertValue("zbigniew");
    }

    @Test
    public void testAnotherSubscriptionBeforeDelay_receivesBothData() throws Exception {
        final TestSubscriber<String> subscriber = cachedFlowable.test();
        source.onNext("cygan");

        subscriber
                .assertValue("cygan");

        subscriber.dispose();
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        final TestSubscriber<String> subscriber1 = cachedFlowable.test();
        source.onNext("zbigniew");

        subscriber1
                .assertValues("cygan", "zbigniew");
    }

}