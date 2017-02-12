package com.appunite.flowable;

import org.junit.Test;

import java.io.IOException;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class FlowableResponseOrErrorTest {

    @Test
    public void testFlowableReturnsValidValue_onNextValid() {
        Flowable.just(1)
                .compose(FlowableResponseOrError.<Integer>toResponseOrError())
                .test()
                .assertValue(new Predicate<FlowableResponseOrError<Integer>>() {
                    @Override
                    public boolean test(FlowableResponseOrError<Integer> responseOrError) throws Exception {
                        return responseOrError.isData() && responseOrError.data() == 1;
                    }
                });
    }

    @Test
    public void testFlowableReturnsException_onNextError() {
        final Exception exception = new IOException();

        Flowable.error(exception)
                .compose(FlowableResponseOrError.toResponseOrError())
                .test()
                .assertValue(new Predicate<FlowableResponseOrError<Object>>() {
                    @Override
                    public boolean test(FlowableResponseOrError<Object> responseOrError) throws Exception {
                        return responseOrError.isError() && responseOrError.error() == exception;
                    }
                });
    }

    @Test
    public void testThatMapReturnsCorrectValue_onNextNewMappedValue() {
        Flowable.just(5)
                .compose(FlowableResponseOrError.<Integer>toResponseOrError())
                .compose(FlowableResponseOrError.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        return 10;
                    }
                }))
                .test()
                .assertValue(new Predicate<FlowableResponseOrError<Integer>>() {
                    @Override
                    public boolean test(FlowableResponseOrError<Integer> responseOrError) throws Exception {
                        return responseOrError.isData() && responseOrError.data() == 10;
                    }
                });
    }

    @Test
    public void testThatFlatMapReturnsCorrectValue_onNextNewFlatMappedValue() {
        Flowable.just(5)
                .compose(FlowableResponseOrError.<Integer>toResponseOrError())
                .compose(FlowableResponseOrError.flatMap(new Function<Integer, Flowable<FlowableResponseOrError<Integer>>>() {
                    @Override
                    public Flowable<FlowableResponseOrError<Integer>> apply(Integer integer) throws Exception {
                        return Flowable.just(FlowableResponseOrError.fromData(10));
                    }
                }))
                .test()
                .assertValue(new Predicate<FlowableResponseOrError<Integer>>() {
                    @Override
                    public boolean test(FlowableResponseOrError<Integer> responseOrError) throws Exception {
                        return responseOrError.isData() && responseOrError.data() == 10;
                    }
                });
    }

    @Test
    public void testThatFlatMapWithErrorReturnsError_onNextReturnsFlatMappedError() {
        Flowable.just(5)
                .compose(FlowableResponseOrError.<Integer>toResponseOrError())
                .compose(FlowableResponseOrError.flatMap(new Function<Integer, Flowable<FlowableResponseOrError<Integer>>>() {
                    @Override
                    public Flowable<FlowableResponseOrError<Integer>> apply(Integer integer) throws Exception {
                        return Flowable.just(FlowableResponseOrError.<Integer>fromError(new IOException()));
                    }
                }))
                .test()
                .assertValue(new Predicate<FlowableResponseOrError<Integer>>() {
                    @Override
                    public boolean test(FlowableResponseOrError<Integer> responseOrError) throws Exception {
                        return responseOrError.isError() && responseOrError.error() instanceof IOException;
                    }
                });
    }

    @Test
    public void testThatSwitchMapReturnsCorrectValue_onNextNewSwitchMappedValue() {
        Flowable.just(5)
                .compose(FlowableResponseOrError.<Integer>toResponseOrError())
                .compose(FlowableResponseOrError.switchMap(new Function<Integer, Flowable<FlowableResponseOrError<Integer>>>() {
                    @Override
                    public Flowable<FlowableResponseOrError<Integer>> apply(Integer integer) throws Exception {
                        return Flowable.just(FlowableResponseOrError.fromData(10));
                    }
                }))
                .test()
                .assertValue(new Predicate<FlowableResponseOrError<Integer>>() {
                    @Override
                    public boolean test(FlowableResponseOrError<Integer> responseOrError) throws Exception {
                        return responseOrError.isData() && responseOrError.data() == 10;
                    }
                });
    }

    @Test
    public void testThatSwitchMapWithErrorReturnsError_onNextReturnsSwitchMappedError() {
        Flowable.just(5)
                .compose(FlowableResponseOrError.<Integer>toResponseOrError())
                .compose(FlowableResponseOrError.switchMap(new Function<Integer, Flowable<FlowableResponseOrError<Integer>>>() {
                    @Override
                    public Flowable<FlowableResponseOrError<Integer>> apply(Integer integer) throws Exception {
                        return Flowable.just(FlowableResponseOrError.<Integer>fromError(new IOException()));
                    }
                }))
                .test()
                .assertValue(new Predicate<FlowableResponseOrError<Integer>>() {
                    @Override
                    public boolean test(FlowableResponseOrError<Integer> responseOrError) throws Exception {
                        return responseOrError.isError() && responseOrError.error() instanceof IOException;
                    }
                });
    }

}