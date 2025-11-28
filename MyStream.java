import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface MyStream {
  MyStream filter(Predicate<Integer> predicate);
  MyStream map(Function<Integer, Integer> mapper);
  MyStream distinct();
  MyStream sorted();
  List<Integer> toList();
  Integer reduce(Integer init, BiFunction<Integer, Integer, Integer> accumulator);
}
