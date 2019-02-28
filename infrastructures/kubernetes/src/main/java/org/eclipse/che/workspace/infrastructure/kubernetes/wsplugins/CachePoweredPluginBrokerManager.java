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

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.wsplugins.model.ChePlugin;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginMeta;
import org.eclipse.che.commons.annotation.Traced;
import org.eclipse.che.workspace.infrastructure.kubernetes.StartSynchronizer;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.slf4j.Logger;

/**
 * @author Alexander Garagatyi
 */
@Singleton
public class CachePoweredPluginBrokerManager<E extends KubernetesEnvironment> implements
    PluginBrokerManager<E> {

  private static final Logger LOG = getLogger(CachePoweredPluginBrokerManager.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final PluginBrokerManagerImpl<E> brokerManager;

  // serialized plugin meta to plugins config
  // use serialized plugin instead of plugin ID:version to prevent in-place changes
  // of a plugin in Che plugin registry
  private Map<String, ChePlugin> pluginsCache = new ConcurrentHashMap<>();

  @Inject
  public CachePoweredPluginBrokerManager(PluginBrokerManagerImpl<E> brokerManager) {
    this.brokerManager = brokerManager;
  }

  @Beta
  @Traced
  public List<ChePlugin> getTooling(
      RuntimeIdentity runtimeID,
      StartSynchronizer startSynchronizer,
      Collection<PluginMeta> pluginsMeta,
      boolean isEphemeral) throws InfrastructureException {

    List<PluginMeta> nonCachedMetas = new ArrayList<>();
    List<ChePlugin> cached = new ArrayList<>();

    for (PluginMeta meta : pluginsMeta) {
      String metaJson = toJson(meta);
      ChePlugin plugin = pluginsCache.get(metaJson);
      if (plugin == null) {
        nonCachedMetas.add(meta);
      } else {
        cached.add(plugin);
      }
    }
// TODO do not mix cached and non-cached configs since configs evaluation also download binaries
    // check that all the plugins were provisioned in the WS since we need binaries there
    List<ChePlugin> nonCachedPlugins = Collections.emptyList();
    if (!nonCachedMetas.isEmpty()) {
      nonCachedPlugins = brokerManager.getTooling(runtimeID,
          startSynchronizer,
          nonCachedMetas,
          isEphemeral);

      for (ChePlugin nonCachedPlugin : nonCachedPlugins) {
        for (PluginMeta nonCachedMeta : nonCachedMetas) {
          if (nonCachedMeta.getId().equals(nonCachedPlugin.getId()) &&
          nonCachedMeta.getVersion().equals(nonCachedPlugin.getVersion())) {
            String metaJson = toJson(nonCachedMeta);

            pluginsCache.put(metaJson, nonCachedPlugin);
          }
        }
      }
    }

    return Stream.concat(nonCachedPlugins.stream(), cached.stream()).collect(Collectors.toList());
  }

  private String toJson(PluginMeta meta) {
    try {
      return objectMapper.writeValueAsString(meta);
    } catch (JsonProcessingException e) {
      LOG.error("Serializing PluginMeta failed. Error: " + e.getMessage(), e);
    }
    return "";
  }

  public static class CacheNeededProvider {

    public boolean get(RuntimeIdentity runtimeIdentity) {
      // TODO set using property
      return true;
    }
  }

  // IDEAS:
  // If we cash serialized objects instead of IDs it would be easier to reflect any changes.
  // Second step would be to declare plugins metas immutable and check only IDs.
  // Use toggle to introduce it. Using provider in rh-che we can make it per-user.

  // Implementation:
  // HERE
  // sort plugins metas list
  // serialize metas list
  // get base64 of plugins metas list
  // if environment.getAttributes().get(CachedMetasAttributeConstant) == base64
  //    return deserialize(environment.getAttributes().get(CachedPluginsAttributeConstant))

  // On runtime start:
  // add setting of base64 of metas and plugins before workspace start:
  // serialize plugins, sort and serialize metas
  // base64 plugins and metas
  // set into workspace
  // Even when workspace didn't start properly
}
