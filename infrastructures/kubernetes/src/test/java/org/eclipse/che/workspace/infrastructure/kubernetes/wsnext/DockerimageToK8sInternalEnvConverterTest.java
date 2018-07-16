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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertSame;

import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironment;
import org.eclipse.che.workspace.infrastructure.docker.environment.compose.ComposeEnvironment;
import org.eclipse.che.workspace.infrastructure.docker.environment.dockerfile.DockerfileEnvironment;
import org.eclipse.che.workspace.infrastructure.docker.environment.dockerimage.DockerImageEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.convert.DockerImageEnvironmentConverter;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** @author Oleksandr Garagatyi */
@Listeners(MockitoTestNGListener.class)
public class DockerimageToK8sInternalEnvConverterTest {
  @Mock KubernetesEnvironment kubernetesEnvironment;

  @Mock DockerImageEnvironment environment;

  @Mock DockerImageEnvironmentConverter k8sSpecificConverter;

  @InjectMocks DockerimageToK8sInternalEnvConverter sidecarConverter;

  @Test
  public void shouldConvertDockerimageEnvToK8sEnv() throws Exception {
    when(k8sSpecificConverter.convert(any(DockerImageEnvironment.class)))
        .thenReturn(kubernetesEnvironment);

    InternalEnvironment convertedEnvironment = sidecarConverter.convert(environment);

    assertSame(convertedEnvironment, kubernetesEnvironment);
  }

  @Test(dataProvider = "nonDockerimageInternalEnvironmentProvider")
  public void shouldNotChangeOtherThanDockerimageEnvironment(InternalEnvironment environment)
      throws Exception {
    InternalEnvironment convertedEnvironment = sidecarConverter.convert(environment);

    assertSame(convertedEnvironment, environment);
  }

  @DataProvider(name = "nonDockerimageInternalEnvironmentProvider")
  public static Object[][] nonDockerimageInternalEnvironmentProvider() {
    return new Object[][] {
      {mock(ComposeEnvironment.class)},
      {mock(KubernetesEnvironment.class)},
      {mock(DockerfileEnvironment.class)},
      {mock(InternalEnvironment.class)},
    };
  }
}
