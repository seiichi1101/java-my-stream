import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    List<Integer> list1 = new ArrayList<Integer>();
    list1.add(1);
    list1.add(2);
    list1.add(3);
    list1.add(4);
    list1.add(5);
    // Stream<Integer> stream1 = list1.stream();
    // List<Integer> res11 = stream1.filter(Main::isEven).collect(Collectors.toList());
    // List<Integer> res12 = stream1.filter(Main::isOdd).collect(Collectors.toList());
    // Integer res13 = stream1.filter(Main::isEven).reduce(0, Main::acc);

    // res11.forEach(System.out::println);
    // res12.forEach(System.out::println);
    // System.out.println(res13);


    // --- MyCollection version ---
    MyList list2 = new MyList();
    list2.add(1);
    list2.add(2);
    list2.add(3);
    list2.add(4);
    list2.add(5);

    // List<Integer> res21 = list2.stream().filter(Main::isEven).collect();
    // Sink
    MyStream stream2 = list2.stream();
    List<Integer> res21 = stream2.filter(Main::isEven).map(Main::add10).collect();
    List<Integer> res22 = stream2.filter(Main::isOdd).map(Main::add10).collect();
    Integer res23 = stream2.reduce(0, Main::acc);

    res21.stream().forEach(System.out::println);
    res22.stream().forEach(System.out::println);
    System.out.println(res23);
  }

  static int add10(int val){
    return val + 10;
  }
  static boolean isEven(int val){
    return val % 2 == 0;
  }
  static boolean isOdd(int val){
    return val % 2 != 0;
  }
  static Integer acc(Integer acc, Integer val){
    return acc + val;
  }

}
