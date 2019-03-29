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
package org.eclipse.che.api.workspace.server.wsplugins;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalInfrastructureException;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginFQN;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginMeta;
import org.eclipse.che.api.workspace.shared.Constants;
import org.eclipse.che.commons.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches Che tooling plugin objects corresponding to attributes of a workspace config.
 *
 * <p>This API is in <b>Beta</b> and is subject to changes or removal.
 *
 * @author Oleksander Garagatyi
 */
@Beta
public class PluginMetaRetriever {

  private static final Logger LOG = LoggerFactory.getLogger(PluginMetaRetriever.class);
  private static final String CHE_PLUGIN_OBJECT_ERROR =
      "Che plugin '%s:%s' configuration is invalid. %s";
  private static final String CHE_REGISTRY_MISSING_ERROR =
      String.format(
          "Workspace requested Che Editor/Plugins but plugin registry is not configured. "
              + "Property %s should be set for to allow Editors and Plugins",
          Constants.CHE_WORKSPACE_PLUGIN_REGISTRY_URL_PROPERTY);

  private static final ObjectMapper YAML_PARSER = new ObjectMapper(new YAMLFactory());

  private static final String REGISTRY_AND_PUBLISHER_GROUP = "registryAndPublisher";
  private static final String VERSION_GROUP = "version";
  private static final String ID_GROUP = "id";
  private static final String REGISTRY_GROUP = "registry";
  private static final String PUBLISHER_GROUP = "publisher";
  private static final Pattern pluginPattern = Pattern.compile(
      "(?<" + REGISTRY_AND_PUBLISHER_GROUP + ">\\w+/)?(?<" + ID_GROUP + ">\\w+):(?<" + VERSION_GROUP
          + ">[\\w.]+)");
  private static final Pattern registryPublisherPattern = Pattern
      .compile("(?<registry>http.?//[^?#]+)?(?<publisher>\\w+/)");

  private final UriBuilder pluginRegistry;

  @Inject
  public PluginMetaRetriever(
      @Nullable @Named(Constants.CHE_WORKSPACE_PLUGIN_REGISTRY_URL_PROPERTY)
          String pluginRegistry) {
    if (pluginRegistry == null) {
      LOG.info(
          format(
              "Che tooling plugins feature is disabled - Che plugin registry API endpoint property '%s' is not configured",
              Constants.CHE_WORKSPACE_PLUGIN_REGISTRY_URL_PROPERTY));
      this.pluginRegistry = null;
    } else {
      this.pluginRegistry = UriBuilder.fromUri(pluginRegistry).path("plugins");
    }
  }

  /**
   * Gets Che tooling plugins list from provided workspace config attributes, fetches corresponding
   * meta objects from Che plugin registry and returns list of {@link PluginMeta} with meta
   * information about plugins in a workspace.
   *
   * <p>This API is in <b>Beta</b> and is subject to changes or removal.
   *
   * @param attributes workspace config attributes
   * @throws InfrastructureException when attributes contain invalid Che plugins entries or Che
   *     plugin meta files retrieval from Che plugin registry fails or returns invalid data
   */
  @Beta
  public Collection<PluginMeta> get(Map<String, String> attributes) throws InfrastructureException {
    if (attributes == null) {
      return emptyList();
    }

    // Have to check for empty value instead of plain null as it's possible to have empty
    // plugins attribute
    String pluginsAttribute =
        attributes.getOrDefault(Constants.WORKSPACE_TOOLING_PLUGINS_ATTRIBUTE, null);
    String editorAttribute =
        attributes.getOrDefault(Constants.WORKSPACE_TOOLING_EDITOR_ATTRIBUTE, null);

    // Check if any plugins/editor is in workspace
    if (Strings.isNullOrEmpty(editorAttribute) && Strings.isNullOrEmpty(pluginsAttribute)) {
      return emptyList();
    }

    // If workspace plugins or editor are not null, but registry is null, throw error
    if (pluginRegistry == null) {
      throw new InfrastructureException(CHE_REGISTRY_MISSING_ERROR);
    }

    List<PluginFQN> metaFQNs = new ArrayList<>();
    if (!isNullOrEmpty(pluginsAttribute)) {
      metaFQNs.addAll(parsePluginFQNs(pluginsAttribute));
    }
    if (!isNullOrEmpty(editorAttribute)) {
      Collection<PluginFQN> editorsFQNs = parsePluginFQNs(editorAttribute);
      if (editorsFQNs.size() > 1) {
        throw new InfrastructureException(
            "Multiple editors found in workspace config attributes. "
                + "It is not supported. Please, use one editor only.");
      }
      metaFQNs.addAll(editorsFQNs);
    }

    return getMetas(metaFQNs);
  }

  private Collection<PluginFQN> parsePluginFQNs(String pluginsAttribute)
      throws InfrastructureException {
    String[] plugins = pluginsAttribute.split(" *, *");
    if (plugins.length == 0) {
      return Collections.emptyList();
    }
    List<PluginFQN> collectedFQNs = new ArrayList<>();
    for (String plugin : plugins) {
      PluginFQN pFQN = parsePlugin(plugin);
      if (collectedFQNs
          .stream()
          .anyMatch(
              p -> p.getId().equals(pFQN.getId()) && p.getVersion().equals(pFQN.getVersion()))) {
        throw new InfrastructureException(
            format(
                "Invalid Che tooling plugins configuration: plugin %s is duplicated",
                pFQN.getId() + ":" + pFQN.getVersion())); // even if different repos
      }
      collectedFQNs.add(pFQN);
    }
    return collectedFQNs;
  }

