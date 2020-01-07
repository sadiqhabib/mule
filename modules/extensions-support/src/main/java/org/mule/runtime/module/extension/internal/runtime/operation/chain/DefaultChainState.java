/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.chain;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.collection.SmallMap.unmodifiable;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.extension.api.runtime.route.ChainState;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;

public class DefaultChainState implements ChainState {

  private final Map<String, Object> variables = new SmallMap<>();
  private final ComponentLocation componentLocation;
  private ChainState root;
  private Optional<ChainState> parent;

  private DefaultChainState(ComponentLocation componentLocation) {
    this.componentLocation = componentLocation;
    this.root = this;
    this.parent = empty();
  }

  private void setRoot(ChainState root) {
    this.root = root;
  }

  private void setParent(ChainState parent) {
    this.parent = of(parent);
  }

  @Override
  public ComponentLocation getComponentLocation() {
    return componentLocation;
  }

  @Override
  public Map<String, Object> getVariables() {
    return unmodifiable(variables);
  }

  @Override
  public <T> Optional<T> getVariable(String key) {
    if (!hasVariable(key)) {
      return empty();
    }
    //TODO ver forma mas clara de resolverlo
    return ofNullable((T) variables.get(key));
  }

  @Override
  public boolean hasVariable(String key) {
    return variables.containsKey(key);
  }

  @Override
  public void addVariable(String key, Object value) {
    variables.put(key, value);
  }

  @Override
  public ChainState getRoot() {
    return root;
  }

  @Override
  public Optional<ChainState> getParent() {
    return parent;
  }

  public static DefaultChainStateBuilder builder() {
    return new DefaultChainStateBuilder();
  }

  public static class DefaultChainStateBuilder {

    private ComponentLocation compLocation;
    private ArrayDeque<ChainState> ancestors;

    public DefaultChainStateBuilder setComponentLocation(ComponentLocation componentLocation) {
      compLocation = componentLocation;
      return this;
    }

    public DefaultChainStateBuilder setAncestors(ArrayDeque<ChainState> ancestors) {
      this.ancestors = ancestors;
      return this;
    }

    public DefaultChainState build() {
      checkArgument(compLocation != null, "Component location cannot be null");
      DefaultChainState chainState = new DefaultChainState(compLocation);
      if (ancestors != null && !ancestors.isEmpty()) {
        chainState.setParent(ancestors.getFirst());
        chainState.setRoot(ancestors.getLast());
      }
      return chainState;
    }
  }
}
