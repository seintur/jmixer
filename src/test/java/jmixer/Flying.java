package jmixer;

public abstract class Flying {

	public void fly() {}
	public void print() {
		System.out.println("Flying");
		_super_print();
	}

	protected abstract void _super_print();
}
