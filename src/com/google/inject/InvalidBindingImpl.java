/*
 * Copyright (C) 2007 Google Inc.
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
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.InjectionPoint;

class InvalidBindingImpl<T> extends BindingImpl<T> {

  InvalidBindingImpl(InjectorImpl injector, Key<T> key, Object source) {
    super(injector, key, source, new InternalFactory<T>() {
      public T get(Errors errors, InternalContext context, InjectionPoint<?> injectionPoint) {
        throw new AssertionError();
      }
    }, Scopes.NO_SCOPE, LoadStrategy.LAZY);
  }

  public <V> V acceptTargetVisitor(BindingTargetVisitor<? super T, V> vVisitor) {
    throw new UnsupportedOperationException();
  }

  @Override public String toString() {
    return "InvalidBinding";
  }
}
