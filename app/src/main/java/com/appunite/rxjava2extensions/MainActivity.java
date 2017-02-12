package com.appunite.rxjava2extensions;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    private Disposable disposable;

    final PublishProcessor<Integer> test = PublishProcessor.create();

    final PublishSubject test1 = PublishSubject.create();

//    @NonNull
//    public Processor<Integer> obs() {
//        return test;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final AtomicInteger i = new AtomicInteger(0);
        final PublishProcessor<Integer> publishProcessor = PublishProcessor.create();


        final View click = findViewById(R.id.hello_id);
        final View zwai = findViewById(R.id.hello_zwai);

        final Flowable<Object> clickListener = Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(final FlowableEmitter<Object> e) throws Exception {

                final View.OnClickListener l = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!e.isCancelled()) {
                            e.onNext(new Object());
                        }
                    }
                };
                zwai.setOnClickListener(l);

                e.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        zwai.setOnClickListener(null);
                    }
                });
            }
        }, BackpressureStrategy.LATEST);


        clickListener
                .map(new Function<Object, Integer>() {
                    @Override
                    public Integer apply(Object o) throws Exception {
                        return i.incrementAndGet();
                    }
                })
                .subscribe(publishProcessor);


        SerialDisposable serialDisposable = new SerialDisposable();

        final List<Integer> abc = new ArrayList<>();
        abc.add(5);
        abc.add(4);
        abc.add(3);
        abc.add(2);

        disposable = publishProcessor
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.e("Value", "" + integer);
                    }
                });
//
//        Observable.interval(1, TimeUnit.MILLISECONDS, Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .flatMap(new Function<Long, ObservableSource<String>>() {
//                    @Override
//                    public ObservableSource<String> apply(Long aLong) throws Exception {
//                        Thread.sleep(5);
//                        return Observable.just("s " + aLong);
//                    }
//                })
//                .subscribe(new Consumer<String>() {
//                    @Override
//                    public void accept(String s) throws Exception {
//                        Log.e("VALUE", "" + s);
//                    }
//                });


//        Flowable.just(1)

        final PublishProcessor<Object> pushEvent = PublishProcessor.create();
        final AtomicInteger atomicInteger = new AtomicInteger(0);

//        Flowable.create(new FlowableOnSubscribe<Integer>() {
//            @Override
//            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
//                Log.e("Emitter", "emitting exception");
////                e.onNext(1);
//                if (atomicInteger.addAndGet(1) == 4) {
//                    Log.e("TestingExponentialBackoff", "onNext");
//                    e.onNext(5);
//                } else {
//                    Log.e("TestingExponentialBackoff", "onError");
//                    e.onError(new RuntimeException());
//                }
//            }
//        }, BackpressureStrategy.LATEST)
////                .retryWhen(new Function<Flowable<Throwable>, Publisher<?>>() {
////                    @Override
////                    public Publisher<?> apply(Flowable<Throwable> throwableFlowable) throws Exception {
////                        return throwableFlowable
////                                .scan(0, new BiFunction<Integer, Object, Integer>() {
////                                    @Override
////                                    public Integer apply(Integer integer, Object o) throws Exception {
////                                        return integer == 0 ? 1 : integer * 2;
////                                    }
////                                })
////                                .flatMap(new Function<Integer, Publisher<?>>() {
////                                    @Override
////                                    public Publisher<?> apply(Integer integer) throws Exception {
////                                        Log.e("TestingExponentialBackoff", "Time " + integer);
////                                        if(integer == 4) {
////                                            return Flowable.never();
////                                        } else {
////                                            return Flowable.timer(integer, TimeUnit.SECONDS);
////                                        }
////                                    }
////                                });
////                    }
////                })
//                .compose(new FlowableTransformer<Integer, FlowableResponseOrError<Integer>>() {
//                    @Override
//                    public Publisher<FlowableResponseOrError<Integer>> apply(Flowable<Integer> upstream) {
//                        return RxJavaPlugins.onAssembly(new FlowableResponseOrErrorRetryWhen<>(upstream, new Function<Flowable<Object>, Publisher<?>>() {
//                            @Override
//                            public Publisher<?> apply(Flowable<Object> throwableFlowable) throws Exception {
//                                return throwableFlowable
//                                        .scan(1, new BiFunction<Integer, Object, Integer>() {
//                                            @Override
//                                            public Integer apply(Integer integer, Object o) throws Exception {
//                                                return integer * 2;
//                                            }
//                                        })
//                                        .switchMap(new Function<Integer, Publisher<?>>() {
//                                            @Override
//                                            public Publisher<?> apply(Integer integer) throws Exception {
//                                                Log.e("TestingExponentialBackoff", "Time " + integer + " Sqrt " + (Math.log(integer) / Math.log(2)));
//                                                    return Flowable.timer(integer, TimeUnit.SECONDS);
//                                            }
//                                        });
//                            }
//                        }));
//                    }
//                })
//                .subscribeOn(Schedulers.io())
//                .compose(FlowableOperators.<FlowableResponseOrError<Integer>>refresh(pushEvent))
//                .subscribe(new Consumer<FlowableResponseOrError<Integer>>() {
//                    @Override
//                    public void accept(FlowableResponseOrError<Integer> responseOrError) throws Exception {
//                        Log.e("TestingExponentialBackoff", "Consume " + responseOrError);
//                    }
//                });

