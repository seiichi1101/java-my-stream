import java.util.Iterator;

public class MyList implements MyCollection {
  private static final int DEFAULT_CAPACITY = 10;
  private Object[] elements;
  private int size;

  public MyList(){
    elements = new Object[DEFAULT_CAPACITY];
  }

  void add(int i) {
    elements[size++] = i;
  }

  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public Integer next() {
        return (Integer) elements[index++];
      }
    };
  }

  @Override
  public int size() {
    return size;
  }
}
