/**
 * Copyright (C) 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.inject.internal.util.ImmutableList;
import com.google.inject.internal.util.Maps;
import com.google.inject.spi.Dependency;

/**
 * Internal context. Used to coordinate injections and support circular
 * dependencies.
 *
 * @author crazybob@google.com (Bob Lee)
 */
final class InternalContext {

  private Map<Object, ConstructionContext<?>> constructionContexts = Maps.newHashMap();
  private LinkedList<Dependency<?>> dependencyStack = new LinkedList<Dependency<?>>();

  @SuppressWarnings("unchecked")
  public <T> ConstructionContext<T> getConstructionContext(Object key) {
    ConstructionContext<T> constructionContext
        = (ConstructionContext<T>) constructionContexts.get(key);
    if (constructionContext == null) {
      constructionContext = new ConstructionContext<T>();
      constructionContexts.put(key, constructionContext);
    }
    return constructionContext;
  }

  public void pushDependency(Dependency<?> dependency)
  {
    dependencyStack.addFirst(dependency);
  }

  public Dependency<?> popDependency()
  {
    return dependencyStack.removeFirst();
  }

  public Dependency<?> getDependency()
  {
    return dependencyStack.getFirst();
  }

  public List<Dependency<?>> getDependencyStack() {
    return ImmutableList.copyOf(dependencyStack);
  }
}
