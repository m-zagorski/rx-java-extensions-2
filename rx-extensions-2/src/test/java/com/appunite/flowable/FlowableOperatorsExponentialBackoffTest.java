package com.appunite.flowable;


import org.junit.Test;

import java.io.IOException;

import io.reactivex.Flowable;
import io.reactivex.schedulers.TestScheduler;

public class FlowableOperatorsExponentialBackoffTest {

    private final TestScheduler scheduler = new TestScheduler();

    @Test
    public void testSubscribeToSuccess_getNextValue() {
        Flowable.just(5)
                .compose(FlowableOperators.<Integer>exponentialBackoff(scheduler))
                .test()
                .assertValue(5);
    }

    @Test
    public void testSubscribeToError_noNextValues() {
        Flowable.error(new IOException())
                .compose(FlowableOperators.exponentialBackoff(scheduler))
                .test()
                .assertNoValues();
    }

    @Test
    public void testSubscribeToError_noErrorValues() {
        Flowable.error(new IOException())
                .compose(FlowableOperators.exponentialBackoff(scheduler))
                .test()
                .assertNoErrors();
    }

}