  private PluginFQN parsePlugin(String plugin) throws InfrastructureException {
    Matcher matcher = pluginPattern.matcher(plugin);
    String registryAndPublisher = matcher.group(REGISTRY_AND_PUBLISHER_GROUP);
    String id = matcher.group(ID_GROUP);
    String version = matcher.group(VERSION_GROUP);
    String publisher = null;
    URI registry = null;

    if (!isNullOrEmpty(registryAndPublisher)) {
      // when old notation `registryURL/pluign_ID:version` is used
      // treat last `/plugins` from registryURL as publisher_ID in new
      // notation `registryURL:/publisher_ID/plugin_ID:version`
      Matcher registryPublisherMatcher = registryPublisherPattern.matcher(registryAndPublisher);
      publisher = registryPublisherMatcher.group(PUBLISHER_GROUP);
      String registryGroup = registryPublisherMatcher.group(REGISTRY_GROUP);
      if (!isNullOrEmpty(registryGroup)) {
        try {
          registry = new URI(registryGroup);
        } catch (URISyntaxException e) {
          throw new InfrastructureException(
              format("Plugin registry URL '%s' is incorrect. Problematic plugin entry: '%s'", registryGroup, plugin));
        }
      }
    }

    return new PluginFQN(registry, id, version, publisher);
  }

  private Collection<PluginMeta> getMetas(List<PluginFQN> pluginFQNs)
      throws InfrastructureException {
    ArrayList<PluginMeta> metas = new ArrayList<>();
    for (PluginFQN pluginFqn : pluginFQNs) {
      metas.add(getMeta(pluginFqn));
    }

    return metas;
  }

  private PluginMeta getMeta(PluginFQN pluginFQN) throws InfrastructureException {
    final String id = pluginFQN.getId();
    final String version = pluginFQN.getVersion();
    try {
      UriBuilder metaURIBuilder =
          pluginFQN.getRegistry() == null
              ? pluginRegistry.clone()
              : UriBuilder.fromUri(pluginFQN.getRegistry());

      if (!isNullOrEmpty(pluginFQN.getPublisher())) {
        metaURIBuilder.path(pluginFQN.getPublisher());
      }
      URI metaURI = metaURIBuilder.path(id).path(version).path("meta.yaml").build();
      PluginMeta meta = getBody(metaURI, PluginMeta.class);
      validateMeta(meta, id, version);
      return meta;
    } catch (IllegalArgumentException | UriBuilderException | MalformedURLException e) {
      throw new InternalInfrastructureException(
          format(
              "Metadata of plugin %s:%s retrieval failed. Error is %s",
              id, version, e.getMessage()));
    } catch (IOException e) {
      throw new InfrastructureException(
          format(
              "Error occurred on retrieval of plugin %s. Error: %s",
              id + ':' + version, e.getMessage()));
    }
  }

  @VisibleForTesting
  void validateMeta(PluginMeta meta, String id, String version) throws InfrastructureException {
    requireNotNullNorEmpty(meta.getId(), CHE_PLUGIN_OBJECT_ERROR, id, version, "ID is missing.");
    requireEqual(
        id,
        meta.getId(),
        "Plugin id in attribute doesn't match plugin metadata. Plugin object seems broken.");
    requireNotNullNorEmpty(
        meta.getVersion(), CHE_PLUGIN_OBJECT_ERROR, id, version, "Version is missing.");
    requireEqual(
        version,
        meta.getVersion(),
        "Plugin version in workspace config attributes doesn't match plugin metadata. Plugin object seems broken.");
    requireNotNullNorEmpty(
        meta.getName(), CHE_PLUGIN_OBJECT_ERROR, id, version, "Name is missing.");
    requireNotNullNorEmpty(
        meta.getType(), CHE_PLUGIN_OBJECT_ERROR, id, version, "Type is missing.");
  }

  @VisibleForTesting
  protected <T> T getBody(URI uri, Class<T> clas) throws IOException {
    HttpURLConnection httpURLConnection = null;
    try {
      httpURLConnection = (HttpURLConnection) uri.toURL().openConnection();

      int responseCode = httpURLConnection.getResponseCode();
      if (responseCode != 200) {
        throw new IOException(
            format(
                "Can't get object by URI '%s'. Error: %s",
                uri.toString(), getError(httpURLConnection)));
      }

      return parseYamlResponseStreamAndClose(httpURLConnection.getInputStream(), clas);
    } finally {
      if (httpURLConnection != null) {
        httpURLConnection.disconnect();
      }
    }
  }

  private String getError(HttpURLConnection httpURLConnection) throws IOException {
    try (InputStreamReader isr = new InputStreamReader(httpURLConnection.getInputStream())) {
      return CharStreams.toString(isr);
    }
  }

  protected <T> T parseYamlResponseStreamAndClose(InputStream inputStream, Class<T> clazz)
      throws IOException {
    try (InputStreamReader reader = new InputStreamReader(inputStream)) {
      return YAML_PARSER.readValue(reader, clazz);
    } catch (IOException e) {
      throw new IOException(
          "Internal server error. Unexpected response body received from Che plugin registry API."
              + e.getLocalizedMessage(),
          e);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void requireNotNullNorEmpty(String s, String error, String... errorArgs)
      throws InfrastructureException {
    if (s == null || s.isEmpty()) {
      throw new InfrastructureException(format(error, (Object[]) errorArgs));
    }
  }

  private void requireEqual(String version, String version1, String error, String... errorArgs)
      throws InfrastructureException {
    if (!version.equals(version1)) {
      throw new InfrastructureException(format(error, (Object[]) errorArgs));
    }
  }
}
