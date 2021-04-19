/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2020-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.opendistro.opensearch.performanceanalyzer.decisionmaker.actions;


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
