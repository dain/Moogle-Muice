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

import com.google.inject.internal.Errors;
import com.google.inject.internal.ErrorsException;
import com.google.inject.internal.ToStringBuilder;
import com.google.inject.spi.Dependency;

/**
 * @author crazybob@google.com (Bob Lee)
 */
class ConstantFactory<T> implements InternalFactory<T> {

  private final T value;

  public ConstantFactory(T value) {
    this.value = value;
  }

  public T get(Errors errors, InternalContext context, Dependency dependency)
      throws ErrorsException {
    context.ensureMemberInjected(errors, value);
    return value;
  }

  public String toString() {
    return new ToStringBuilder(ConstantFactory.class)
        .add("value", value)
        .toString();
  }
}
