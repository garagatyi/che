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
package org.eclipse.che.api.workspace.server.wsplugins.model;

import java.net.URI;

/**
 * Represents full information about plugin, including registry address, publisher, id and version.
 *
 * @author Max Shaposhnyk
 */
public class PluginFQN {

  private URI registry;
  private String id;
  private String version;
  private String publisher;

  public PluginFQN(URI registry, String id, String version, String publisher) {
    this.registry = registry;
    this.id = id;
    this.version = version;
    this.publisher = publisher;
  }

  public URI getRegistry() {
    return registry;
  }

  public void setRegistry(URI registry) {
    this.registry = registry;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }
}
