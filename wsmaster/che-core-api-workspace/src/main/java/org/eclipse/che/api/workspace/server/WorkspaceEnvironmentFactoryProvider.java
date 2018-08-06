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
package org.eclipse.che.api.workspace.server;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import org.eclipse.che.api.workspace.server.sidecartooling.ChePluginsApplier;
import org.eclipse.che.api.workspace.server.sidecartooling.ChePluginsBasedWorkspaceEnvironmentFactory;
import org.eclipse.che.api.workspace.server.sidecartooling.ChePluginsRetriever;
import org.eclipse.che.api.workspace.server.sidecartooling.InternalEnvironmentConverter;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironmentFactory;

/**
 * Returns an implementation of {@link WorkspaceEnvironmentFactory} depending on a toggle
 * configuration.
 *
 * @author Oleksandr Garagatyi
 */
public class WorkspaceEnvironmentFactoryProvider implements Provider<WorkspaceEnvironmentFactory> {

  public static final String CHE_PLUGINS_TOOLING_TOGGLE = "plugins_tooling";
  private final Toggles toggles;
  private final WorkspaceEnvironmentFactory classicProvider;
  private final ChePluginsBasedWorkspaceEnvironmentFactory sidecarBasedProvider;

  @Inject
  public WorkspaceEnvironmentFactoryProvider(
      Toggles toggles,
      Map<String, ChePluginsApplier> workspaceNextAppliers,
      ChePluginsRetriever workspaceNextObjectsRetriever,
      Map<String, InternalEnvironmentFactory> environmentFactories,
      InternalEnvironmentConverter environmentConverter) {
    this.toggles = toggles;
    this.classicProvider = new WorkspaceEnvironmentFactory(environmentFactories);
    this.sidecarBasedProvider =
        new ChePluginsBasedWorkspaceEnvironmentFactory(
            workspaceNextAppliers,
            workspaceNextObjectsRetriever,
            environmentFactories,
            environmentConverter);
  }

  @Override
  public WorkspaceEnvironmentFactory get() {
    if (toggles.isEnabled(CHE_PLUGINS_TOOLING_TOGGLE)) {
      return sidecarBasedProvider;
    } else {
      return classicProvider;
    }
  }
}
