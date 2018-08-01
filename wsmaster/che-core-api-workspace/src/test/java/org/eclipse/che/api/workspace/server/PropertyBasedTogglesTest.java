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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.eclipse.che.inject.ConfigurationProperties;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** @author Oleksandr Garagatyi */
@Listeners(MockitoTestNGListener.class)
public class PropertyBasedTogglesTest {
  private static String TOGGLE_NAME = "test-toggle";
  private static String TOGGLE_PREFIX = "che.";
  private static String PREFIXED_TOGGLE_NAME = TOGGLE_PREFIX + TOGGLE_NAME;
  private static String ACTIVATED_PROPERTY_VALUE = Boolean.TRUE.toString();
  private static String DEACTIVATED_PROPERTY_VALUE = Boolean.FALSE.toString();

  @Mock ConfigurationProperties configurationProperties;

  @InjectMocks PropertyBasedToggles toggles;

  @Test(dataProvider = "activatingTogglePropertiesSets")
  public void shouldReturnTrueIfToggleIsActivated(Map<String, String> properties) {
    when(configurationProperties.getProperties(TOGGLE_PREFIX + TOGGLE_NAME)).thenReturn(properties);

    assertTrue(toggles.isEnabled(TOGGLE_NAME));
  }

  @DataProvider(name = "activatingTogglePropertiesSets")
  public static Object[][] activatingTogglePropertiesSets() {
    return new Object[][] {
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "True")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "tRuE")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "true", PREFIXED_TOGGLE_NAME + ".", "false")}
    };
  }

  @Test(dataProvider = "deactivatingTogglePropertiesSets")
  public void shouldReturnFalseIfToggleIsDeactivated(Map<String, String> properties) {
    when(configurationProperties.getProperties(TOGGLE_NAME)).thenReturn(properties);

    assertFalse(toggles.isEnabled(TOGGLE_NAME));
  }

  @DataProvider(name = "deactivatingTogglePropertiesSets")
  public static Object[][] deactivatingTogglePropertiesSets() {
    return new Object[][] {
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "null")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "Null")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "Nil")},
      {singletonMap(PREFIXED_TOGGLE_NAME, null)},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "true1")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "1true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "false")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "False")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "yes")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "so true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME, "true so")}
    };
  }

  @Test(dataProvider = "missingTogglePropertiesSets")
  public void shouldReturnFalseIfToggleIsMissing(Map<String, String> properties) {
    when(configurationProperties.getProperties(TOGGLE_NAME)).thenReturn(properties);

    assertFalse(toggles.isEnabled(TOGGLE_NAME));
  }

  @DataProvider(name = "missingTogglePropertiesSets")
  public static Object[][] missingTogglePropertiesSets() {
    return new Object[][] {
      {emptyMap()},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME + "1", "true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME + ".", "true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME + ".value", "true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME + ".true", "true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME + ":true", "true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME + "=true", "true")},
      {ImmutableMap.of(PREFIXED_TOGGLE_NAME + "*", "true")},
      {ImmutableMap.of("." + PREFIXED_TOGGLE_NAME, "true")},
      {ImmutableMap.of("1" + PREFIXED_TOGGLE_NAME, "true")},
      {ImmutableMap.of("*" + PREFIXED_TOGGLE_NAME, "true")},
      {ImmutableMap.of("che" + PREFIXED_TOGGLE_NAME, "true")}
    };
  }

  @Test
  public void shouldReturnTrueIfToggleIsActivatedAndFallbackValueIsTrue() {
    activateToggle();

    assertTrue(toggles.isEnabled(TOGGLE_NAME, true));
  }

  @Test
  public void shouldReturnTrueIfToggleIsActivatedAndFallbackValueIsFalse() {
    activateToggle();

    assertTrue(toggles.isEnabled(TOGGLE_NAME, false));
  }

  @Test
  public void shouldReturnFalseIfToggleIsDeactivatedAndFallbackIsFalse() {
    deactivateToggle();

    assertFalse(toggles.isEnabled(TOGGLE_NAME, false));
  }

  @Test
  public void shouldReturnTrueIfToggleIsMissingAndFallbackIsTrue() {
    assertTrue(toggles.isEnabled(TOGGLE_NAME, true));
  }

  @Test
  public void shouldReturnFalseIfToggleIsMissingAndFallbackIsFalse() {
    assertFalse(toggles.isEnabled(TOGGLE_NAME, false));
  }

  private void activateToggle() {
    when(configurationProperties.getProperties(PREFIXED_TOGGLE_NAME))
        .thenReturn(singletonMap(PREFIXED_TOGGLE_NAME, ACTIVATED_PROPERTY_VALUE));
  }

  private void deactivateToggle() {
    when(configurationProperties.getProperties(PREFIXED_TOGGLE_NAME))
        .thenReturn(singletonMap(PREFIXED_TOGGLE_NAME, DEACTIVATED_PROPERTY_VALUE));
  }
}
