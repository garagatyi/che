/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.workspace.server.sidecartooling;

import java.util.Collection;
import java.util.Map;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.config.Environment;
import org.eclipse.che.api.workspace.server.WorkspaceEnvironmentFactory;
import org.eclipse.che.api.workspace.server.sidecartooling.model.ChePlugin;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironmentFactory;

/**
 * Creates {@link InternalEnvironment} from provided environment configuration, then converts it
 * using {@link InternalEnvironmentConverter} and applies {@link ChePluginsApplier} to the resulting
 * {@code InternalEnvironment} to add sidecar tooling
 *
 * @author Oleksandr Garagatyi
 */
public class ChePluginsBasedWorkspaceEnvironmentFactory extends WorkspaceEnvironmentFactory {
  private final Map<String, ChePluginsApplier> chePluginsAppliers;
  private final ChePluginsRetriever chePluginsRetriever;
  private final InternalEnvironmentConverter environmentConverter;

  public ChePluginsBasedWorkspaceEnvironmentFactory(
      Map<String, ChePluginsApplier> chePluginsAppliers,
      ChePluginsRetriever chePluginsRetriever,
      Map<String, InternalEnvironmentFactory> environmentFactories,
      InternalEnvironmentConverter environmentConverter) {
    super(environmentFactories);
    this.chePluginsAppliers = chePluginsAppliers;
    this.chePluginsRetriever = chePluginsRetriever;
    this.environmentConverter = environmentConverter;
  }

  @Override
  public InternalEnvironment create(
      Environment environment, Map<String, String> workspaceAttributes)
      throws InfrastructureException, ValidationException, NotFoundException {

    InternalEnvironment internalEnvironment = super.create(environment, workspaceAttributes);

    InternalEnvironment convertedEnvironment = environmentConverter.convert(internalEnvironment);

    applyChePlugins(
        convertedEnvironment, workspaceAttributes, convertedEnvironment.getRecipe().getType());

    return convertedEnvironment;
  }

  private void applyChePlugins(
      InternalEnvironment internalEnvironment,
      Map<String, String> workspaceAttributes,
      String recipeType)
      throws InfrastructureException {
    Collection<ChePlugin> chePlugins = chePluginsRetriever.get(workspaceAttributes);
    if (chePlugins.isEmpty()) {
      return;
    }
    ChePluginsApplier pluginsApplier = chePluginsAppliers.get(recipeType);
    if (pluginsApplier == null) {
      throw new InfrastructureException(
          "Che plugins flow is not supported for recipe type " + recipeType);
    }
    pluginsApplier.apply(internalEnvironment, chePlugins);
  }
}
