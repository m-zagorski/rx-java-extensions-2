package com.appunite.flowable.internals;


import com.appunite.flowable.FlowableResponseOrError;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.internal.subscriptions.SubscriptionArbiter;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.SerializedSubscriber;

public class FlowableResponseOrErrorRetryWhen<T> extends Flowable<FlowableResponseOrError<T>> implements HasUpstreamPublisher<T> {
    private final Publisher<T> source;
    private final Function<? super Flowable<Object>, ? extends Publisher<?>> handler;

    public FlowableResponseOrErrorRetryWhen(@Nonnull Publisher<T> source,
                                            @Nonnull Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        this.source = source;
        this.handler = handler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super FlowableResponseOrError<T>> s) {
        final SerializedSubscriber<FlowableResponseOrError<T>> z = new SerializedSubscriber<>(s);
        final FlowableProcessor<Object> processor = UnicastProcessor.create(8).toSerialized();
        Publisher<?> when;

        try {
            when = ObjectHelper.requireNonNull(handler.apply(processor), "handler returned a null Publisher");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        final WhenReceiver<T> receiver = new WhenReceiver<>(source);

        final RetryWithNextSubscriber<T> subscriber1 = new RetryWithNextSubscriber<>(z, processor, receiver);
        receiver.subscriber = subscriber1;
        s.onSubscribe(subscriber1);
        when.subscribe(receiver);
        receiver.onNext(0);
    }

    @Override
    public Publisher<T> source() {
        return source;
    }

    private static final class WhenReceiver<T> extends AtomicInteger implements Subscriber<Object>, Subscription {
        private static final long serialVersionUID = 2827772011130406689L;

        final Publisher<T> source;
        final AtomicReference<Subscription> subscription;
        final AtomicLong requested;
        RetryWithNextSubscriber<T> subscriber;

        WhenReceiver(Publisher<T> source) {
            this.source = source;
            this.subscription = new AtomicReference<>();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(subscription, requested, s);
        }

        @Override
        public void onNext(Object t) {
            if (getAndIncrement() == 0) {
                for (; ; ) {
                    if (SubscriptionHelper.isCancelled(subscription.get())) {
                        return;
                    }
                    source.subscribe(subscriber);

                    if (decrementAndGet() == 0) {
                        break;
                    }
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            subscriber.cancel();
            subscriber.actual.onError(t);
        }

        @Override
        public void onComplete() {
            subscriber.cancel();
            subscriber.actual.onComplete();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(subscription, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(subscription);
        }
    }

    private static class RetryWithNextSubscriber<T> extends SubscriptionArbiter implements Subscriber<T> {

        final Subscriber<? super FlowableResponseOrError<T>> actual;
        final FlowableProcessor<Object> processor;
        final Subscription receiver;

        private long produced;

        RetryWithNextSubscriber(Subscriber<? super FlowableResponseOrError<T>> actual,
                                FlowableProcessor<Object> processor,
                                Subscription receiver) {
            this.actual = actual;
            this.processor = processor;
            this.receiver = receiver;
        }

        @Override
        public void onSubscribe(Subscription s) {
            setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            produced++;
            actual.onNext(FlowableResponseOrError.fromData(t));
        }

        @Override
        public void onError(Throwable t) {
            actual.onNext(FlowableResponseOrError.<T>fromError(t));
            again(t);
        }

        @Override
        public void onComplete() {
            receiver.cancel();
            actual.onComplete();
        }

        @Override
        public void cancel() {
            super.cancel();
            receiver.cancel();
        }

        final void again(Object signal) {
            long p = produced;
            if (p != 0L) {
                produced = 0L;
                produced(p);
            }
            receiver.request(1);
            processor.onNext(signal);
        }
    }
}
