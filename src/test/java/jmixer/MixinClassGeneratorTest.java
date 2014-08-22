package jmixer;

import java.lang.reflect.Method;

import org.junit.Test;

public class MixinClassGeneratorTest {

	@Test
	public void testMixer() throws Exception {
		String[] args =
			new String[] {
				"-i", "src/test/java",
				"-o", "target/classes",
				"--processors", MixinProcessor.class.getName(),
				"--compile" };
		spoon.Launcher.main(args);
		
		Class<?> cl = Class.forName("jmixer.Duck");
		Object duck = cl.newInstance();
		
		Method[] ms = cl.getMethods();
		
		Method m = cl.getMethod("print");
		m.invoke(duck);
	}
}
