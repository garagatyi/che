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

/**
 * Checks whether a toggle with a specific name is activated.
 *
 * @author Oleksandr Garagatyi
 */
public interface Toggles {

  /**
   * Checks whether a toggle is activated or not.
   *
   * <p>calling this method is equal to calling method {@linkplain #isEnabled(String, boolean)} with
   * value "false" as second parameter
   *
   * @param toggleName name of the toggle to check
   * @return true if toggle is activated, false if not activated or not found
   */
  boolean isEnabled(String toggleName);

  /**
   * Checks whether a toggle is activated or not.
   *
   * @param toggleName name of the toggle to check
   * @param toggleNotFoundValue value that should be returned if toggle with specified name is not
   *     found
   * @return true if toggle is activated, false if not activated and parameter toggleNotFoundValue
   *     if toggle with the specified name is not found
   */
  boolean isEnabled(String toggleName, boolean toggleNotFoundValue);
}
