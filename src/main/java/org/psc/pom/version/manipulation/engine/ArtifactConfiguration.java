package org.psc.pom.version.manipulation.engine;

import java.util.function.Predicate;

public class ArtifactConfiguration {

	private Predicate<String> groupIdFilter;
	private Predicate<String> artifactIdFilter;
	private Predicate<String> versionFilter;

	/**
	 * @return the groupIdFilter
	 */
	public Predicate<String> getGroupIdFilter() {
		return groupIdFilter;
	}

	/**
	 * @param groupIdFilter
	 *            the groupIdFilter to set
	 */
	public void setGroupIdFilter(Predicate<String> groupIdFilter) {
		this.groupIdFilter = groupIdFilter;
	}

	/**
	 * @return the artifactIdFilter
	 */
	public Predicate<String> getArtifactIdFilter() {
		return artifactIdFilter;
	}

	/**
	 * @param artifactIdFilter
	 *            the artifactIdFilter to set
	 */
	public void setArtifactIdFilter(Predicate<String> artifactIdFilter) {
		this.artifactIdFilter = artifactIdFilter;
	}

	/**
	 * @return the versionFilter
	 */
	public Predicate<String> getVersionFilter() {
		return versionFilter;
	}

	/**
	 * @param versionFilter
	 *            the versionFilter to set
	 */
	public void setVersionFilter(Predicate<String> versionFilter) {
		this.versionFilter = versionFilter;
	}

}
