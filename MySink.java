// Sink interface definition (mimics Java Stream API internal implementation)
public interface MySink {
  void begin(long size);
  void accept(Integer t);
  void end();
  default boolean cancellationRequested() { return false; }
}
