/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator.linux;


import org.opensearch.performanceanalyzer.collectors.NetInterfaceSummary;
import org.opensearch.performanceanalyzer.hwnet.NetworkInterface;
import org.opensearch.performanceanalyzer.metrics_generator.IPMetricsGenerator;

public class LinuxIPMetricsGenerator implements IPMetricsGenerator {

    private NetInterfaceSummary inNetInterfaceSummary;
    private NetInterfaceSummary outNetInterfaceSummary;

    @Override
    public double getInPacketRate4() {

        return inNetInterfaceSummary.getPacketRate4();
    }

    @Override
    public double getOutPacketRate4() {

        return outNetInterfaceSummary.getPacketRate4();
    }

    @Override
    public double getInDropRate4() {

        return inNetInterfaceSummary.getDropRate4();
    }

    @Override
    public double getOutDropRate4() {

        return outNetInterfaceSummary.getDropRate4();
    }

    @Override
    public double getInPacketRate6() {

        return inNetInterfaceSummary.getPacketRate6();
    }

    @Override
    public double getOutPacketRate6() {

        return outNetInterfaceSummary.getPacketRate6();
    }

    @Override
    public double getInDropRate6() {

        return inNetInterfaceSummary.getDropRate6();
    }

    @Override
    public double getOutDropRate6() {

        return outNetInterfaceSummary.getDropRate6();
    }

    @Override
    public double getInBps() {

        return inNetInterfaceSummary.getBps();
    }

    @Override
    public double getOutBps() {

        return outNetInterfaceSummary.getBps();
    }

    @Override
    public void addSample() {

        NetworkInterface.addSample();
    }

    public void setInNetworkInterfaceSummary(final NetInterfaceSummary netInterfaceSummary) {

        this.inNetInterfaceSummary = netInterfaceSummary;
    }

    public void setOutNetworkInterfaceSummary(final NetInterfaceSummary netInterfaceSummary) {

        this.outNetInterfaceSummary = netInterfaceSummary;
    }
}