//        final Disposable subscribe = Flowable.create(new FlowableOnSubscribe<Integer>() {
//            @Override
//            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
//                Log.e("Emitter", "emitting exception");
////                e.onNext(1);
//                Thread.sleep(3000);
//                if(atomicInteger.addAndGet(1) == 5) {
//                    Log.e("TestingExponentialBackoff", "onNext");
//                    e.onNext(5);
//                } else {
//                    Log.e("TestingExponentialBackoff", "onError");
//                    e.onError(new RuntimeException());
//                }
//            }
//        }, BackpressureStrategy.LATEST)
//                .compose(new FlowableTransformer<Integer, FlowableResponseOrError<Integer>>() {
//                    @Override
//                    public Publisher<FlowableResponseOrError<Integer>> apply(Flowable<Integer> upstream) {
//                        return RxJavaPlugins.onAssembly(new FlowableResponseOrErrorRetryWhen<>(upstream, new Function<Flowable<Object>, Publisher<?>>() {
//                            @Override
//                            public Publisher<?> apply(Flowable<Object> throwableFlowable) throws Exception {
//                                return throwableFlowable
//                                        .scan(0, new BiFunction<Integer, Object, Integer>() {
//                                            @Override
//                                            public Integer apply(Integer integer, Object o) throws Exception {
//                                                return integer == 0 ? 1 : integer * 2;
//                                            }
//                                        })
//                                        .flatMap(new Function<Integer, Publisher<?>>() {
//                                            @Override
//                                            public Publisher<?> apply(Integer integer) throws Exception {
//                                                Log.e("TestingExponentialBackoff", "Time " + integer);
//                                                return Flowable.timer(integer, TimeUnit.SECONDS);
//                                            }
//                                        });
//                            }
//                        }));
//                    }
//                })
//                .subscribeOn(Schedulers.io())
//                .subscribe(new Consumer<FlowableResponseOrError<Integer>>() {
//                    @Override
//                    public void accept(FlowableResponseOrError<Integer> responseOrError) throws Exception {
//                        Log.e("TestingExponentialBackoff", "Consume " + responseOrError);
//                    }
//                });
//                .subscribe(new Consumer<FlowableResponseOrError<Integer>>() {
//                    @Override
//                    public void accept(FlowableResponseOrError<Integer> responseOrError) throws Exception {
//                        Log.e("RetryWhenUnderstanding", "Value " + responseOrError);
//                    }
//                });
//                .subscribe(new Consumer<Integer>() {
//                    @Override
//                    public void accept(Integer integer) throws Exception {
//        Log.e("Emitter", "INSIDE THROWABLES");
//                        Log.e("Value", "" + integer);
//                    }
//                });


        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("RetryWhenUnderstanding", "DISPOSE SHOULD STOP RECONNECTING");
//                subscribe.dispose();
//                disposable.dispose();
                atomicInteger.set(0);
                pushEvent.onNext(new Object());
            }
        });


