package jmixer;

import org.junit.Test;

public class MixinClassGeneratorTest {

	@Test
	public void testMixer() throws Exception {
		String[] args =
			new String[] {
				"-i", "src/test/java",
				"-d", "target/classes",
				"--processors", MixinProcessor.class.getName(),
				"--compile" };
		spoon.Launcher.main(args);
		
		Duck.class.getMethod("fly");
		Duck.class.getMethod("swim");
	}
}
