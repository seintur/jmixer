package jmixer;

import org.junit.Test;

public class MixinClassGeneratorTest {

	@Test
	public void testMixer() throws Exception {
		String[] args =
			new String[] {
				"-i", "src/test/java",
				"-o", "target/spooned",
				"--processors", MixinProcessor.class.getName(),
				"--compile" };
		spoon.Launcher.main(args);
	}
}
