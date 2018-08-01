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

import java.util.Map;
import javax.inject.Inject;
import org.eclipse.che.inject.ConfigurationProperties;

/**
 * Implementation of {@link Toggles} that gets information about toggles activation from DI
 * container.
 *
 * <p>To leverage Eclipse Che injection features toggle has to be set with a prefix che. For
 * example, toggle with a name "testToggle" can be set by property "che.testToggle". It can also be
 * injected using environment variables and other ways that described in Che documentation.
 *
 * <p>Injected toggle value is interpreted in a way that case insensitive value "true" is mapped to
 * activated toggle and any other value is mapped to deactivated toggle.
 *
 * <p>Note that this implementation doesn't work with toggles names that contain dash and some other
 * symbols because of how {@link System#getenv()} works.
 *
 * @author Oleksandr Garagatyi
 */
public class PropertyBasedToggles implements Toggles {

  private final ConfigurationProperties configurationProperties;

  @Inject
  public PropertyBasedToggles(ConfigurationProperties configurationProperties) {
    this.configurationProperties = configurationProperties;
  }

  @Override
  public boolean isEnabled(String toggleName) {
    return isEnabled(toggleName, false);
  }

  @Override
  public boolean isEnabled(String toggleName, boolean toggleNotFoundValue) {
    String prefixedToggle = "che." + toggleName;
    Map<String, String> matchingProperties = configurationProperties.getProperties(prefixedToggle);
    String toggleValue = matchingProperties.get(prefixedToggle);
    return toggleValue != null ? Boolean.parseBoolean(toggleValue) : toggleNotFoundValue;
  }
}
