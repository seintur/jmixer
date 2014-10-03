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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtSimpleType;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * This class provides some helper methods for Spoon.
 * 
 * @author Lionel Seinturier <Lionel.Seinturier@univ-lille1.fr>
 */
public class SpoonHelper {

    /**
     * Return the fully-qualified name (including generic parameters) of the
     * specified {@link CtTypeReference}.
     */
    public static String getClassName( CtTypeReference<?> tref ) { 
        if( tref instanceof CtArrayTypeReference<?> ) {
            /*
             * Workaround for CtTypeReference#getQualifiedName() which returns
             * the type of elements for array types.
             */
            CtArrayTypeReference<?> aref = (CtArrayTypeReference<?>) tref;
            return getClassName(aref.getComponentType())+"[]";
        }
        String result = tref.getQualifiedName();
        List<CtTypeReference<?>> generics = tref.getActualTypeArguments();
        String str = getFormalTypeParametersString(generics);
        result += str;
        return result;
    }

    /**
     * Return a stringified representation (including enclosing < >) of the
     * specified formal type parameters. Return an empty string if no formal
     * parameters are defined.
     */
    public static String getFormalTypeParametersString(
		List<CtTypeReference<?>> ftps ) {
        
    	if( ftps.size() == 0 ) {
            return "";
        }
    	
    	String[] strs = getFormalTypeParameters(ftps);
        StringBuffer sb = new StringBuffer("<");
        for (int i = 0; i < strs.length; i++) {
			if(i>0) sb.append(',');
			sb.append(strs[i]);
		}
        sb.append('>');
        
        return sb.toString();
    }

    /**
     * Return the stringified representations of the specified formal type
     * parameters. Return an empty array if no formal parameters are defined.
     */
    public static String[] getFormalTypeParameters(
		List<CtTypeReference<?>> ftps ) {
        
        if( ftps.size() == 0 ) {
            return new String[0];
        }
        
        int size = ftps.size();
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            CtTypeReference<?> ftp = ftps.get(i);
            
            if( ftp instanceof CtTypeParameterReference ) {
                StringBuffer name = new StringBuffer(ftp.getQualifiedName());
                CtTypeParameterReference tpr = (CtTypeParameterReference) ftp;
                List<CtTypeReference<?>> bounds = tpr.getBounds();
                if( bounds.size() > 0 ) {
                    name.append(tpr.isUpper() ? " extends " : " super ");
                    boolean boundfirst = true;
                    for (CtTypeReference<?> bound : bounds) {
                        if(boundfirst) {boundfirst=false;} else {name.append('&');}
                        name.append(bound.getQualifiedName());
                        List<CtTypeReference<?>> generics = bound.getActualTypeArguments();
                        String boundstring = getFormalTypeParametersString(generics);
                        name.append(boundstring);
                    }
                }
                names[i] = name.toString();
            }
            else {
                names[i] = ftp.getQualifiedName();
            }
        }

        return names;
    }
    
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
     * This method removes the given element from the given source set.
     * 
     * This method is meant to be a workaround for what seems to be an strange
     * behavior concerning the usage which is done with Spoon of TreeSet and
     * CtElement.
     * 
     * As the name suggests, TreeSet instances are binary trees where each node
     * has a left and a right child. Elements are stored in the tree according
     * to the value returned by the compareTo method:
     *    current.compareTo(elementToAdd)
     * When trying to remove an element, the tree is traversed according to the
     * same scheme. Hence, the whole tree is not traversed but only a branch.
     *
     * In {@link SpoonMixinClassProcessor}, we first create a empty target class
     * with the class factory, and then we fill this class with the code
     * resulting from the mixin process. It appears then that a traversal can no
     * longer reach a previously inserted element.
     * 
     * My guess is that, when inserted, the target is associated to a given
     * value of compareTo() and then stored in a particular location in the
     * tree. However, the side effect of modifying the target modifies the value
     * returned by compareTo() *without* modifying the location of the element
     * in the tree. 
     * 
     * I first though that removing the element from the tree before modifying
     * it and reinserting it after having modified it would solve the issue.
     * Unfortunately it does not. The issue must be trickier.
     * 
     * The issue can be circumvented by iterating over the set. The iterator
     * ensures that all the elements are visited (and not just a branch of the
     * tree.)
     * 
     * @param s  the source set
     * @param o  the element to remove
     * @return   true if the element was present and has been removed, 
     *           false otherwise
     */
    public static boolean safeTypeRemoval( CtSimpleType<?> type ) {
        
        Set<?> s = null;
        if( type.isTopLevel() ) {
            s = type.getParent(CtPackage.class).getTypes();
        }
        else {
            s = type.getParent(CtSimpleType.class).getNestedTypes();
        }
        
        Iterator<?> it = s.iterator();
        while( it.hasNext() ) {
            if( it.next().equals(type) ) {
                it.remove();
                return true;
            }
        }
        return false;
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
            /*
             * Use toString() instead of getQualifiedName().
             * For array types, getQualifiedName() returns the type of the
             * elements stored in the array, not the type of the array ([] is
             * then missing). toString() uses SignaturePrinter that deals
             * correctly with arrays.
             * 
             * Check the Spoon mailing list: the bug may have been corrected
             * since I reported it. 
             */
            comment.append(p.getType().toString());
        }
        comment.append(')');
        return comment.toString();
    }
}
