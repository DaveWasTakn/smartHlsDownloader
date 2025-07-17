package org.davesEnterprise.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

public class TransposeGatherer<T> implements Gatherer<List<T>, List<List<T>>, List<T>> {

    /**
     * A function that produces an instance of the intermediate state used for
     * this gathering operation.
     *
     * @return A function that produces an instance of the intermediate state
     * used for this gathering operation
     * @implSpec The implementation in this interface returns
     * {@link #defaultInitializer()}.
     */
    @Override
    public Supplier<List<List<T>>> initializer() {
        return ArrayList::new;
    }

    /**
     * A function which integrates provided elements, potentially using
     * the provided intermediate state, optionally producing output to the
     * provided {@link Downstream}.
     *
     * @return a function which integrates provided elements, potentially using
     * the provided state, optionally producing output to the provided
     * Downstream
     */
    @Override
    public Integrator<List<List<T>>, List<T>, List<T>> integrator() {
        return (state, element, downstream) -> {
            if (state.isEmpty()) {
                Stream.generate(ArrayList<T>::new).limit(element.size()).forEach(state::add);
            }

            for (int i = 0; i < element.size(); i++) {
                List<T> di = state.get(i);
                di.add(element.get(i));
                state.set(i, di);
            }

            return true;
        };
    }

    /**
     * A function which accepts the final intermediate state
     * and a {@link Downstream} object, allowing to perform a final action at
     * the end of input elements.
     *
     * @return a function which transforms the intermediate result to the final
     * result(s) which are then passed on to the provided Downstream
     * @implSpec The implementation in this interface returns
     * {@link #defaultFinisher()}.
     */
    @Override
    public BiConsumer<List<List<T>>, Downstream<? super List<T>>> finisher() {
        return (state, downstream) -> state.forEach(downstream::push);
    }

}

