/*
 * Copyright (c) 2023. New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.agent.android

import com.newrelic.agent.android.agp4.AGP4Adapter
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.compile.JavaCompile
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class VariantAdapterTest extends PluginTest {

    BuildHelper buildHelper
    VariantAdapter variantAdapter
    NewRelicExtension ext

    // Applying the NR plugin creates a full task model, so we can't create providers, for instance
    VariantAdapterTest() {
        super(true)
    }

    @BeforeEach
    void setup() {
        if (applyPlugin) {
            ext = plugin.pluginExtension
            buildHelper = Mockito.spy(plugin.buildHelper)
        } else {
            ext = NewRelicExtension.register(project)
            buildHelper = Mockito.spy(BuildHelper.register(project))
        }
        variantAdapter = buildHelper.variantAdapter
    }

    @Test
    void getVariants() {
        variantAdapter.configure(ext)
        Assert.assertFalse(variantAdapter.getVariantValues().isEmpty())
    }

    @Test
    void getVariantNames() {
        Assert.assertFalse(variantAdapter.getVariantNames().isEmpty())
        Assert.assertEquals(variantAdapter.getVariantNames(), variantAdapter.variants.get().keySet())
    }

    @Test
    void configure() {
        buildHelper.extension.excludeVariantInstrumentation("release", "staging")
        Assert.assertEquals(variantAdapter, variantAdapter.configure(ext))
        Assert.assertNull(variantAdapter.withVariant("release"))
        Assert.assertNotNull(variantAdapter.withVariant("debug"))
        Assert.assertNotNull(variantAdapter.withVariant("qa"))
    }

    @Test
    void getBuildTypeProvider() {
        variantAdapter.getVariantValues().each { variant ->
            Assert.assertEquals(variant.name, variantAdapter.getBuildTypeProvider(variant.name).get().name)
        }
    }

    @Test
    void getBuildTypeAsString() {
        variantAdapter.getVariantValues().each { variant ->
            Assert.assertEquals(variant.name, variantAdapter.getBuildTypeProvider(variant.name).get().name)
        }
    }

    @Test
    void getJavaCompileProvider() {
        variantAdapter.getVariantValues().each { variant ->
            def provider = variantAdapter.getJavaCompileProvider(variant.name)
            Assert.assertNotNull(provider)
            Assert.assertEquals(JavaCompile, provider.type)
        }
    }

    @Test
    void getConfigProvider() {
        variantAdapter.getVariantValues().each { variant ->
            def provider = variantAdapter.getConfigProvider(variant.name)
            Assert.assertNotNull(provider)
            Assert.assertEquals(NewRelicConfigTask, provider.type)
        }
    }

    @Test
    void getMappingFile() {
        variantAdapter.getVariantValues().each { variant ->
            def provider = variantAdapter.getMappingFileProvider(variant.name)
            Assert.assertTrue(provider instanceof RegularFileProperty)
            Assert.assertTrue(provider.get().asFile.absolutePath.startsWith(project.layout.buildDirectory.asFile.get().absolutePath))
        }
    }

    @Test
    void withVariant() {
        Assert.assertFalse(variantAdapter.variantValues.isEmpty())
        variantAdapter.getVariantNames().each { variantName ->
            Assert.assertNotNull(variantAdapter.withVariant(variantName))
        }
    }

    @Test
    void register() {
        Assert.assertNotNull(VariantAdapter.register(buildHelper))
        Assert.assertFalse(variantAdapter.variantValues.isEmpty())
    }

    @Test
    void registerOrNamed() {
        def registered = variantAdapter.getConfigProvider("release")
        Assert.assertNotNull(registered)
        Assert.assertEquals(NewRelicConfigTask, registered.type)

        def named = variantAdapter.getConfigProvider("release")
        Assert.assertNotNull(named)
        Assert.assertEquals(NewRelicConfigTask, named.type)

        Assert.assertEquals(registered, named)
    }

    @Test
    void getLogger() {
        Assert.assertEquals(buildHelper.logger, NewRelicGradlePlugin.LOGGER)
    }

    @Test
    void getVariantAdapterByGradleVersion() {
        buildHelper = Mockito.spy(BuildHelper.register(project))
        Mockito.doReturn("7.2").when(buildHelper).getAgpVersion()
        Mockito.doReturn("7.3.3").when(buildHelper).getGradleVersion()
        def adapter = VariantAdapter.register(buildHelper)
        Assert.assertTrue(adapter instanceof AGP4Adapter)

        buildHelper = Mockito.spy(BuildHelper.register(project))
        Mockito.doReturn("7.3").when(buildHelper).getAgpVersion()
        Mockito.doReturn("7.4").when(buildHelper).getGradleVersion()
        adapter = VariantAdapter.register(buildHelper)
        Assert.assertTrue(adapter instanceof AGP4Adapter)
    }

    @Test
    void getVariantAdapterByAGPVersion() {
        buildHelper = Mockito.spy(BuildHelper.register(project))
        Mockito.doReturn("7.3.3").when(buildHelper).getAgpVersion()
        def adapter = VariantAdapter.register(buildHelper)
        Assert.assertTrue(adapter instanceof AGP4Adapter)
    }

}