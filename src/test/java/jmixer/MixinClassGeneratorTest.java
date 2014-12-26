package jmixer;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import spoon.Launcher;
import spoon.compiler.SpoonCompiler;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;

public class MixinClassGeneratorTest {

	@Test
	public void testMixer() throws Exception {
		
		SpoonCompiler comp = new Launcher().createCompiler();
		comp.addInputSource(new File("./src/test/java/jmixer/Bird.java"));
		comp.addInputSource(new File("./src/test/java/jmixer/Duck.java"));
		comp.addInputSource(new File("./src/test/java/jmixer/Flying.java"));
		comp.addInputSource(new File("./src/test/java/jmixer/Swimming.java"));
		comp.addInputSource(new File("./src/main/java/jmixer/Mixin.java"));
		comp.build();
		
		Factory factory = comp.getFactory();
		CtClass<?> duck = factory.Class().get("jmixer.Duck");
		CtClass<?> flying = factory.Class().get("jmixer.Flying");
		CtClass<?> swimming = factory.Class().get("jmixer.Swimming");
		
		MixinClassGenerator mcg = new MixinClassGenerator(factory);
		mcg.generate(duck,flying,swimming);
		
		CtMethod<?> fly = duck.getMethod("fly");
		Assert.assertEquals("void",fly.getType().toString());

		CtMethod<?> swim = duck.getMethod("swim");
		Assert.assertEquals("void",swim.getType().toString());
	}
}
