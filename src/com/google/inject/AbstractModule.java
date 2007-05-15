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

import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.AnnotatedConstantBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.internal.Objects;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.SourceProviders;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;

/**
 * A support class for {@link Module}s which reduces repetition and results in
 * a more readable configuration. Simply extend this class, implement {@link
 * #configure()}, and call the inherited methods which mirror those found in
 * {@link Binder}. For example:
 *
 * <pre>
 * import static com.google.inject.Names.named;
 *
 * public class MyModule extends AbstractModule {
 *   protected void configure() {
 *     bind(Foo.class).to(FooImpl.class).in(Scopes.SINGLETON);
 *     bind(BarImpl.class);
 *     link(Bar.class).to(BarImpl.class);
 *     bindConstant(named("port")).to(8080);
 *   }
 * }
 * </pre>
 *
 * @author crazybob@google.com (Bob Lee)
 */
public abstract class AbstractModule implements Module {

  static {
    SourceProviders.skip(AbstractModule.class);
  }

  Binder binder;

  public final synchronized void configure(Binder builder) {
    try {
      if (this.binder != null) {
        throw new IllegalStateException("Re-entry is not allowed.");
      }
      this.binder = Objects.nonNull(builder, "builder");

      configure();

    }
    finally {
      this.binder = null;
    }
  }

  /**
   * Configures a {@link Binder} via the exposed methods.
   */
  protected abstract void configure();

  /**
   * Gets direct access to the underlying {@code Binder}.
   */
  protected Binder binder() {
    return binder;
  }

  /**
   * @see Binder#bindScope(Class, Scope)
   */
  protected void bindScope(Class<? extends Annotation> scopeAnnotation,
      Scope scope) {
    binder.bindScope(scopeAnnotation, scope);
  }

  /**
   * @see Binder#bind(Key)
   */
  protected <T> LinkedBindingBuilder<T> bind(Key<T> key) {
    return binder.bind(key);
  }

  /**
   * @see Binder#bind(TypeLiteral)
   */
  protected <T> AnnotatedBindingBuilder<T> bind(TypeLiteral<T> typeLiteral) {
    return binder.bind(typeLiteral);
  }

  /**
   * @see Binder#bind(Class)
   */
  protected <T> AnnotatedBindingBuilder<T> bind(Class<T> clazz) {
    return binder.bind(clazz);
  }

  /**
   * @see Binder#bindConstant()
   */
  protected AnnotatedConstantBindingBuilder bindConstant() {
    return binder.bindConstant();
  }

  /**
   * @see Binder#install(Module)
   */
  protected void install(Module module) {
    binder.install(module);
  }

  /**
   * @see Binder#addError(String, Object[])
   */
  protected void addError(String message, Object... arguments) {
    binder.addError(message, arguments);
  }

  /**
   * @see Binder#addError(Throwable) 
   */
  protected void addError(Throwable t) {
    binder.addError(t);
  }

  /**
   * @see Binder#requestStaticInjection(Class[])
   */
  protected void requestStaticInjection(Class<?>... types) {
    binder.requestStaticInjection(types);
  }

  /**
   * @see Binder#bindInterceptor(com.google.inject.matcher.Matcher,
   *  com.google.inject.matcher.Matcher,
   *  org.aopalliance.intercept.MethodInterceptor[])
   */
  protected void bindInterceptor(Matcher<? super Class<?>> classMatcher,
      Matcher<? super Method> methodMatcher,
      MethodInterceptor... interceptors) {
    binder.bindInterceptor(classMatcher, methodMatcher, interceptors);
  }

  /**
   * @see Binder#getProvider(Key)
   */
  public <T> Provider<T> getProvider(Key<T> key) {
    return binder.getProvider(key);
  }

  /**
   * @see Binder#getProvider(Class)
   */
  public <T> Provider<T> getProvider(Class<T> type) {
    return binder.getProvider(type);
  }
}
