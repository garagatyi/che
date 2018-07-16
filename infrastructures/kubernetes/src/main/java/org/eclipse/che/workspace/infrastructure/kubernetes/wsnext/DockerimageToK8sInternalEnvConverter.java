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
package org.eclipse.che.workspace.infrastructure.kubernetes.wsnext;

import javax.inject.Inject;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironment;
import org.eclipse.che.api.workspace.server.wsnext.InternalEnvironmentConverter;
import org.eclipse.che.workspace.infrastructure.docker.environment.dockerimage.DockerImageEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.convert.DockerImageEnvironmentConverter;

/**
 * Converts Dockerimage workspace environment to kubernetes environment. Other internal environments
 * are returned without changes.
 *
 * <p>Allows support of sidecar tooling flow on Dockerimage environments while it is directly
 * implemented in the kubernetes environment only.
 *
 * <p>This class differs from {@link DockerImageEnvironmentConverter} because it is supposed that
 * this class can perform transformation of environments other than {@link DockerImageEnvironment}
 * to environments other than {@link KubernetesEnvironment} in future. But for now it does
 * transformation of {@link DockerImageEnvironment} to {@link KubernetesEnvironment} only.
 *
 * @author Oleksandr Garagatyi
 */
public class DockerimageToK8sInternalEnvConverter implements InternalEnvironmentConverter {
  private final DockerImageEnvironmentConverter dockerImageEnvConverter;

  @Inject
  public DockerimageToK8sInternalEnvConverter(
      DockerImageEnvironmentConverter dockerImageEnvConverter) {
    this.dockerImageEnvConverter = dockerImageEnvConverter;
  }

  @Override
  public InternalEnvironment convert(InternalEnvironment internalEnvironment)
      throws InfrastructureException {

    if (internalEnvironment instanceof DockerImageEnvironment) {
      return dockerImageEnvConverter.convert((DockerImageEnvironment) internalEnvironment);
    }
    return internalEnvironment;
  }
}
