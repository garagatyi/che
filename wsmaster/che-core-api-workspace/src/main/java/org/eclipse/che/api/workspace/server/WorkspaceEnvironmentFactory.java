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
package org.eclipse.che.api.workspace.server;

import static java.lang.String.format;

import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.config.Environment;
import org.eclipse.che.api.workspace.server.sidecartooling.ChePluginsBasedWorkspaceEnvironmentFactory;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.RuntimeInfrastructure;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironmentFactory;

/**
 * Creates {@link InternalEnvironment} from provided environment configuration and workspace
 * attributes.
 *
 * <p>Workspace attributes are not part of the {@link Environment}, but current sidecar flow stores
 * information about sidecars configuration in workspace attributes. This class, comparing to {@link
 * InternalEnvironmentFactory}, allows to use workspace attributes to generate an internal
 * representation of workspace environment.
 *
 * <p>Apart from that, this class may be an extension point when additional operation with generated
 * {@link InternalEnvironment} are needed. For example, it might be interesting to convert some
 * implementations of {@link InternalEnvironment} to another implementation which is directly
 * supported in bound {@link RuntimeInfrastructure}. This would allow to avoid conversion of
 * environments inside of that infrastructure.
 *
 * @see ChePluginsBasedWorkspaceEnvironmentFactory
 * @author Oleksandr Garagatyi
 */
public class WorkspaceEnvironmentFactory {

  private final Map<String, InternalEnvironmentFactory> environmentFactories;

  @Inject
  public WorkspaceEnvironmentFactory(Map<String, InternalEnvironmentFactory> environmentFactories) {
    this.environmentFactories = environmentFactories;
  }

  public Set<String> supportedRecipes() {
    return environmentFactories.keySet();
  }

  public InternalEnvironment create(
      Environment environment, Map<String, String> workspaceAttributes)
      throws InfrastructureException, ValidationException, NotFoundException {

    String recipeType = environment.getRecipe().getType();
    InternalEnvironmentFactory factory = environmentFactories.get(recipeType);
    if (factory == null) {
      throw new NotFoundException(
          format("InternalEnvironmentFactory is not configured for recipe type: '%s'", recipeType));
    }
    return factory.create(environment);
  }
}
