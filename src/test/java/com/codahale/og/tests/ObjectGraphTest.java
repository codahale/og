package com.codahale.og.tests;

import com.codahale.og.DependencyException;
import com.codahale.og.Named;
import com.codahale.og.ObjectGraph;
import com.codahale.og.Provides;
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
        assertThat(graph.get(new TypeToken<List<String>>() {
        }))
                .containsOnly("woo");
    }

    @Test
    public void memoizesProvidedObjects() throws Exception {
        graph.get(new TypeToken<List<String>>() {
        });
        graph.get(new TypeToken<List<String>>() {
        });

        assertThat(listModule.called)
                .isEqualTo(1);
    }

    @Test
    public void providesGenericTypesDependentOnProvidedTypes() throws Exception {
        assertThat(graph.get(new TypeToken<Map<String, Integer>>() {
        }))
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
        assertThat(graph.get(new TypeToken<List<Integer>>() {
        }))
                .containsOnly(1, 2, 3);
    }

    @Test
    public void checksTheNameOfSuperclassTypes() throws Exception {
        try {
            graph.get(new TypeToken<List<Integer>>() {
            }, "woo");
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
}
