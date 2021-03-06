package org.corfudb.runtime.collections;

/**
 * This is the callback interface that any client subscribing to CorfuStore updates must implement.
 *
 * Created by hisundar on 2019-10-18
 */
public interface StreamListener {
    /**
     * A corfu update can/may have multiple updates belonging to different streams.
     * This callback will return those updates as a list grouped by their Stream UUIDs.
     *
     * @param results is a map of stream UUID -> list of entries of this stream.
     */
    void onNext(CorfuStreamEntries results);

    /**
     * Callback to indicate that an error or exception has occurred while streaming or that the stream is
     * shutting down. Some exceptions can be handled by restarting the stream (TrimmedException) while
     * some errors (SystemUnavailableError) are unrecoverable.
     * @param throwable
     */
    void onError(Throwable throwable);

    /**
     * Return if two streamer objects are equal.
     * This is to allow CorfuStore may have to put these callback objects into a Set or Map.
     */
    boolean equals(StreamListener o);

    /**
     * Return a unique integer representing this StreamListener
     * This is to allow CorfuStore may have to put these callback objects into a Set or Map.
     */
    int hashcode();

    /**
     * Return a human readable string representing this StreamListener.
     * Log messages within CorfuStore will use this to identify different StreamListeners.
     */
    String toString();
}
