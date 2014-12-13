package jmixer;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

public class MixinClassGeneratorTest {

	@Test
	public void testMixer() throws Exception {
		
		String[] args =
			new String[] {
				"-i", "src/test/java",
				"-o", "target/spooned",
				"-b", "target/spooned-classes",
				"--processors", MixinProcessor.class.getName(),
				"--compile" };
		spoon.Launcher.main(args);
		
		URLClassLoader urlcl =
			new URLClassLoader(new URL[]{new URL("file:target/spooned-classes/")},null);
		
		urlcl.loadClass("jmixer.Duck").getMethod("fly");
		urlcl.loadClass("jmixer.Duck").getMethod("swim");
		urlcl.close();
	}
}
