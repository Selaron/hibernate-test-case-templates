package org.hibernate.bugs;

import org.hibernate.testing.TestForIssue;

/**
 * 
 * Test with Hibernate XML Mapping file and normally Lazy OneToMany collection.
 * 
 * @author Selaron
 *
 */
@TestForIssue(jiraKey = "HHH-12648")
public class DirtyTrackingLazyPersistentSetWithXmlMappingTest extends DirtyTrackingPersistentSetTest {

	@Override
	protected String[] getMappings() {
		return new String[] { "../bugs/MappingsLazy.hbm.xml" };
	}

}
