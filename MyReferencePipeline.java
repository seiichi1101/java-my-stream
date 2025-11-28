import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class MyReferencePipeline implements MyStream {
  private final MyReferencePipeline previousStage;

  private final MyCollection sourceCollection;

  MyReferencePipeline(MyCollection sourceCollection) {
    this.previousStage = null;
    this.sourceCollection = sourceCollection;
  }

  MyReferencePipeline(MyReferencePipeline previousStage) {
    this.previousStage = previousStage;
    this.sourceCollection = null;
  }

  public static class Head extends MyReferencePipeline {
    public Head(MyCollection sourceCollection) {
      super(sourceCollection);
    }

    @Override
    MySink opWrapSink(MySink downstream) {
      throw new UnsupportedOperationException("Head should not wrap sink");
    }
  }

  static abstract class StatelessOp extends MyReferencePipeline {
    StatelessOp(MyReferencePipeline previousStage) {
      super(previousStage);
    }
  }

  static abstract class StatefulOp extends MyReferencePipeline {
    StatefulOp(MyReferencePipeline previousStage) {
      super(previousStage);
    }
  }

  abstract MySink opWrapSink(MySink downstream);

  private MyCollection getSourceCollection() {
    if (previousStage == null) {
      return sourceCollection;
    }
    return previousStage.getSourceCollection();
  }

  @Override
  public MyStream filter(Predicate<Integer> predicate){
    return new StatelessOp(this) {
      @Override
      MySink opWrapSink(MySink downstream) {
        return new MySink() {
          @Override
          public void begin(long size) {}

          @Override
          public void accept(Integer t) {
            if (predicate.test(t)) {
              downstream.accept(t);
            }
          }

          @Override
          public void end() {
            downstream.end();
          }
        };
      }
    };
  }

  @Override
  public MyStream map(Function<Integer, Integer> mapper){
    return new StatelessOp(this) {
      @Override
      MySink opWrapSink(MySink downstream) {
        return new MySink() {
          @Override
          public void begin(long size) {}

          @Override
          public void accept(Integer t) {
            Integer mappedValue = mapper.apply(t);
            downstream.accept(mappedValue);
          }

          @Override
          public void end() {
            downstream.end();
          }
        };
      }
    };
  }

    @Override
  public MyStream sorted(){
    return new StatefulOp(this) {
      @Override
      MySink opWrapSink(MySink downstream) {
        return new MySink() {
          private List<Integer> buffer = new ArrayList<>();

          @Override
          public void begin(long size) {}

          @Override
          public void accept(Integer t) {
            // Accumulate elements in buffer
            buffer.add(t);
          }

          @Override
          public void end() {
            downstream.begin(-1);
            buffer.sort(Integer::compareTo);
            downstream.end();
          }
        };
      }
    };
  }

  @Override
  public MyStream distinct(){
    return new StatefulOp(this) {
      @Override
      MySink opWrapSink(MySink downstream) {
        return new MySink() {
          private Set<Integer> seen = new HashSet<>();

          @Override
          public void begin(long size) {}

          @Override
          public void accept(Integer t) {
            if (seen.add(t)) { // Returns true if new element
              downstream.accept(t);
            }
          }

          @Override
          public void end() {
            downstream.end();
          }
        };
      }
    };
  }

  @Override
  public List<Integer> toList(){
    List<Integer> result = new ArrayList<>();
    MySink resultSink = new MySink() {
      @Override
      public void begin(long size) {}

      @Override
      public void accept(Integer t) {
        result.add(t);
      }

      @Override
      public void end() {}
    };

    evaluate(resultSink);
    return result;
  }

  @Override
  public Integer reduce(Integer init, BiFunction<Integer, Integer, Integer> acc){
    Integer [] result = new Integer[] {init}; // Use array to allow modification in inner class
    MySink reduceSink = new MySink() {
      @Override
      public void begin(long size) {}

      @Override
      public void accept(Integer t) {
        result[0] = acc.apply(result[0], t);
      }

      @Override
      public void end() {}
    };

    evaluate(reduceSink);
    return result[0];
  }

  private void evaluate(MySink terminalMySink){
    // Build Sink pipeline
    MySink wrappedMySink = wrapMySink(terminalMySink);

    // Execute pipeline
    wrappedMySink.begin(-1);

    MyCollection source = getSourceCollection();
    for (Integer element : source) {
      wrappedMySink.accept(element);
    }
    // Signal end of processing
    wrappedMySink.end();
  }

  // Build Sink pipeline (traverse backward from current stage to build chain)
  private MySink wrapMySink(MySink terminalSink) {
    MySink sink = terminalSink;

    MyReferencePipeline p = this;
    while (p.previousStage != null) {
      sink = p.opWrapSink(sink);
      p = p.previousStage;
    }

    return sink;
  }

}
