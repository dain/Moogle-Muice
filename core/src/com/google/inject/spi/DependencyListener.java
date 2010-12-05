/**
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

package com.google.inject.spi;

/**
 * Listens for dependency instance creation of type {@code I}.  This listener is
 * provided with the instance and Dependency metadata which is useful for
 * performing actions based on the binding key or injection point.
 *
 * @param <T> type of the instance
 *
 * @author dain@iq80.com (Dain Sundstrom)
 * @since 3.0
 */
public interface DependencyListener<T> {

  /**
   * Invoked by Guice during the inject members phase which is after
   * normal Guice constructor and member injection.
   *
   * @param instance the instance that Guice created to satisfy the dependency
   * @param dependency dependency metadata for the injection
   */
  void injectMembers(T instance, Dependency<T> dependency);

  /**
   * Invoked by Guice during the after injection phase which is the injection
   * phase.
   *
   * @param instance the instance that Guice created to satisfy the dependency
   * @param dependency dependency metadata for the injection
   */
  void afterInjection(T instance, Dependency<T> dependency);
}
