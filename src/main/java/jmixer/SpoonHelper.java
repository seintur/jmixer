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

import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.reference.CtTypeReference;

/**
 * This class provides some helper methods for Spoon.
 * 
 * @author Lionel Seinturier <Lionel.Seinturier@univ-lille1.fr>
 */
public class SpoonHelper {

    /**
     * Return the 0-value literal for the given type reference.
     */
    public static CtLiteral<?> nil( CtTypeReference<?> ctr ) {
        
        CodeFactory cf = ctr.getFactory().Code();
        String qname = ctr.getQualifiedName();
        
        if( qname.equals("boolean") )
            return cf.createLiteral(false);
        if( qname.equals("byte") )
            return cf.createLiteral((byte)0);
        if( qname.equals("char") )
            return cf.createLiteral(' ');
        if( qname.equals("short") )
            return cf.createLiteral((short)0);
        if( qname.equals("int") )
            return cf.createLiteral((int)0);
        if( qname.equals("long") )
            return cf.createLiteral((long)0);
        if( qname.equals("float") )
            return cf.createLiteral((float)0.0);
        if( qname.equals("double") )
            return cf.createLiteral((double)0.0);
        
        return cf.createLiteral(null);
    }

    /**
     * Return a string containing a @see link to reference the specified method.
     */
    public static String toSeeLink( CtMethod<?> method ) {
        String className = method.getDeclaringType().getQualifiedName();
        StringBuffer comment = new StringBuffer(" @see ");
        comment.append(className);
        comment.append('#');
        comment.append(method.getSimpleName());
        comment.append('(');
        boolean first = true;
        for( CtParameter<?> p : method.getParameters() ) {
            if(first) {
                first = false;
            } else {
                comment.append(',');
            }
            comment.append(p.getType().toString());
        }
        comment.append(')');
        return comment.toString();
    }
}
