package org.corfudb.runtime.collections;

import io.netty.buffer.Unpooled;
import org.corfudb.runtime.exceptions.unrecoverable.UnrecoverableCorfuError;
import org.corfudb.util.serializer.ISerializer;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 *
 * Created by Maithem on 1/21/20.
 */

public class RocksDbEntryIterator<K, V> implements Iterator<Map.Entry<K, V>>, AutoCloseable {

    final private RocksIterator iterator;
    final private ISerializer serializer;
    private Map.Entry<K, V> next;
    public RocksDbEntryIterator(RocksIterator iterator, ISerializer serializer) {
        this.iterator = iterator;
        this.serializer = serializer;
        iterator.seekToFirst();
        checkInvariants();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        while (next == null && iterator.isValid()) {
            // Go ahead and cache that entry.
            next = new AbstractMap.SimpleEntry(
                    serializer.deserialize(Unpooled.wrappedBuffer(iterator.key()), null),
                    serializer.deserialize(Unpooled.wrappedBuffer(iterator.value()), null));
            // Advance the underlying iterator.
            iterator.next();
            checkInvariants();
        }
        return next != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map.Entry<K, V> next() {
        if (hasNext()) {
            Map.Entry res = next;
            next = null;
            return res;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Ensure that this iterator is still valid.
     */
    private void checkInvariants() {
        try {
            iterator.status();
        } catch (RocksDBException e) {
            throw new UnrecoverableCorfuError("There was an error reading the persisted map.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // Release the underlying RocksDB resources
        if (!iterator.isOwningHandle()) {
            throw new IllegalStateException("Detected multi-threaded access to this iterator.");
        }
        iterator.close();
    }
}
