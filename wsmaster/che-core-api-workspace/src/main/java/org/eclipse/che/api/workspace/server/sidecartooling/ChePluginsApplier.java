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
package org.eclipse.che.api.workspace.server.sidecartooling;

import com.google.common.annotations.Beta;
import java.util.Collection;
import org.eclipse.che.api.workspace.server.sidecartooling.model.ChePlugin;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironment;

/**
 * Applies Che plugins tooling configuration to an internal runtime object that represents workspace
 * runtime configuration on an infrastructure level.
 *
 * @author Oleksander Garagatyi
 */
@Beta
public interface ChePluginsApplier {

  /**
   * Applies Che plugins tooling configuration to internal environment.
   *
   * @param internalEnvironment infrastructure specific representation of workspace runtime
   *     environment
   * @param chePlugins Che plugin tooling configuration to apply to {@code internalEnvironment}
   * @throws InfrastructureException when applying Che plugin tooling objects fails
   */
  void apply(InternalEnvironment internalEnvironment, Collection<ChePlugin> chePlugins)
      throws InfrastructureException;
}
