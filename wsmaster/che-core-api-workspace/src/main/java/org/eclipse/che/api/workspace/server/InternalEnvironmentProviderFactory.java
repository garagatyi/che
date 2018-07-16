/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.workspace.server;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironmentFactory;
import org.eclipse.che.api.workspace.server.wsnext.InternalEnvironmentConverter;
import org.eclipse.che.api.workspace.server.wsnext.SidecarBasedInternalEnvironmentProvider;
import org.eclipse.che.api.workspace.server.wsnext.WorkspaceNextApplier;
import org.eclipse.che.api.workspace.server.wsnext.WorkspaceNextObjectsRetriever;

/**
 * Returns an implementation of {@link InternalEnvironmentProvider} depending on a toggle
 * configuration.
 *
 * @author Oleksandr Garagatyi
 */
public class InternalEnvironmentProviderFactory implements Provider<InternalEnvironmentProvider> {

  private final Toggles toggles;
  private final InternalEnvironmentProvider classicProvider;
  private final SidecarBasedInternalEnvironmentProvider sidecarBasedProvider;

  @Inject
  public InternalEnvironmentProviderFactory(
      Toggles toggles,
      Map<String, WorkspaceNextApplier> workspaceNextAppliers,
      WorkspaceNextObjectsRetriever workspaceNextObjectsRetriever,
      Map<String, InternalEnvironmentFactory> environmentFactories,
      InternalEnvironmentConverter environmentConverter) {
    this.toggles = toggles;
    this.classicProvider = new InternalEnvironmentProvider(environmentFactories);
    this.sidecarBasedProvider =
        new SidecarBasedInternalEnvironmentProvider(
            workspaceNextAppliers,
            workspaceNextObjectsRetriever,
            environmentFactories,
            environmentConverter);
  }

  @Override
  public InternalEnvironmentProvider get() {
    if (toggles.isEnabled("sidecar_tooling")) {
      return sidecarBasedProvider;
    } else {
      return classicProvider;
    }
  }
}
