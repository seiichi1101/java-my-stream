import java.util.ArrayList;
import java.util.List;

public class Main {

  public static void main(String[] args) {
    // --- Java Stream version ---
    List<Integer> list = new ArrayList<Integer>();
    list.add(4);
    list.add(3);
    list.add(2);
    list.add(1);
    list.add(1);

    List<Integer> res11 = list.stream().filter(Main::isEven).map(Main::add10).sorted().toList();
    List<Integer> res12 = list.stream().filter(Main::isOdd).map(Main::add10).distinct().toList();
    Integer res13 = list.stream().reduce(0, Main::acc);

    System.out.println("--- Java Stream version ---");
    System.err.println("Filtering even numbers and adding 10:");
    res11.forEach(System.out::println);
    System.err.println("Filtering odd numbers and adding 10:");
    res12.forEach(System.out::println);
    System.err.println("Reducing all numbers:");
    System.out.println(res13);

    // --- MyStream version ---
    MyList myList = new MyList();
    myList.add(4);
    myList.add(3);
    myList.add(2);
    myList.add(1);
    myList.add(1);

    List<Integer> res21 = myList.stream().filter(Main::isEven).map(Main::add10).sorted().toList();
    List<Integer> res22 = myList.stream().filter(Main::isOdd).map(Main::add10).distinct().toList();
    Integer res23 = myList.stream().reduce(0, Main::acc);

    System.out.println("--- MyStream version ---");
    System.err.println("Filtering even numbers and adding 10:");
    res21.forEach(System.out::println); // 12, 14
    System.err.println("Filtering odd numbers and adding 10:");
    res22.forEach(System.out::println); // 13, 11
    System.err.println("Reducing all numbers:");
    System.out.println(res23); // 11
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
