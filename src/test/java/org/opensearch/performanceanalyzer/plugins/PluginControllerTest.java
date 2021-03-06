/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.plugins;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ActionListener;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.Publisher;

public class PluginControllerTest {

    @Test
    public void testInit() {
        List<Class<? extends Plugin>> frameworkPlugins =
                new ArrayList<Class<? extends Plugin>>() {
                    {
                        add(TestActionListener.class);
                        add(TestPlugin.class);
                    }
                };
        PluginControllerConfig pluginControllerConfig = Mockito.mock(PluginControllerConfig.class);
        Mockito.when(pluginControllerConfig.getFrameworkPlugins()).thenReturn(frameworkPlugins);
        Publisher publisher = Mockito.mock(Publisher.class);
        PluginController pluginController = new PluginController(pluginControllerConfig, publisher);
        pluginController.initPlugins();

        List<Plugin> plugins = pluginController.getPlugins();
        assertEquals(2, plugins.size());

        // Only action listeners registered with publisher
        Mockito.verify(publisher, times(1)).addActionListener(any());
        Mockito.verify(publisher, times(1)).addActionListener(isA(TestActionListener.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testPrivateConstructorPlugin() {
        List<Class<? extends Plugin>> frameworkPlugins =
                new ArrayList<Class<? extends Plugin>>() {
                    {
                        add(TestPrivateConstructorPlugin.class);
                    }
                };
        PluginControllerConfig pluginControllerConfig = Mockito.mock(PluginControllerConfig.class);
        Mockito.when(pluginControllerConfig.getFrameworkPlugins()).thenReturn(frameworkPlugins);
        Publisher publisher = Mockito.mock(Publisher.class);
        PluginController pluginController = new PluginController(pluginControllerConfig, publisher);
        pluginController.initPlugins();
    }

    @Test(expected = IllegalStateException.class)
    public void testMultiConstructorPlugin() {
        List<Class<? extends Plugin>> frameworkPlugins =
                new ArrayList<Class<? extends Plugin>>() {
                    {
                        add(TestMultiConstructorPlugin.class);
                    }
                };
        PluginControllerConfig pluginControllerConfig = Mockito.mock(PluginControllerConfig.class);
        Mockito.when(pluginControllerConfig.getFrameworkPlugins()).thenReturn(frameworkPlugins);
        Publisher publisher = Mockito.mock(Publisher.class);
        PluginController pluginController = new PluginController(pluginControllerConfig, publisher);
        pluginController.initPlugins();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonDefaultConstructorPlugin() {
        List<Class<? extends Plugin>> frameworkPlugins =
                new ArrayList<Class<? extends Plugin>>() {
                    {
                        add(TestNonDefaultConstructorPlugin.class);
                    }
                };
        PluginControllerConfig pluginControllerConfig = Mockito.mock(PluginControllerConfig.class);
        Mockito.when(pluginControllerConfig.getFrameworkPlugins()).thenReturn(frameworkPlugins);
        Publisher publisher = Mockito.mock(Publisher.class);
        PluginController pluginController = new PluginController(pluginControllerConfig, publisher);
        pluginController.initPlugins();
    }

    public static class TestActionListener extends Plugin implements ActionListener {

        @Override
        public void actionPublished(Action action) {
            assert true;
        }

        @Override
        public String name() {
            return "Test_Action_Listener";
        }
    }

    public static class TestPlugin extends Plugin {

        @Override
        public String name() {
            return "Test_Plugin";
        }
    }

    public static class TestPrivateConstructorPlugin extends Plugin {

        private TestPrivateConstructorPlugin() {
            assert true;
        }

        @Override
        public String name() {
            return "Test_Private_Constructor_Plugin";
        }
    }

    public static class TestMultiConstructorPlugin extends Plugin {

        private String name;

        public TestMultiConstructorPlugin() {
            this.name = "Test_Multi_Constructor_Plugin";
        }

        public TestMultiConstructorPlugin(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

    public static class TestNonDefaultConstructorPlugin extends Plugin {

        private String name;

        public TestNonDefaultConstructorPlugin(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }
}
