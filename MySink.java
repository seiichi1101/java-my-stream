public interface MySink {
  void begin(long size);
  void accept(Integer i);
  void end();
}
