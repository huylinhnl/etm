package com.jecstar.etm.server.core.elasticsearch;


import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SyncResponseListener implements ResponseListener {

    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Response> response = new AtomicReference<>();
    private final AtomicReference<Exception> exception = new AtomicReference<>();

    private final long timeout;

    public SyncResponseListener(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void onSuccess(Response response) {
        Objects.requireNonNull(response, "response must not be null");
        boolean wasResponseNull = this.response.compareAndSet(null, response);
        if (!wasResponseNull) {
            throw new IllegalStateException("response is already set");
        }

        latch.countDown();
    }

    @Override
    public void onFailure(Exception exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        boolean wasExceptionNull = this.exception.compareAndSet(null, exception);
        if (!wasExceptionNull) {
            throw new IllegalStateException("exception is already set");
        }
        latch.countDown();
    }

    /**
     * Waits (up to a timeout) for some result of the request: either a response, or an exception.
     */
    public Response get() {
        try {
            //providing timeout is just a safety measure to prevent everlasting waits
            //the different client timeouts should already do their jobs
            if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
                return null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("thread waiting for the response was interrupted", e);
        }
        return this.response.get();
    }
}
