/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.chain;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.extension.api.runtime.route.ChainContext;
import org.mule.runtime.extension.api.runtime.route.ChainState;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

public class DefaultChainContext implements ChainContext, EventInternalContext {

  private final Map<String, ArrayDeque<ChainState>> stackByComponent = new SmallMap<>();
  private final ArrayDeque<ChainState> stack = new ArrayDeque<>();

  @Override
  public Optional<ChainState> getState(String extensionNamespace, String componentName) {
    checkArgument(!isEmpty(extensionNamespace), "Extension namespace cannot be empty");
    checkArgument(!isEmpty(extensionNamespace), "Component name cannot be empty");

    String key = generateKey(extensionNamespace, componentName);
    if (!stackByComponent.containsKey(key))
      return Optional.empty();
    return Optional.of(stackByComponent.get(key).peek());
  }

  public ChainState addState(ComponentLocation componentLocation) {
    checkArgument(componentLocation != null, "Component location cannot be null");

    String key = getKeyOf(componentLocation);
    if (!stackByComponent.containsKey(key)) {
      stackByComponent.put(key, new ArrayDeque<>());
    }
    ChainState chainState = DefaultChainState.builder()
        .setComponentLocation(componentLocation)
        .setAncestors(stackByComponent.get(key).clone())
        .build();
    stackByComponent.get(key).push(chainState);
    stack.push(chainState);
    return chainState;
  }

  public ChainState removeState() {
    ChainState chainState = stack.pop();
    String key = getKeyOf(chainState);
    stackByComponent.get(key).remove(chainState);
    return chainState;
  }

  private String getKeyOf(ChainState chainState) {
    return getKeyOf(chainState.getComponentLocation());
  }

  private String getKeyOf(ComponentLocation componentLocation) {
    ComponentIdentifier componentIdentifier = componentLocation.getComponentIdentifier().getIdentifier();
    return generateKey(componentIdentifier.getNamespace(), componentIdentifier.getName());
  }

  private String generateKey(String extensionNamespace, String componentName) {
    return String.format("{}.{}", extensionNamespace.toUpperCase(), componentName.toUpperCase());
  }

  @Override
  public EventInternalContext copy() {
    DefaultChainContext chainContext = new DefaultChainContext();
    for (ChainState chainState : chainContext.stack) {
      chainContext.addState(chainState.getComponentLocation());
    }
    return chainContext;
  }
}
