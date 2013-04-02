package com.codahale.og.tests;

import com.codahale.og.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

public class ObjectGraphTest {
    private static class ListModule {
        int called = 0;

        @Provides
        List<String> getList(String s) {
            this.called++;
            return ImmutableList.of(s);
        }

        @Provides
        @Singleton
        List<Long> getList() {
            this.called++;
            return ImmutableList.of();
        }
    }

    private static class MapModule {
        @Provides
        Map<String, Integer> getMap(List<String> strings) {
            final ImmutableMap.Builder<String, Integer> map = ImmutableMap.builder();
            for (String string : strings) {
                map.put(string, string.length());
            }
            return map.build();
        }
    }

    private static class NamedStringModule {
        @Provides
        @Named("thingy")
        public String getString() {
            return "yay";
        }

        @Provides
        @Named("other")
        public String getString(@Named("thingy") String s) {
            return "oh " + s;
        }
    }

    private static class ImmutableListModule {
        @Provides
        public ImmutableList<Integer> getNumbers() {
            return ImmutableList.of(1, 2, 3);
        }
    }

    private static class UnprovidableModule {
        @Provides
        public Long getLong(Integer i) {
            return i.longValue();
        }
    }

    private static class FirstLongModule {
        @Provides
        public Long getString() {
            return 1L;
        }
    }

    private static class SecondLongModule {
        @Provides
        public Long getString() {
            return 2L;
        }
    }

    private static class PrimitiveModule {
        @Provides
        public byte getByte() {
            return 10;
        }

        @Provides
        public short getShort() {
            return 20;
        }

        @Provides
        public int getInt() {
            return 30;
        }

        @Provides
        public long getLong() {
            return 40;
        }

        @Provides
        public float getFloat() {
            return 50;
        }

        @Provides
        public double getDouble() {
            return 60;
        }

        @Provides
        public boolean getBoolean() {
            return true;
        }

        @Provides
        public char getChar() {
            return 'A';
        }
    }

    private final ObjectGraph graph = new ObjectGraph();

    private final ListModule listModule = new ListModule();
    private final MapModule mapModule = new MapModule();
    private final NamedStringModule namedStringModule = new NamedStringModule();
    private final ImmutableListModule immutableListModule = new ImmutableListModule();
    private final UnprovidableModule unprovidableModule = new UnprovidableModule();

    @Before
    public void setUp() throws Exception {
        graph.addSingleton("woo");
        graph.addModule(listModule);
        graph.addModule(mapModule);
        graph.addModule(namedStringModule);
        graph.addModule(immutableListModule);
    }

    @Test
    public void providesSingletons() throws Exception {
        assertThat(graph.get(String.class))
                .isEqualTo("woo");
    }

    @Test
    public void providesGenericTypesDependentOnSingletons() throws Exception {
        assertThat(graph.get(new TypeToken<List<String>>() {}))
                .containsOnly("woo");
    }

    @Test
    public void memoizesOnlySingletonAnnotatedObjects() throws Exception {
        graph.get(new TypeToken<List<String>>() {});
        graph.get(new TypeToken<List<String>>() {});

        assertThat(listModule.called)
                .isEqualTo(2);

        graph.get(new TypeToken<List<Long>>() {});
        graph.get(new TypeToken<List<Long>>() {});

        assertThat(listModule.called)
                .isEqualTo(3);
    }

    @Test
    public void providesGenericTypesDependentOnProvidedTypes() throws Exception {
        assertThat(graph.get(new TypeToken<Map<String, Integer>>() {}))
                .isEqualTo(ImmutableMap.of("woo", 3));
    }

    @Test
    public void providesNamedTypes() throws Exception {
        assertThat(graph.get(String.class, "thingy"))
                .isEqualTo("yay");
    }

    @Test
    public void throwsADependencyExceptionForUnprovidableTypes() throws Exception {
        try {
            graph.get(Integer.class);
            failBecauseExceptionWasNotThrown(DependencyException.class);
        } catch (DependencyException e) {
            assertThat(e.getMessage())
                    .isEqualTo("Unable to provide a java.lang.Integer");
        }
    }

    @Test
    public void throwsADependencyExceptionForUnprovidableNamedTypes() throws Exception {
        try {
            graph.get(Integer.class, "woo");
            failBecauseExceptionWasNotThrown(DependencyException.class);
        } catch (DependencyException e) {
            assertThat(e.getMessage())
                    .isEqualTo("Unable to provide a java.lang.Integer named 'woo'");
        }
    }

    @Test
    public void throwsANestedDependencyExceptionForTransitivelyUnprovidableTypes() throws Exception {
        graph.addModule(unprovidableModule);
        try {
            graph.get(Long.class);
            failBecauseExceptionWasNotThrown(DependencyException.class);
        } catch (DependencyException e) {
            assertThat(e.getMessage())
                    .isEqualTo("Unable to provide a java.lang.Long");

            assertThat(e.getCause().getMessage())
                    .isEqualTo("Unable to provide a java.lang.Integer");
        }
    }

    @Test
    public void providesSuperclassTypes() throws Exception {
        assertThat(graph.get(new TypeToken<List<Integer>>() {}))
                .containsOnly(1, 2, 3);
    }

    @Test
    public void checksTheNameOfSuperclassTypes() throws Exception {
        try {
            graph.get(new TypeToken<List<Integer>>() {}, "woo");
            failBecauseExceptionWasNotThrown(DependencyException.class);
        } catch (DependencyException e) {
            assertThat(e.getMessage())
                    .isEqualTo("Unable to provide a java.util.List<java.lang.Integer> named 'woo'");
        }
    }

    @Test
    public void injectsNamedParameters() throws Exception {
        assertThat(graph.get(String.class, "other"))
                .isEqualTo("oh yay");
    }

    @Test
    public void preloadsSingletons() throws Exception {
        graph.preload();

        assertThat(listModule.called)
                .isEqualTo(1);
    }

    @Test
    public void overridesExistingModules() throws Exception {
        graph.addModule(new FirstLongModule());

        assertThat(graph.get(Long.class))
                .isEqualTo(1);

        graph.addModule(new SecondLongModule());

        assertThat(graph.get(Long.class))
                .isEqualTo(2);
    }

    @Test
    public void mapsPrimitiveTypes() throws Exception {
        graph.addModule(new PrimitiveModule());

        assertThat(graph.get(Byte.class))
                .isEqualTo((byte) 10);

        assertThat(graph.get(byte.class))
                .isEqualTo((byte) 10);

        assertThat(graph.get(Short.class))
                .isEqualTo((short) 20);

        assertThat(graph.get(short.class))
                .isEqualTo((short) 20);

        assertThat(graph.get(Integer.class))
                .isEqualTo(30);

        assertThat(graph.get(int.class))
                .isEqualTo(30);

        assertThat(graph.get(Long.class))
                .isEqualTo(40);

        assertThat(graph.get(long.class))
                .isEqualTo(40);

        assertThat(graph.get(Float.class))
                .isEqualTo(50);

        assertThat(graph.get(float.class))
                .isEqualTo(50);

        assertThat(graph.get(Double.class))
                .isEqualTo(60);

        assertThat(graph.get(double.class))
                .isEqualTo(60);

        assertThat(graph.get(Boolean.class))
                .isTrue();

        assertThat(graph.get(boolean.class))
                .isTrue();

        assertThat(graph.get(Character.class))
                .isEqualTo('A');

        assertThat(graph.get(char.class))
                .isEqualTo('A');
    }
}
