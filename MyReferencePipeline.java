import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

// Mimics Java's ReferencePipeline (holds reference to previous pipeline stage)
// Implements MyStream Interface
// Abstract class because of abstract method opWrapSink
public abstract class MyReferencePipeline implements MyStream {
  // Previous pipeline stage (null for Head)
  private final MyReferencePipeline previousStage;

  // Source collection (only Head holds this)
  private final MyCollection sourceCollection;

  // Constructor for Head
  // Just holds data
  MyReferencePipeline(MyCollection sourceCollection) {
    this.previousStage = null;
    this.sourceCollection = sourceCollection;
  }

  // Constructor for intermediate operations
  // Just adds intermediate processing like map or filter
  MyReferencePipeline(MyReferencePipeline previousStage) {
    this.previousStage = previousStage;
    this.sourceCollection = null;
  }

  // Head subclass
  // Subclass is implemented because constructor cannot be used directly in Abstract class
  public static class Head extends MyReferencePipeline {
    public Head(MyCollection sourceCollection) {
      super(sourceCollection);
    }

    @Override
    MySink opWrapSink(MySink downstream) {
      throw new UnsupportedOperationException("Head should not wrap sink");
    }
  }

  // Subclass for stateless intermediate operations (filter, map, etc.)
  // Subclass is implemented because constructor cannot be used directly in Abstract class
  static abstract class StatelessOp extends MyReferencePipeline {
    StatelessOp(MyReferencePipeline previousStage) {
      super(previousStage);
    }
  }

  // Subclass for stateful intermediate operations (sorted, distinct, etc.)
  // Operations that need to collect all elements before processing
  static abstract class StatefulOp extends MyReferencePipeline {
    StatefulOp(MyReferencePipeline previousStage) {
      super(previousStage);
    }
  }

  // Wrap this stage's operation in a Sink (implemented by subclasses)
  // Actual filter and map processing is implemented here
  abstract MySink opWrapSink(MySink downstream);

  // Get source (traverse to root)
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
          public void begin(long size) {
            downstream.begin(-1);
          }

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

          @Override
          public boolean cancellationRequested() {
            return downstream.cancellationRequested();
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
          public void begin(long size) {
            downstream.begin(-1);
          }

          @Override
          public void accept(Integer t) {
            Integer mappedValue = mapper.apply(t);
            downstream.accept(mappedValue);
          }

          @Override
          public void end() {
            downstream.end();
          }

          @Override
          public boolean cancellationRequested() {
            return downstream.cancellationRequested();
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
          public void begin(long size) {
            // Do nothing at begin (wait until all elements are collected)
          }

          @Override
          public void accept(Integer t) {
            // Accumulate elements in buffer
            buffer.add(t);
          }

          @Override
          public void end() {
            // At end, sort all elements and flow to downstream
            downstream.begin(-1);
            buffer.stream()
                  .sorted()
                  .forEach(downstream::accept);
            downstream.end();
          }

          @Override
          public boolean cancellationRequested() {
            return downstream.cancellationRequested();
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
          public void begin(long size) {
            downstream.begin(-1);
          }

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

          @Override
          public boolean cancellationRequested() {
            return downstream.cancellationRequested();
          }
        };
      }
    };
  }

  @Override
  public List<Integer> collect(){
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

    // Call internal helper to execute processing
    evaluatePipeline(resultSink);
    return result;
  }

  @Override
  public Integer reduce(Integer init, BiFunction<Integer, Integer, Integer> accumulator){
    Integer[] result = new Integer[]{ init };  // Wrap in array to hold reference
    MySink reduceSink = new MySink() {
      @Override
      public void begin(long size) {}

      @Override
      public void accept(Integer t) {
        result[0] = accumulator.apply(result[0], t);
      }

      @Override
      public void end() {}
    };

    // Call internal helper to execute pipeline
    evaluatePipeline(reduceSink);
    // Return updated value
    return result[0];
  }

  // Generic terminal operation: execute pipeline with a Sink
  // In Java's standard Stream API, terminal operations like collect are implemented internally with this mechanism
  private void evaluatePipeline(MySink terminalMySink){
    // Build Sink pipeline
    MySink wrappedMySink = wrapMySink(terminalMySink);

    // Execute pipeline
    wrappedMySink.begin(-1);

    MyCollection source = getSourceCollection();
    for (Integer element : source) {
      wrappedMySink.accept(element);
      if (wrappedMySink.cancellationRequested()) {
        break;
      }
    }

    wrappedMySink.end();
  }

  // Build Sink pipeline (traverse backward from current stage to build chain)
  private MySink wrapMySink(MySink terminalSink) {
    MySink sink = terminalSink;

    // Traverse pipeline from current stage to root
    MyReferencePipeline p = this;
    while (p.previousStage != null) {
      sink = p.opWrapSink(sink); // Wrap previous stage's Sink at current stage
      p = p.previousStage;
    }

    return sink;
  }

}
