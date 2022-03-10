/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collections;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 * Caches a set of elements which are automatically evicted based on the cache TTL.
 *
 * <p>Subsequent calls to add with the same element refresh the expiry period for that element.
 */
public class TimeExpiringSet<E> implements Iterable<E> {
    private Cache<E, E> cache;

    /**
     * Allocates a new TimeExpiringSet whose elements expire after the given time period
     *
     * <p>E.g. for a ttl of 5 and a unit of TimeUnit.SECONDS, a newly added element will remain in
     * the Set for 5 seconds before it is evicted.
     *
     * @param ttl The magnitude of the time a unit will remain in the cache before it is evicted
     * @param unit The unit of the ttl
     */
    public TimeExpiringSet(long ttl, TimeUnit unit) {
        cache = CacheBuilder.newBuilder().expireAfterWrite(ttl, unit).build();
    }

    /**
     * Returns true if e is currently a member of the Set
     *
     * @param e The element to tests
     * @return true if e is currently a member of the Set
     */
    public boolean contains(E e) {
        return cache.getIfPresent(e) != null;
    }

    /**
     * Returns the number of elements currently in the Set
     *
     * @return the number of elements currently in the Set
     */
    public long size() {
        return cache.size();
    }

    /**
     * Returns a weakly-consistent, thread-safe {@link Iterator} over the elements in the Set
     *
     * <p>This means that while the Iterator is thread-safe, if elements expire after the Iterator
     * is created, the changes may not be reflected in the iteration. That is, you may iterate over
     * an element which was invalidated during your iteration. This is okay for many use cases which
     * can tolerate weak consistency.
     *
     * @return a weakly-consistent, thread-safe {@link Iterator} over the elements in the Set
     */
    @Nonnull
    public Iterator<E> iterator() {
        return cache.asMap().keySet().iterator();
    }

    /**
     * Adds an element into the Set
     *
     * @param e the element to add into the Set
     */
    public void add(E e) {
        cache.put(e, e);
    }

    /**
     * Simple weakly-consistent forEach implementation applies the given action to each element
     *
     * @param action The action to apply to each element
     */
    public void forEach(Consumer<? super E> action) {
        iterator().forEachRemaining(action);
    }
}
