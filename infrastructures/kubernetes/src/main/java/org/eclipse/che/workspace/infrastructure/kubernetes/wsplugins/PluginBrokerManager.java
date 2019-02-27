package org.eclipse.che.workspace.infrastructure.kubernetes.wsplugins;

import com.google.common.annotations.Beta;
import java.util.Collection;
import java.util.List;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.wsplugins.model.ChePlugin;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginMeta;
import org.eclipse.che.workspace.infrastructure.kubernetes.StartSynchronizer;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;

/**
 * @author Alexander Garagatyi
 */
public interface PluginBrokerManager<E extends KubernetesEnvironment> {

  @Beta
  List<ChePlugin> getTooling(
      RuntimeIdentity runtimeID,
      StartSynchronizer startSynchronizer,
      Collection<PluginMeta> pluginsMeta,
      boolean isEphemeral) throws InfrastructureException;
}
