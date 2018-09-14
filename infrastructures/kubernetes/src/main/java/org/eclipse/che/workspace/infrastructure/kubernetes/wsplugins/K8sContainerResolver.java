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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.che.api.workspace.server.wsplugins.model.CheContainer;
import org.eclipse.che.api.workspace.server.wsplugins.model.ChePluginEndpoint;
import org.eclipse.che.api.workspace.server.wsplugins.model.EnvVar;
import org.eclipse.che.workspace.infrastructure.kubernetes.Names;

/** @author Alexander Garagatyi */
public class K8sContainerResolver {

  private final CheContainer cheContainer;
  private final List<ChePluginEndpoint> containerEndpoints;

  public K8sContainerResolver(CheContainer container, List<ChePluginEndpoint> containerEndpoints) {
    this.cheContainer = container;
    this.containerEndpoints = containerEndpoints;
  }

  public List<ChePluginEndpoint> getEndpoints() {
    return containerEndpoints;
  }

  public Container create() {
    List<ContainerPort> containerPorts = getContainerPorts();

    return new ContainerBuilder()
        .withImage(cheContainer.getImage())
        .withName(Names.generateName("tooling"))
        .withEnv(toK8sEnv(cheContainer.getEnv()))
        .withPorts(containerPorts)
        .build();
  }

  private List<ContainerPort> getContainerPorts() {
    return containerEndpoints
        .stream()
        .map(
            endpoint ->
                new ContainerPortBuilder()
                    .withContainerPort(endpoint.getTargetPort())
                    .withProtocol("TCP")
                    .build())
        .collect(Collectors.toList());
  }

  private List<io.fabric8.kubernetes.api.model.EnvVar> toK8sEnv(List<EnvVar> env) {
    List<io.fabric8.kubernetes.api.model.EnvVar> result = new ArrayList<>();

    if (env != null) {
      for (EnvVar envVar : env) {
        result.add(
            new io.fabric8.kubernetes.api.model.EnvVar(envVar.getName(), envVar.getValue(), null));
      }
    }

    return result;
  }
}