package jmixer;

public abstract class Swimming {

	public void swim() {}
	public void print() {
		System.out.println("Swimming");
		_super_print();
	}
	
	protected abstract void _super_print();
}
