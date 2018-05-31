/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bugs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.custom.collection.AccessiblePersistentSet;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * Test Extra Lazy OneToMany Collection with default ByteCode enhancement and
 * Annotation Configuration.
 * 
 * @author Selaron
 */
@TestForIssue(jiraKey = "HHH-12648")
@RunWith(BytecodeEnhancerRunner.class)
// TODO: commenting out @CustomEnhancementContext below will add failures.
// I Failed to run the tests with multiple different EnhancerContexts in
// Eclipse.
@CustomEnhancementContext(DirtyTrackingPersistentSetTest.CustomEnhancerTestContext.class)
public class DirtyTrackingPersistentSetTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class, ChildEntity.class };
	}

	@Override
	protected void configure(final Configuration configuration) {
		super.configure(configuration);

		// print SQL
		configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
		configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());

	}

	/**
	 * Prepare and persist a {@link SimpleEntity} with two {@link ChildEntity}.
	 */
	@Before
	public void prepare() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();

		try {
			final Number count = (Number) s.createQuery("SELECT count(ID) FROM SimpleEntity").stream().findFirst()
					.get();
			if (count.longValue() > 0L) {
				// entity already added previously
				return;
			}

			final SimpleEntity entity = new SimpleEntity();
			entity.setId(1L);
			entity.setName("TheParent");

			final ChildEntity c1 = new ChildEntity();
			c1.setId(1L);
			c1.setParent(entity);
			c1.setName("child1");

			final ChildEntity c2 = new ChildEntity();
			c2.setId(2L);
			c2.setParent(entity);
			c2.setName("child2");

			s.save(entity);
			s.save(c1);
			s.save(c2);

		} finally {
			t.commit();
			s.close();
		}
	}

	/**
	 * Test PersistentSet lazyness in general.
	 */
	@Test
	public void testPersistentSetLaziness() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final SimpleEntity entity = s.load(SimpleEntity.class, 1L);
			final AccessiblePersistentSet children = (AccessiblePersistentSet) entity.getChildren();

			assertFalse("Extra lazy collection should not yet be initialized.", children.isInitialized());

			assertEquals(2, children.size());

			assertFalse("Extra lazy collection should still not be initialized.", children.isInitialized());

			children.stream().findAny();

			assertTrue("Extra lazy collection should be initialized.", children.isInitialized());
		} finally {
			t.rollback();
			s.close();
		}
	}

	/**
	 * Test that the collection field {@link SimpleEntity#children} is not
	 * initialized after load. It should be the the HashSet from instance
	 * construction until invocation of {@link SimpleEntity#getChildren()},
	 * where the PersistentCollection is instantiated.
	 */
	@Test
	public void testCollectionFieldNotInitializedAfterLoad() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final SimpleEntity entity = s.load(SimpleEntity.class, 1L);
			assertEquals("As children collection is not initialized, it is expected to be a HashSet", HashSet.class,
					entity.children.getClass());
		} finally {
			t.rollback();
			s.close();
		}
	}

	/**
	 * Test that the PersistentCollection if present on field
	 * {@link SimpleEntity#children} after load is not read in any way.
	 */
	@Test
	public void testLazyCollectionNotReadAfterLoad() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final SimpleEntity entity = s.load(SimpleEntity.class, 1L);
			final Set<ChildEntity> children = entity.children;

			if (children instanceof AccessiblePersistentSet) {
				// if entity.children is an AccessiblePersistentSet directly
				// after load for whatever reason, make sure it had not yet been
				// read in any way.
				assertFalse("Children collection was initialized on parent entity load although considered lazy.",
						((AccessiblePersistentSet) children).isInitialized());
				assertEquals(
						"Children collection size was queried on parent entity load."
								+ "\nWhile not as bad as full initialization,"
								+ "\nthis is still considered harmful in terms of performance.",
						-1, ((AccessiblePersistentSet) children).getCachedSize());
			}
		} finally {
			t.rollback();
			s.close();
		}
	}

	@Test
	public void testCollectionPropertyLazinessConfiguration() {
		final SessionFactoryImplementor sf = sessionFactory();
		final MetamodelImplementor mm = sf.getMetamodel();
		final EntityPersister simpleEntityPersister = mm.entityPersister(SimpleEntity.class);
		final boolean[] lazyness = simpleEntityPersister.getPropertyLaziness();
		final String[] names = simpleEntityPersister.getPropertyNames();
		int match = -1;
		// find index of children property
		for (int i = 0; i < names.length; i++) {
			if ("children".equals(names[i])) {
				match = i;
				break;
			}
		}

		assertTrue("Property 'children' not found in persister.", match >= 0);

		assertTrue("OneToMany Collection Property 'children' expected to be lazy.", lazyness[match]);
	}

	public static class CustomEnhancerTestContext extends EnhancerTestContext {
		@Override
		public boolean doBiDirectionalAssociationManagement(final UnloadedField field) {
			return false;
		}

		@Override
		public boolean doExtendedEnhancement(final UnloadedClass classDescriptor) {
			return false;
		}
	}
}