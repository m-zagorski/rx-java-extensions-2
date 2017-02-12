package com.appunite.flowable;


import org.junit.Test;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableOperatorsRefreshTest {

    private int value = 5;
    private final PublishProcessor<Object> refresher = PublishProcessor.create();

    @Test
    public void testThatCorrectValueIsPassed_onNextCorrectValue() {
        Flowable.just(5)
                .compose(FlowableOperators.<Integer>refresh(refresher))
                .test()
                .assertValue(5);
    }

    @Test
    public void testThatAfterRefreshNewValueIsPassed_onNextWithNewValue() {
        final TestSubscriber<Integer> subscriber = Flowable
                .create(new FlowableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                        e.onNext(value);
                    }
                }, BackpressureStrategy.LATEST)
                .compose(FlowableOperators.<Integer>refresh(refresher))
                .test();

        subscriber.assertValue(5);
        value = 10;
        refresher.onNext(new Object());
        subscriber.assertValueCount(2);
        subscriber.assertValues(5, 10);
    }

}
