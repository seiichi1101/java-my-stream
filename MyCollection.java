public interface MyCollection extends Iterable<Integer> {
  default MyStream stream(){
    return new MyReferencePipeline.Head(this);
  }
  int size();
}
