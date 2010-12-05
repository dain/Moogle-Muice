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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import com.google.inject.matcher.Matchers;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.DependencyListener;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import junit.framework.TestCase;

/**
 * @author dain@iq80.com (Dain Sundstrom)
 */
public class DependencyListenerTest extends TestCase {

  private Injector injector;

  @Inject
  DependencyHolder injectMember;

  protected void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bindListener(Matchers.only(new TypeLiteral<DependencyHolder>() {}), new TypeListener() {
          public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            TypeEncounter<DependencyHolder> dependencyHolderEncounter = (TypeEncounter<DependencyHolder>) encounter;
            dependencyHolderEncounter.register(new DependencyListener<DependencyHolder>() {
              public void injectMembers(DependencyHolder instance, Dependency<DependencyHolder> dependency) {
                instance.injectMembersDependency = dependency;
              }

              public void afterInjection(DependencyHolder instance, Dependency<DependencyHolder> dependency) {
                instance.afterInjectionDependency = dependency;
              }
            });
          }
        });
      }
    });
  }

  public void testLoggerWithMember() throws Exception {
    injector.injectMembers(this);
    verifyDependencyHolder("injectMember", injectMember, -1, getClass().getDeclaredField("injectMember"), getClass());
  }

  public void testDependencyListener() throws Exception {
    TargetObject targetObject = injector.getInstance(TargetObject.class);
    verifyTargetObject(targetObject);
  }

  private void verifyTargetObject(TargetObject targetObject) throws Exception {
    assertNotNull(targetObject);

    Constructor<?> constructor = TargetObject.class.getConstructor(DependencyHolder.class, DependencyHolder.class);
    verifyDependencyHolder("consturctorParameter0", targetObject.consturctorParameter0, 0, constructor, TargetObject.class);
    verifyDependencyHolder("consturctorParameter1", targetObject.consturctorParameter1, 1, constructor, TargetObject.class);

    Method method = TargetObject.class.getMethod("method", DependencyHolder.class, DependencyHolder.class);
    verifyDependencyHolder("methodParameter0", targetObject.methodParameter0, 0, method, TargetObject.class);
    verifyDependencyHolder("methodParameter1", targetObject.methodParameter1, 1, method, TargetObject.class);

    Field field = TargetObject.class.getDeclaredField("field");
    verifyDependencyHolder("field", targetObject.field, -1, field, TargetObject.class);
  }

  private void verifyDependencyHolder(String name, DependencyHolder dependencyHolder, int parameterIndex, Member member, Class<?> targetClass) {
    assertNotNull(name, dependencyHolder);

    Dependency<?> dependency = dependencyHolder.afterInjectionDependency;
    assertNotNull(name, dependency);
    assertEquals(name, Key.get(DependencyHolder.class), dependency.getKey());
    assertEquals(name, parameterIndex, dependency.getParameterIndex());
    assertFalse(name, dependency.isNullable());

    InjectionPoint injectionPoint = dependency.getInjectionPoint();
    assertNotNull(name, injectionPoint);
    assertEquals(name, TypeLiteral.get(targetClass), injectionPoint.getDeclaringType());
    assertEquals(name, member, injectionPoint.getMember());
    assertFalse(name, injectionPoint.isOptional());
    assertFalse(name, injectionPoint.isToolable());

    assertSame(name, dependencyHolder.injectMembersDependency, dependencyHolder.afterInjectionDependency);
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
    public Dependency<DependencyHolder> injectMembersDependency;
    public Dependency<DependencyHolder> afterInjectionDependency;
  }
}
