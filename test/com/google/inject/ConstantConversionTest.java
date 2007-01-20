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

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;

/**
 * @author crazybob@google.com (Bob Lee)
 */
public class ConstantConversionTest extends TestCase {

  public static class Foo {
    @Inject("#") Integer integerField;
    @Inject("#") int primitiveIntField;
    @Inject("#") Long longField;
    @Inject("#") long primitiveLongField;
    @Inject("boolean") Boolean booleanField;
    @Inject("boolean") boolean primitiveBooleanField;
    @Inject("#") Byte byteField;
    @Inject("#") byte primitiveByteField;
    @Inject("#") Short shortField;
    @Inject("#") short primitiveShortField;
    @Inject("#") Float floatField;
    @Inject("#") float primitiveFloatField;
    @Inject("#") Double doubleField;
    @Inject("#") short primitiveDoubleField;
    @Inject("enum") Bar enumField;
    @Inject("class") Class<?> classField;
  }

  public enum Bar {
    TEE, BAZ, BOB;
  }

  public void testConstantInjection() {
    Map<String, String> properties = new HashMap<String, String>() {{
      put("#", "5");
      put("boolean", "true");
      put("enum", "TEE");
      put("class", Foo.class.getName());
    }};

    ContainerBuilder b = new ContainerBuilder();
    b.bindProperties(properties);
    Container c = b.create(false);
    Foo foo = c.inject(Foo.class);

    checkNumbers(
      foo.integerField,
      foo.primitiveIntField,
      foo.longField,
      foo.primitiveLongField,
      foo.byteField,
      foo.primitiveByteField,
      foo.shortField,
      foo.primitiveShortField,
      foo.floatField,
      foo.primitiveFloatField,
      foo.doubleField,
      foo.primitiveDoubleField
    );

    assertEquals(Bar.TEE, foo.enumField);
    assertEquals(Foo.class, foo.classField);
  }

  void checkNumbers(Number... ns) {
    for (Number n : ns) {
      assertEquals(5, n.intValue());
    }
  }

  public void testInvalidInteger() {
    ContainerBuilder b = new ContainerBuilder();
    b.bind("#").to("invalid");
    Container c = b.create(false);
    try {
      c.inject(InvalidInteger.class);
      fail();
    } catch (ConstantConversionException e) {
      assertTrue(e.getMessage().startsWith(
          "Error converting 'invalid' to Integer while injecting integerField "
              + "with dependency named '#' in InvalidInteger. Reason:"));
    }
  }

  public static class InvalidInteger {
    @Inject("#") Integer integerField;
  }

  public void testInvalidCharacter() {
    ContainerBuilder b = new ContainerBuilder();
    b.bind("foo").to("invalid");
    Container c = b.create(false);
    try {
      c.inject(InvalidCharacter.class);
      fail();
    } catch (ConstantConversionException e) {
      assertTrue(e.getMessage().startsWith(
          "Error converting 'invalid' to char while injecting foo "
              + "with dependency named 'foo' in InvalidCharacter. Reason:"));
    }
  }

  public static class InvalidCharacter {
    @Inject("foo") char foo;
  }

  public void testInvalidEnum() {
    ContainerBuilder b = new ContainerBuilder();
    b.bind("foo").to("invalid");
    Container c = b.create(false);
    try {
      c.inject(InvalidEnum.class);
      fail();
    } catch (ConstantConversionException e) {
      assertTrue(e.getMessage().startsWith(
          "Error converting 'invalid' to Bar while injecting foo "
              + "with dependency named 'foo' in InvalidEnum. Reason:"));
    }
  }

  public static class InvalidEnum {
    @Inject("foo") Bar foo;
  }
}
