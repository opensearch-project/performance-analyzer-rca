/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;


import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ImpactVectorTest {

    @Test
    public void testInit() {
        ImpactVector impactVector = new ImpactVector();
        Map<ImpactVector.Dimension, ImpactVector.Impact> impact = impactVector.getImpact();
        impact.forEach((k, v) -> Assert.assertEquals(impact.get(k), ImpactVector.Impact.NO_IMPACT));
    }

    @Test
    public void testUpdateImpact() {
        ImpactVector impactVector = new ImpactVector();

        impactVector.increasesPressure(
                ImpactVector.Dimension.HEAP,
                ImpactVector.Dimension.CPU,
                ImpactVector.Dimension.DISK);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.HEAP),
                ImpactVector.Impact.INCREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.CPU),
                ImpactVector.Impact.INCREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.RAM),
                ImpactVector.Impact.NO_IMPACT);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.DISK),
                ImpactVector.Impact.INCREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.NETWORK),
                ImpactVector.Impact.NO_IMPACT);

        impactVector.decreasesPressure(ImpactVector.Dimension.RAM, ImpactVector.Dimension.NETWORK);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.HEAP),
                ImpactVector.Impact.INCREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.CPU),
                ImpactVector.Impact.INCREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.RAM),
                ImpactVector.Impact.DECREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.DISK),
                ImpactVector.Impact.INCREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.NETWORK),
                ImpactVector.Impact.DECREASES_PRESSURE);

        impactVector.noImpact(ImpactVector.Dimension.DISK, ImpactVector.Dimension.RAM);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.HEAP),
                ImpactVector.Impact.INCREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.CPU),
                ImpactVector.Impact.INCREASES_PRESSURE);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.RAM),
                ImpactVector.Impact.NO_IMPACT);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.DISK),
                ImpactVector.Impact.NO_IMPACT);
        Assert.assertEquals(
                impactVector.getImpact().get(ImpactVector.Dimension.NETWORK),
                ImpactVector.Impact.DECREASES_PRESSURE);
    }
}
