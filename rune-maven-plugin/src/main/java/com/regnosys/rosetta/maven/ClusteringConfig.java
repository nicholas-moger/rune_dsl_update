package com.regnosys.rosetta.maven;

/**
 * XML-mappable stand-in for {@code org.eclipse.xtext.maven.ClusteringConfig}.
 * The released 9.83.0 plugin exposes a {@code clusteringConfig} parameter that
 * tunes Xtext's resource-clustering memory management. The fork pipeline builds
 * the whole workspace in one pass and has no clustering stage, so the values
 * are accepted (a consumer pom carrying the block still parses) and ignored.
 * No measured consumer sets it (CDM 6.20.6 / DRR 6.34.1).
 */
public class ClusteringConfig {

    private long minimumFreeMemory;
    private int minimumClusterSize;
    private long minimumPercentFreeMemory;

    public long getMinimumFreeMemory() {
        return minimumFreeMemory;
    }

    public void setMinimumFreeMemory(long minimumFreeMemory) {
        this.minimumFreeMemory = minimumFreeMemory;
    }

    public int getMinimumClusterSize() {
        return minimumClusterSize;
    }

    public void setMinimumClusterSize(int minimumClusterSize) {
        this.minimumClusterSize = minimumClusterSize;
    }

    public long getMinimumPercentFreeMemory() {
        return minimumPercentFreeMemory;
    }

    public void setMinimumPercentFreeMemory(long minimumPercentFreeMemory) {
        this.minimumPercentFreeMemory = minimumPercentFreeMemory;
    }
}