//        Flowable.create(new FlowableOnSubscribe<Integer>() {
//            @Override
//            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
//                Log.e("Emitter", "emitting exception");
////                e.onNext(1);
//                Thread.sleep(6000);
//                e.onError(new RuntimeException());
////                e.onComplete();
//            }
//        }, BackpressureStrategy.LATEST)
//                .compose(FlowableResponseOrError.<Integer>toResponseOrError())
//                .doOnComplete(new Action() {
//                    @Override
//                    public void run() throws Exception {
//                        Log.e("RetryWhenUnderstanding", "OnComplete #1");
//                    }
//                })
//                .compose(new FlowableTransformer<FlowableResponseOrError<Integer>, FlowableResponseOrError<Integer>>() {
//                    @Override
//                    public Publisher<FlowableResponseOrError<Integer>> apply(Flowable<FlowableResponseOrError<Integer>> upstream) {
//                        return RxJavaPlugins.onAssembly(new FlowableResponseOrErrorRetryWhen<>(upstream, new Function<Flowable<Object>, Publisher<?>>() {
//                            @Override
//                            public Publisher<?> apply(Flowable<Object> objectFlowable) throws Exception {
//                                return objectFlowable
//                                        .map(new Function<Object, Integer>() {
//                                            @Override
//                                            public Integer apply(Object o) throws Exception {
//                                                Log.e("RetryWhenUnderstanding", "----Inside Function");
//                                                return 10;
//                                            }
//                                        });
//                            }
//                        }));
//                    }
//                })
////                .compose(new FlowableTransformer<FlowableResponseOrError<Integer>, FlowableResponseOrError<Integer>>() {
////                    @Override
////                    public Publisher<FlowableResponseOrError<Integer>> apply(Flowable<FlowableResponseOrError<Integer>> upstream) {
////                        return RxJavaPlugins.onAssembly(new FlowableResponseOrErrorRetryWhen<>(upstream, new Function<Flowable<Throwable>, Publisher<?>>() {
////                            @Override
////                            public Publisher<?> apply(Flowable<Throwable> throwableFlowable) throws Exception {
////                                return throwableFlowable
////                                        .map(new Function<Throwable, Integer>() {
////                                            @Override
////                                            public Integer apply(Throwable throwable) throws Exception {
////                                                Log.e("Emitter", "INSIDE THROWABLES");
////                                                return 10;
////                                            }
////                                        });
////                            }
////                        }));
////                    }
////                })
//                .doOnComplete(new Action() {
//                    @Override
//                    public void run() throws Exception {
//                        Log.e("RetryWhenUnderstanding", "OnComplete #2");
//                    }
//                })
//                .subscribe(new Consumer<FlowableResponseOrError<Integer>>() {
//                    @Override
//                    public void accept(FlowableResponseOrError<Integer> integer) throws Exception {
//                        Log.e("RetryWhenUnderstanding", "@Value: " + integer);
//                    }
//                });


//        Flowable.interval(1, TimeUnit.MILLISECONDS, Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .retryWhen(new Function<Flowable<Throwable>, Publisher<?>>() {
//                    @Override
//                    public Publisher<?> apply(Flowable<Throwable> throwableFlowable) throws Exception {
//                        return null;
//                    }
//                })
//                .subscribe(new Consumer<Long>() {
//                    @Override
//                    public void accept(Long aLong) throws Exception {
//                        // do smth
//                    }
//                });
//
//        Flowable.interval(1, TimeUnit.MILLISECONDS, Schedulers.io())
//                .takeUntil(new Predicate<Long>() {
//                    @Override
//                    public boolean test(Long aLong) throws Exception {
//                        return aLong == 10000;
//                    }
//                })
////                .onBackpressureDrop()
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<Long>() {
//                    @Override
//                    public void accept(Long aLong) throws Exception {
////                        Thread.sleep(1000);
//                        Log.e("VALUE", "" + aLong);
//                    }
//                });


        Observable.interval(1, TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .replay()
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Thread.sleep(1000);
                        Log.e("Value", "" + aLong);
                    }
                });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
}
