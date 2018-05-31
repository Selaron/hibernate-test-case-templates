package org.hibernate.bugs;

import org.hibernate.testing.TestForIssue;

/**
 * 
 * Test with Hibernate XML Mapping file and Extra Lazy OneToMany collection.
 * 
 * @author Selaron
 *
 */
@TestForIssue(jiraKey = "HHH-12648")
public class DirtyTrackingExtraLazyPersistentSetWithXmlMappingTest extends DirtyTrackingPersistentSetTest {

	@Override
	protected String[] getMappings() {
		return new String[] { "../bugs/MappingsExtraLazy.hbm.xml" };
	}

}
