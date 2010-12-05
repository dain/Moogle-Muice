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

package com.google.inject;

import java.lang.reflect.Member;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;
import junit.framework.TestCase;

public class DependencyInjectionTest extends TestCase {
  private static final Key<DependencyHolder> dependencyHolderKey = Key.get(DependencyHolder.class);


  public void testDirectInjection() throws Exception {
    TargetObject targetObject = Guice.createInjector().getInstance(TargetObject.class);
    verifyTargetObject(targetObject, false);
  }

  public void testProviderInjection() throws Exception {
    TargetObject targetObject = Guice.createInjector(
        new Module() {
          public void configure(Binder binder) {
            binder.bind(DependencyHolder.class).toProvider(DependencyHolderProvider.class);
          }
        }
    ).getInstance(TargetObject.class);
    verifyTargetObject(targetObject, true);
  }

  private void verifyTargetObject(TargetObject targetObject, final boolean createdByProvider) throws NoSuchMethodException, NoSuchFieldException {
    assertNotNull(targetObject);

    Constructor<?> constructor = TargetObject.class.getConstructor(DependencyHolder.class, DependencyHolder.class);
    verifyDependencyHolder("consturctorParameter0", targetObject.consturctorParameter0, 0, constructor, createdByProvider);
    verifyDependencyHolder("consturctorParameter1", targetObject.consturctorParameter1, 1, constructor, createdByProvider);

    Method method = TargetObject.class.getMethod("method", DependencyHolder.class, DependencyHolder.class);
    verifyDependencyHolder("methodParameter0", targetObject.methodParameter0, 0, method, createdByProvider);
    verifyDependencyHolder("methodParameter1", targetObject.methodParameter1, 1, method, createdByProvider);

    Field field = TargetObject.class.getDeclaredField("field");
    verifyDependencyHolder("field", targetObject.field, -1, field, createdByProvider);
  }

  private void verifyDependencyHolder(String name, DependencyHolder dependencyHolder, int parameterIndex, Member member, final boolean createdByProvider) {
    assertNotNull(name, dependencyHolder);
    assertEquals(name, createdByProvider, dependencyHolder.createdByProvider);

    Dependency<?> dependency = dependencyHolder.dependency;
    assertNotNull(name, dependency);
    assertEquals(name, dependencyHolderKey, dependency.getKey());
    assertEquals(name, parameterIndex, dependency.getParameterIndex());
    assertFalse(name, dependency.isNullable());

    InjectionPoint injectionPoint = dependency.getInjectionPoint();
    assertNotNull(name, injectionPoint);
    assertEquals(name, TypeLiteral.get(TargetObject.class), injectionPoint.getDeclaringType());
    assertEquals(name, member, injectionPoint.getMember());
    assertFalse(name, injectionPoint.isOptional());
    assertFalse(name, injectionPoint.isToolable());
  }

  public void testNoInjectionPoint() {
    DependencyHolder dependencyHolder = Guice.createInjector().getInstance(DependencyHolder.class);
    assertNotNull(dependencyHolder);

    Dependency<?> dependency = dependencyHolder.dependency;
    assertEquals(dependencyHolderKey, dependency.getKey());
    assertEquals(-1, dependency.getParameterIndex());
    assertTrue(dependency.isNullable());

    // there should be no injection point since the dependency was fetched
    // directly from the injector
    assertNull(dependency.getInjectionPoint());
  }


  @Inject Dependency<?> dependency;

  public void testGetDependencyDirect() {
    // With no dependencies on the stack the Dependency provider will return
    // null as expected
    assertNull(Guice.createInjector().getInstance(Dependency.class));
    assertNull(Guice.createInjector().getInstance(Key.get(new TypeLiteral<Dependency<?>>(){})));

    Guice.createInjector().injectMembers(this);
    assertNull(dependency);
  }

  public static class TargetObject {
    private final DependencyHolder consturctorParameter0;
    private final DependencyHolder consturctorParameter1;
    private DependencyHolder methodParameter0;
    private DependencyHolder methodParameter1;

    @Inject
    protected DependencyHolder field;

    @Inject
    public TargetObject(DependencyHolder consturctorParameter0, DependencyHolder consturctorParameter1) {
      this.consturctorParameter0 = consturctorParameter0;
      this.consturctorParameter1 = consturctorParameter1;
    }

    @Inject
    public void method(DependencyHolder methodParameter0, DependencyHolder methodParameter1) {
      this.methodParameter0 = methodParameter0;
      this.methodParameter1 = methodParameter1;
    }
  }

  public static class DependencyHolder {
    private final Dependency<?> dependency;
    private final boolean createdByProvider;

    @Inject
    public DependencyHolder(Dependency<?> dependency) {
      this.dependency = dependency;
      createdByProvider = false;
    }

    public DependencyHolder(Dependency<?> dependency, boolean createdByProvider) {
      this.dependency = dependency;
      this.createdByProvider = createdByProvider;
    }

    public Dependency<?> getDependency() {
      return dependency;
    }
  }

  public static class DependencyHolderProvider implements Provider<DependencyHolder> {
    private final Dependency<?> dependency;

    @Inject
    public DependencyHolderProvider(Dependency<?> dependency) {
      this.dependency = dependency;
    }

    @Override
    public DependencyHolder get() {
      return new DependencyHolder(dependency, true);
    }
  }
}
