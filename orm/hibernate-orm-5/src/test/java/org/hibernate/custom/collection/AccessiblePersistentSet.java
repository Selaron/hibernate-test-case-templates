package org.hibernate.custom.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

/**
 * 
 * A {@link PersistentSet} granting more access than it's parent. Grants access
 * to protected {@link #isInitialized()} and {@link #getCachedSize()}.
 * 
 * @author Selaron
 *
 */
public class AccessiblePersistentSet extends PersistentSet implements UserCollectionType {

	private static final long serialVersionUID = 835991444426456017L;

	public AccessiblePersistentSet() {
		super();
	}

	public AccessiblePersistentSet(SharedSessionContractImplementor session, Set set) {
		super(session, set);
	}

	public AccessiblePersistentSet(SharedSessionContractImplementor session) {
		super(session);
	}

	/** Make protected cachedSize accessible. */
	@Override
	public int getCachedSize() {
		return super.getCachedSize();
	}

	/** Make the protected initialized accessible. */
	@Override
	public boolean isInitialized() {
		return super.isInitialized();
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
			throws HibernateException {
		return new AccessiblePersistentSet(session);
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		if (collection instanceof AccessiblePersistentSet) {
			return (AccessiblePersistentSet) collection;
		}
		return new AccessiblePersistentSet(session, (Set) collection);
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ((Collection) collection).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return false;
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		return null;
	}

	@Override
	public Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner,
			Map copyCache, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return new HashSet<>();
	}

}
