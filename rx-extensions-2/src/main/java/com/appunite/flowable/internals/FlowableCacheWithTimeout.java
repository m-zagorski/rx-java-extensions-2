package com.appunite.flowable.internals;


import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.annotations.Experimental;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

@Experimental
public final class FlowableCacheWithTimeout<T> extends Flowable<T> implements HasUpstreamPublisher<T> {

    private final ConnectableFlowable<? extends T> source;
    private final Publisher<T> upstreamSource;
    private volatile CompositeDisposable baseDisposable = new CompositeDisposable();
    private final AtomicInteger subscriptionCount = new AtomicInteger();

    private final long delay;
    private final TimeUnit unit;
    private final Scheduler scheduler;

    /**
     * Use this lock for every subscription and disconnect action.
     */
    private final ReentrantLock lock = new ReentrantLock();


    /**
     * Constructor.
     *
     * @param source observable to apply cache with timeout to
     */
    public FlowableCacheWithTimeout(@Nonnull ConnectableFlowable<T> source,
                                    long delay,
                                    @Nonnull TimeUnit unit,
                                    @Nonnull Scheduler scheduler) {
        this.upstreamSource = source;
        this.source = source;
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        lock.lock();
        System.out.println("CacheWithTimeout SubscriptionCount " + subscriptionCount.get());
        if (subscriptionCount.incrementAndGet() == 1) {

            System.out.println("CacheWithTimeout FirstSubscription AfterIncrement " + subscriptionCount.get());

            final AtomicBoolean writeLocked = new AtomicBoolean(true);

            try {
                // need to use this overload of connect to ensure that
                // baseSubscription is set in the case that source is a
                // synchronous Observable
                source.connect(onSubscribe(s, writeLocked));
            } finally {
                // need to cover the case where the source is subscribed to
                // outside of this class thus preventing the Action1 passed
                // to source.connect above being called
                if (writeLocked.get()) {
                    // Action1 passed to source.connect was not called
                    lock.unlock();
                }
            }
        } else {

            System.out.println("CacheWithTimeout OtherSubscriptions AfterIncrement " + subscriptionCount.get());

            try {
                // ready to subscribe to source so do it
                doSubscribe(s, baseDisposable);
            } finally {
                // release the read lock
                lock.unlock();
            }
        }
    }

    private Consumer<Disposable> onSubscribe(final Subscriber<? super T> subscriber,
                                             final AtomicBoolean writeLocked) {
        return new Consumer<Disposable>() {
            @Override
            public void accept(Disposable subscription) {
                try {
                    baseDisposable.add(subscription);
                    // ready to subscribe to source so do it
                    doSubscribe(subscriber, baseDisposable);
                } finally {
                    // release the write lock
                    lock.unlock();
                    writeLocked.set(false);
                }
            }
        };
    }

    private void doSubscribe(final Subscriber<? super T> subscriber, final CompositeDisposable currentBase) {
        // handle disposing from the base subscription
        Disposable d = disconnect(currentBase);

        ConnectionSubscriber connection = new ConnectionSubscriber(subscriber, currentBase, d);
        subscriber.onSubscribe(connection);

        source.subscribe(connection);
    }

    private Disposable disconnect(final CompositeDisposable current) {
        return Disposables.fromRunnable(new Runnable() {
            @Override
            public void run() {
                disconnectDelayed(current);
//                lock.lock();
//                try {
//                    if (baseDisposable == current) {
//                        if (subscriptionCount.decrementAndGet() == 0) {
//                            baseDisposable.dispose();
//                            // need a new baseDisposable because once
//                            // disposed stays that way
//                            baseDisposable = new CompositeDisposable();
//                        }
//                    }
//                } finally {
//                    lock.unlock();
//                }
            }
        });
    }

    private void disconnectDelayed(final CompositeDisposable current) {
        final Scheduler.Worker worker = scheduler.createWorker();
        baseDisposable.add(worker);

        System.out.println("CacheWithTimeout DisconnectDelayed " + subscriptionCount.get());
        if (subscriptionCount.decrementAndGet() == 0) {
            System.out.println("CacheWithTimeout START SCHEDULED " + subscriptionCount.get());
            worker.schedule(new Runnable() {
                @Override
                public void run() {
                    System.out.println("CacheWithTimeout @DisconnectNow@ " + subscriptionCount.get());
                    disconnectNow(current);
                }
            }, delay, unit);
        }
    }

    private void disconnectNow(final CompositeDisposable current) {
        lock.lock();
        try {
            System.out.println("CacheWithTimeout DisconnectNow Check if current " + subscriptionCount.get());
            if (baseDisposable == current) {
                System.out.println("CacheWithTimeout DisconnectNow IS current " + subscriptionCount.get());
                if (subscriptionCount.get() == 0) {
                    baseDisposable.dispose();
                    // need a new baseDisposable because once
                    // disposed stays that way
                    baseDisposable = new CompositeDisposable();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Publisher<T> source() {
        return upstreamSource;
    }

    private final class ConnectionSubscriber extends AtomicReference<Subscription> implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = 152064694420235350L;
        final Subscriber<? super T> subscriber;
        final CompositeDisposable currentBase;
        final Disposable resource;

        final AtomicLong requested;

        ConnectionSubscriber(Subscriber<? super T> subscriber,
                             CompositeDisposable currentBase, Disposable resource) {
            this.subscriber = subscriber;
            this.currentBase = currentBase;
            this.resource = resource;
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this, requested, s);
        }

        @Override
        public void onError(Throwable e) {
            cleanup();
            subscriber.onError(e);
        }

        @Override
        public void onNext(T t) {
            subscriber.onNext(t);
        }

        @Override
        public void onComplete() {
            cleanup();
            subscriber.onComplete();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
            resource.dispose();
        }

        void cleanup() {
            // on error or completion we need to dispose the base CompositeDisposable
            // and set the subscriptionCount to 0
            lock.lock();
            try {
                if (baseDisposable == currentBase) {
                    baseDisposable.dispose();
                    baseDisposable = new CompositeDisposable();
                    subscriptionCount.set(0);
                }
            } finally {
                lock.unlock();
            }
        }
    }


}
