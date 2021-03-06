/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 */
package org.identityconnectors.framework.impl.test;

import static org.identityconnectors.framework.impl.api.local.LocalConnectorInfoManagerImpl.createDefaultAPIConfiguration;
import static org.identityconnectors.framework.impl.api.local.LocalConnectorInfoManagerImpl.loadMessageCatalog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.identityconnectors.framework.impl.api.ConfigurationPropertiesImpl;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;
import org.identityconnectors.framework.impl.api.ConnectorMessagesImpl;
import org.identityconnectors.framework.impl.api.local.JavaClassProperties;
import org.identityconnectors.framework.impl.api.local.LocalConnectorInfoImpl;
import org.identityconnectors.framework.impl.api.local.operations.SearchImpl;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.spi.TestHelpersSpi;

public class TestHelpersImpl implements TestHelpersSpi {

    private static final Log LOG = Log.getLog(TestHelpersImpl.class);

    /**
     * Method for convenient testing of local connectors.
     */
    @Override
    public APIConfiguration createTestConfiguration(
            final Class<? extends Connector> clazz, final Configuration config) {

        final LocalConnectorInfoImpl info = new LocalConnectorInfoImpl();
        info.setConnectorConfigurationClass(config.getClass());
        info.setConnectorClass(clazz);
        info.setConnectorDisplayNameKey("DUMMY_DISPLAY_NAME");
        info.setConnectorKey(new ConnectorKey(clazz.getName() + ".bundle", "1.0", clazz.getName()));
        info.setMessages(createDummyMessages());
        try {
            final APIConfigurationImpl rv = createDefaultAPIConfiguration(info);
            rv.setConfigurationProperties(JavaClassProperties.createConfigurationProperties(config));

            info.setDefaultAPIConfiguration(rv);
            return rv;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    /**
     * Method for convenient testing of local connectors.
     */
    @Override
    public APIConfiguration createTestConfiguration(final Class<? extends Connector> clazz,
            final Set<String> bundleContents, final PropertyBag configData, final String prefix) {

        assert null != clazz;
        final ConnectorClass options = clazz.getAnnotation(ConnectorClass.class);
        assert null != options;
        final LocalConnectorInfoImpl info = new LocalConnectorInfoImpl();
        info.setConnectorClass(clazz);
        info.setConnectorConfigurationClass(options.configurationClass());
        info.setConnectorDisplayNameKey(options.displayNameKey());
        info.setConnectorCategoryKey(options.categoryKey());
        info.setConnectorKey(new ConnectorKey(clazz.getName() + ".bundle", "1.0", clazz.getName()));
        if (null == bundleContents || bundleContents.isEmpty()) {
            info.setMessages(createDummyMessages());
        } else {
            final ConnectorMessagesImpl messages =
                    loadMessageCatalog(bundleContents, clazz.getClassLoader(), info.getConnectorClass());
            info.setMessages(messages);
        }

        final APIConfigurationImpl impl = createDefaultAPIConfiguration(info);
        info.setDefaultAPIConfiguration(impl);

        final ConfigurationPropertiesImpl configProps = impl.getConfigurationProperties();

        final String fullPrefix = StringUtil.isBlank(prefix) ? null : prefix + ".";

        for (ConfigurationPropertyImpl property : configProps.getProperties()) {
            @SuppressWarnings("unchecked")
            Object value = configData.getProperty(
                    null == fullPrefix ? property.getName() : fullPrefix + property.getName(),
                    (Class<Object>) property.getType(), property.getValue());
            if (value != null) {
                property.setValue(value);
            }
        }
        return impl;
    }

    @Override
    public void fillConfiguration(final Configuration config, final Map<String, ? extends Object> configData) {
        final Map<String, Object> configDataCopy = new HashMap<String, Object>(configData);
        final ConfigurationPropertiesImpl configProps = JavaClassProperties.createConfigurationProperties(config);
        for (String propName : configProps.getPropertyNames()) {
            // Remove the entry from the config map, so that at the end
            // the map only contains entries that were not assigned to a config
            // property.
            final Object value = configDataCopy.remove(propName);
            if (value != null) {
                configProps.setPropertyValue(propName, value);
            }
        }
        // The config map now contains entries that were not assigned to a
        // config property.
        for (String propName : configDataCopy.keySet()) {
            LOG.warn("Configuration property {0} does not exist!", propName);
        }
        JavaClassProperties.mergeIntoBean(configProps, config);
    }

    /**
     * Performs a raw, unfiltered search at the SPI level, eliminating
     * duplicates from the result set.
     *
     * @param search The search SPI
     * @param objectClass The object class - passed through to connector so it may be null if the connecor allowing it
     * to be null. (This
     * is convenient for unit tests, but will not be the case in general)
     * @param filter The filter to search on
     * @param handler The result handler
     * @param options The options - may be null - will be cast to an empty OperationOptions
     */
    @Override
    public SearchResult search(final SearchOp<?> search, final ObjectClass objectClass,
            final Filter filter, final ResultsHandler handler, OperationOptions options) {

        if (options == null) {
            options = new OperationOptionsBuilder().build();
        }
        final AtomicReference<SearchResult> result = new AtomicReference<SearchResult>(null);

        SearchImpl.rawSearch(search, objectClass, filter, new SearchResultsHandler() {

            @Override
            public void handleResult(final SearchResult searchResult) {
                result.set(searchResult);
            }

            @Override
            public boolean handle(final ConnectorObject connectorObject) {
                return handler.handle(connectorObject);
            }
        }, options);
        return result.get();
    }

    @Override
    public ConnectorMessages createDummyMessages() {
        return new DummyConnectorMessages();
    }

    private static class DummyConnectorMessages implements ConnectorMessages {

        @Override
        public String format(final String key, final String dflt, final Object... args) {
            final StringBuilder builder = new StringBuilder();
            builder.append(key);
            builder.append(": ");
            String sep = "";
            for (Object arg : args) {
                builder.append(sep);
                builder.append(arg);
                sep = ", ";
            }
            return builder.toString();
        }
    }

}
