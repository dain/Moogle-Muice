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

package com.google.inject;

/**
 * @author crazybob@google.com (Bob Lee)
*/
class InternalFactoryToFactoryAdapter<T> implements InternalFactory<T> {

  private final Factory<? extends T> factory;

  public InternalFactoryToFactoryAdapter(
      Factory<? extends T> factory) {
    this.factory = factory;
  }

  public T get(InternalContext context) {
    return factory.get(context.getExternalContext());
  }

  public String toString() {
    return factory.toString();
  }
}
