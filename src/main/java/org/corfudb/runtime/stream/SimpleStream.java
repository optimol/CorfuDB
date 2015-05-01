package org.corfudb.runtime.stream;

import org.corfudb.runtime.LinearizationException;
import org.corfudb.runtime.OutOfSpaceException;
import org.corfudb.runtime.entries.IStreamEntry;
import org.corfudb.runtime.entries.SimpleStreamEntry;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mwei on 4/30/15.
 */
public class SimpleStream implements IStream {

    IStreamingSequencer sequencer;
    IWriteOnceAddressSpace addressSpace;
    UUID streamID;
    AtomicLong streamPointer;

    /**
     * Open a simple stream. If the simple stream already exists, it is re-opened.
     * @param streamID          The id of the stream
     * @param sequencer         A streaming sequencer to use
     * @param addressSpace      A write once address space to use
     */
    public SimpleStream(UUID streamID, IStreamingSequencer sequencer, IWriteOnceAddressSpace addressSpace) {
        this.sequencer = sequencer;
        this.addressSpace = addressSpace;
        this.streamID = streamID;
        this.streamPointer = new AtomicLong();
    }

    /**
     * Append an object to the stream. This operation may or may not be successful. For example,
     * a move operation may occur, and the append will not be part of the stream.
     *
     * @param data A serializable object to append to the stream.
     * @return A timestamp, which reflects the physical position and the epoch the data was written in.
     */
    @Override
    public ITimestamp append(Serializable data) throws OutOfSpaceException, IOException {
        long sequence = sequencer.getNext(streamID);
        SimpleTimestamp timestamp = new SimpleTimestamp(sequence);
        addressSpace.write(sequence, new SimpleStreamEntry(streamID, data, timestamp));
        return timestamp;
    }


    /**
     * Read the next entry in the stream as a CorfuDBStreamEntry. This function
     * retrieves the next entry in the stream, or returns null if there are no entries in the stream
     * to be read.
     *
     * @return A CorfuDBStreamEntry containing the payload of the next entry in the stream.
     */
    @Override
    public IStreamEntry readNextEntry() throws IOException, InterruptedException {
        long current = sequencer.getCurrent(streamID);
        long thisPointer;
        while ((thisPointer = streamPointer.getAndIncrement()) < current)
        {
            try {
                return (SimpleStreamEntry) addressSpace.readObject(thisPointer);
            }
            catch (ClassNotFoundException|ClassCastException e)
            {
                //ignore, not a entry we understand.
            }
        }
        return null;
    }

    /**
     * Returns a fresh or cached timestamp, which can serve as a linearization point. This function
     * may return a non-linearizable (invalid) timestamp which may never occur in the ordering
     * due to a move/epoch change.
     *
     * @param cached Whether or not the timestamp returned is cached.
     * @return A timestamp, which reflects the most recently allocated timestamp in the stream,
     * or currently read, depending on whether cached is set or not.
     */
    @Override
    public ITimestamp check(boolean cached) {
        return new SimpleTimestamp(sequencer.getCurrent(streamID));
    }

    /**
     * Gets the current position the stream has read to (which may not point to an entry in the
     * stream).
     *
     * @return A timestamp, which reflects the most recently read address in the stream.
     */
    @Override
    public ITimestamp getCurrentPosition() {
        return new SimpleTimestamp(streamPointer.get());
    }

    /**
     * Requests a trim on this stream. This function informs the configuration master that the
     * position on this stream is trimmable, and moves the start position of this stream to the
     * new position.
     *
     * @param address
     */
    @Override
    public void trim(ITimestamp address) {

    }


    /**
     * Close the stream. This method must be called to free resources.
     */
    @Override
    public void close() {

    }

    /**
     * Get the ID of the stream.
     *
     * @return The ID of the stream.
     */
    @Override
    public UUID getStreamID() {
        return streamID;
    }
}
