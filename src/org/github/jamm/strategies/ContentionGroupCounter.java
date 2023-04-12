package org.github.jamm.strategies;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Utility class for counting Contention groups.
 *
 */
final class ContentionGroupsCounter
{
    private static final String ANONYMOUS_GROUP = "";

    /**
     * The number of anonymous groups (each counting as one) 
     */
    private int anonymousCounter;

    /**
     * The non-anonymous groups (each one counting as only one) 
     */
    private Set<String> contentionGroups = emptySet();

    /**
     * Adds a group tag to this counter
     * @param contentionGroupTag the contention group tag of a field
     */
    public void add(String contentionGroupTag)
    {
        if (ANONYMOUS_GROUP.equals(contentionGroupTag)) {
            this.anonymousCounter++;
        } else {
            if (contentionGroups.isEmpty())
                this.contentionGroups = new HashSet<>();
            this.contentionGroups.add(contentionGroupTag);
        }
    }

    /**
     * Returns the total number of groups.
     * 
     * @return the total number of groups
     */
    public int count() {
        return anonymousCounter + contentionGroups.size();
    }

    @Override
    public String toString()
    {
        return "ContentionGroupsCounter [anonymousCounter=" + anonymousCounter + ", contentionGroups="
                + contentionGroups + "]";
    }
}
