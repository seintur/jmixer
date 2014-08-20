package jmixer;

@Mixin(value={Flying.class, Swimming.class},i=0,c=String.class,s={"g","h"},cc={String.class,Integer.class})
public class Duck extends Bird {

	@Override
	public void print() {
		System.out.println("Bird");
		super.print();
	}
}
