/***
 * JMixer
 * Copyright (C) 2014 Inria, University Lille 1
 *
 * This software is governed by the CeCILL-C License under French law and
 * abiding by the rules of distribution of free software. You can use, modify 
 * and/or redistribute the software under the terms of the CeCILL-C license as 
 * circulated by CEA, CNRS and INRIA at http://www.cecill.info. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the CeCILL-C License for more details.
 *  
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * Author: Lionel Seinturier
 */

package jmixer;

import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.factory.ClassFactory;
import spoon.reflect.factory.Factory;

/**
 * An annotation processor for the @{@link Mixin} annotation.
 * 
 * @author Lionel Seinturier <Lionel.Seinturier@univ-lille1.fr>
 */
public class MixinProcessor
extends AbstractAnnotationProcessor<Mixin,CtClass<?>> {

	@Override
	public void process( Mixin annotation, CtClass<?> ctclass ) {
		
		Factory f = ctclass.getFactory();
		ClassFactory cf = f.Class();

		String[] s = annotation.s();
		System.out.println(s);
		
		Class<?>[] mixes = annotation.value();
		CtClass<?>[] ctmixes = new CtClass<?>[mixes.length + 1];
		ctmixes[0] = ctclass;
		for (int i = 1; i < mixes.length; i++) {
			Class<?> mix = mixes[i-1];
			ctmixes[i] = cf.get(mix);
		}
		
		MixinClassGenerator mcg = new MixinClassGenerator(f);
		CtClass<?> c = mcg.generate("A",ctmixes);
		System.out.println(c);
	}
}
