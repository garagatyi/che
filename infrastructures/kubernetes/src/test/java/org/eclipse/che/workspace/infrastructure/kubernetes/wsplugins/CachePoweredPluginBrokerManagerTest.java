/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.wsplugins;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEqualsNoOrder;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.RuntimeIdentityImpl;
import org.eclipse.che.api.workspace.server.wsplugins.model.ChePlugin;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginMeta;
import org.eclipse.che.workspace.infrastructure.kubernetes.StartSynchronizer;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * @author Alexander Garagatyi
 */
@Listeners(MockitoTestNGListener.class)
public class CachePoweredPluginBrokerManagerTest {
  @Mock
  private StartSynchronizer startSynchronizer;
  @Mock
  private PluginBrokerManagerImpl<KubernetesEnvironment> nonCachingPluginBrokerManager;

  private List<ChePlugin> defaultFallbackValues;
  private RuntimeIdentity runtimeIdentity;
  private CachePoweredPluginBrokerManager<KubernetesEnvironment> brokerManager;

  @BeforeMethod
  public void setUp() throws Exception {
    brokerManager = new CachePoweredPluginBrokerManager<>(nonCachingPluginBrokerManager);

    runtimeIdentity = new RuntimeIdentityImpl("WS_ID", "ENV_NAME", "USER_ID");
    defaultFallbackValues = asList(new ChePlugin().id("shouldNotAppear").version("1"),
        new ChePlugin().id("neitherShouldAppear").version("2"));
  }

  @Test
  public void shouldFallbackIfNotCached() throws Exception {
    // given
    List<PluginMeta> metas = new ArrayList<>();
    metas.add(new PluginMeta().id("metaId1").version("v1"));
    when(nonCachingPluginBrokerManager.getTooling(any(RuntimeIdentity.class),
        eq(startSynchronizer),
        anyCollection(),
        anyBoolean()))
        .thenReturn(defaultFallbackValues);

    // when
    List<ChePlugin> actual = brokerManager
        .getTooling(runtimeIdentity, startSynchronizer, metas, false);

    // then
    assertEqualsNoOrder(actual.toArray(), defaultFallbackValues.toArray());
  }

  @Test
  public void shouldReturnCachedPlugins() throws Exception {
    // given
    List<PluginMeta> metas = new ArrayList<>();
    metas.add(new PluginMeta().id("metaId1").version("v1"));
    List<ChePlugin> expected = new ArrayList<>();
    expected.add(new ChePlugin().id("metaId1").version("v1"));
    when(nonCachingPluginBrokerManager.getTooling(any(RuntimeIdentity.class),
        eq(startSynchronizer),
        anyCollection(),
        anyBoolean()))
        .thenReturn(singletonList(new ChePlugin().id("metaId1").version("v1")));
    // pre-cache
    brokerManager.getTooling(runtimeIdentity, startSynchronizer, metas, false);

    // when
    List<ChePlugin> actual = brokerManager
        .getTooling(runtimeIdentity, startSynchronizer, metas, false);

    // then
    assertEqualsNoOrder(actual.toArray(), expected.toArray());
  }

  @Test
  public void shouldIgnoreFallbackChangesWhenPluginIsCached() throws Exception {
    // given
    List<PluginMeta> metas = new ArrayList<>();
    metas.add(new PluginMeta().id("metaId1").version("v1"));
    List<ChePlugin> expected = new ArrayList<>();
    expected.add(new ChePlugin().id("metaId1").version("v1"));
    when(nonCachingPluginBrokerManager.getTooling(any(RuntimeIdentity.class),
        eq(startSynchronizer),
        anyCollection(),
        anyBoolean()))
        .thenReturn(asList(new ChePlugin().id("metaId1").version("v1"),
            new ChePlugin().id("metaId2").version("v2"),
            new ChePlugin().id("metaId3").version("v3")));
    // pre-cache
    brokerManager.getTooling(runtimeIdentity, startSynchronizer, metas, false);
    when(nonCachingPluginBrokerManager.getTooling(any(RuntimeIdentity.class),
        eq(startSynchronizer),
        anyCollection(),
        anyBoolean()))
        .thenReturn(singletonList(new ChePlugin().id("changedMetaId1").version("changedV1")));

    // when
    List<ChePlugin> actual = brokerManager
        .getTooling(runtimeIdentity, startSynchronizer, metas, false);

    // then
    assertEqualsNoOrder(actual.toArray(), expected.toArray());
  }
}
