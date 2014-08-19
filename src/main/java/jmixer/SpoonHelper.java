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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import spoon.reflect.code.CtLiteral;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtSimpleType;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.ReferenceFilter;
import spoon.reflect.visitor.filter.ReferenceTypeFilter;
import spoon.support.reflect.declaration.CtSimpleTypeImpl;

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
     * Return the names of the specified formal type parameters. Return an empty
     * array if no formal parameters are defined.
     * 
     * @since 2.2.6
     */
    public static String[] getFormalTypeParameterNames(
		List<CtTypeReference<?>> ftps ) {
        
        if( ftps.size() == 0 ) {
            return new String[0];
        }
        
        int size = ftps.size();
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            CtTypeReference<?> ftp = ftps.get(i);
            names[i] = ftp.getQualifiedName();
        }

        return names;
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
     * Return the types of the parameters for the specified {@link CtExecutable}
     * (e.g. such as a {@link CtMethod}).
     */
    public static CtTypeReference<?>[] getParameterTypes( CtExecutable<?> exec ) {
        List<CtParameter<?>> params = exec.getParameters();
        CtTypeReference<?>[] parameterTypes =
            new CtTypeReference<?>[params.size()];
        int i=0;
        for (CtParameter<?> param : params) {
            CtTypeReference<?> tref = param.getType();
            parameterTypes[i] = tref;
            i++;
        }
        return parameterTypes;
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
     * Return the list of {@link CtClass}es corresponding to the given list of
     * class names.
     * 
     * @throws IllegalArgumentException
     *      if one of the {@link CtClass}es can not be retrieved
     */
    public static List<CtClass<?>> toCtClass( Factory f, List<String> srcs )
    throws IllegalArgumentException {
        
        List<CtClass<?>> results = new ArrayList<CtClass<?>>();
        
        for (String src : srcs) {
            CtClass<?> ctclass = f.Class().get(src);
            if( ctclass == null ) {
                String msg = "No such CtClass: "+src;
                throw new IllegalArgumentException(msg);
            }
            results.add(ctclass);
        }
        
        return results;
    }

    /**
     * Return the literal for the given type reference.
     */
    public static CtLiteral<?> toCtLiteral( CtTypeReference<?> ctr, String value )
    throws NumberFormatException, IllegalArgumentException {
        
        CodeFactory cf = ctr.getFactory().Code();
        String qname = ctr.getQualifiedName();
        
        if( qname.equals("boolean") )
            return cf.createLiteral(Boolean.valueOf(value));
        if( qname.equals("byte") )
            return cf.createLiteral(Byte.valueOf(value));
        if( qname.equals("char") ) {
            if( value.length() != 1 ) {
                String msg = "Only 1-length value String supported for the char type";
                throw new IllegalArgumentException(msg);
            }
            return cf.createLiteral(value.charAt(0));
        }
        if( qname.equals("short") )
            return cf.createLiteral(Short.valueOf(value));
        if( qname.equals("int") )
            return cf.createLiteral(Integer.valueOf(value));
        if( qname.equals("long") )
            return cf.createLiteral(Long.valueOf(value));
        if( qname.equals("float") )
            return cf.createLiteral(Float.valueOf(value));
        if( qname.equals("double") )
            return cf.createLiteral(Double.valueOf(value));
        if( qname.equals(String.class.getName()) )
            return cf.createLiteral(value);
        
        String msg = "Unsupported type: "+ctr.getQualifiedName();
        throw new IllegalArgumentException(msg);
    }
    
    /**
     * Return a string of the form <code>typename.name(filename:line)</code> for
     * the specified {@link CtNamedElement}. When displayed, such a string can
     * be interpreted by Eclipse as a link to a source code position.
     */
    public static String toEclipseClickableString( CtNamedElement ne ) {
        StringBuffer msg = new StringBuffer();
        SourcePosition sp = ne.getPosition();
        msg.append(sp.getCompilationUnit().getMainType().getQualifiedName());
        msg.append('.');
        msg.append(ne.getSimpleName());
        msg.append('(');
        msg.append(sp.getFile().getName());
        msg.append(':');
        msg.append( sp.getLine());
        msg.append(')');
        return msg.toString();        
    }

    /**
     * Return the {@link Modifier} constant corresponding to the specified
     * {@link ModifierKind}.
     */
    public static int toModifier( ModifierKind mk )
    throws IllegalArgumentException {
        
        if( mk == ModifierKind.PUBLIC ) {
            return Modifier.PUBLIC;
        }
        if( mk == ModifierKind.PROTECTED ) {
            return Modifier.PROTECTED;
        }
        if( mk == ModifierKind.PRIVATE ) {
            return Modifier.PRIVATE;
        }
        if( mk == ModifierKind.ABSTRACT ) {
            return Modifier.ABSTRACT;
        }
        if( mk == ModifierKind.STATIC ) {
            return Modifier.STATIC;
        }
        if( mk == ModifierKind.FINAL ) {
            return Modifier.FINAL;
        }
        if( mk == ModifierKind.TRANSIENT ) {
            return Modifier.TRANSIENT;
        }
        if( mk == ModifierKind.VOLATILE ) {
            return Modifier.VOLATILE;
        }
        if( mk == ModifierKind.SYNCHRONIZED ) {
            return Modifier.SYNCHRONIZED;
        }
        if( mk == ModifierKind.NATIVE ) {
            return Modifier.NATIVE;
        }
        if( mk == ModifierKind.STRICTFP ) {
            return Modifier.STRICT;
        }
        
        String msg = "Unsupported ModifierKind: "+mk;
        throw new IllegalArgumentException(msg);
    }
    
    /**
     * Return the set of {@link ModifierKind} corresponding to the specified
     * {@link Modifier}.
     */
    public static Set<ModifierKind> toModifierKinds( int modifier ) {
        
        Set<ModifierKind> mks = new HashSet<ModifierKind>();
        
        if( (modifier & Modifier.PUBLIC) == Modifier.PUBLIC ) {
            mks.add(ModifierKind.PUBLIC);
        }
        if( (modifier & Modifier.PROTECTED) == Modifier.PROTECTED ) {
            mks.add(ModifierKind.PROTECTED);
        }
        if( (modifier & Modifier.PRIVATE) == Modifier.PRIVATE ) {
            mks.add(ModifierKind.PRIVATE);
        }
        if( (modifier & Modifier.ABSTRACT) == Modifier.ABSTRACT ) {
            mks.add(ModifierKind.ABSTRACT);
        }
        if( (modifier & Modifier.STATIC) == Modifier.STATIC ) {
            mks.add(ModifierKind.STATIC);
        }
        if( (modifier & Modifier.FINAL) == Modifier.FINAL ) {
            mks.add(ModifierKind.FINAL);
        }
        if( (modifier & Modifier.TRANSIENT) == Modifier.TRANSIENT ) {
            mks.add(ModifierKind.TRANSIENT);
        }
        if( (modifier & Modifier.VOLATILE) == Modifier.VOLATILE ) {
            mks.add(ModifierKind.VOLATILE);
        }
        if( (modifier & Modifier.SYNCHRONIZED) == Modifier.SYNCHRONIZED ) {
            mks.add(ModifierKind.SYNCHRONIZED);
        }
        if( (modifier & Modifier.NATIVE) == Modifier.NATIVE ) {
            mks.add(ModifierKind.NATIVE);
        }
        if( (modifier & Modifier.STRICT) == Modifier.STRICT ) {
            mks.add(ModifierKind.STRICTFP);
        }
        
        return mks;
    }

    /**
     * Return a string containing a @see link to reference the specified field.
     */
    public static String toSeeLink( CtField<?> field ) {
        String className = field.getDeclaringType().getQualifiedName();
        StringBuffer comment = new StringBuffer(" @see ");
        comment.append(className);
        comment.append('#');
        comment.append(field.getSimpleName());
        return comment.toString();        
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
             * then missing). toString() uses SignaturePrinter which deals
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

    /**
     * Return a string containing a @see link to reference the specified simple
     * type.
     * 
     * @since 2.2.4
     */
    public static String toSeeLink( CtSimpleType<?> stype ) {
        String qname = stype.getQualifiedName();
        StringBuffer comment = new StringBuffer(" @see ");
        comment.append(qname);
        return comment.toString();        
    }
    
    /**
     * Update all source type references in the specified element to the
     * specified target type reference.
     * 
     * @param element  the element
     * @param src      the source type reference
     * @param dst      the target type reference
     */
    public static void updateTypeReferences(
        CtElement element, CtTypeReference<?> src, CtTypeReference<?> dst ) {
        
        final String srcQName = src.getQualifiedName();
        final String dstSName = dst.getSimpleName();
        final CtPackageReference dstPRef = dst.getPackage();
        
        Query.getReferences(
            element,
            new ReferenceTypeFilter<CtTypeReference<?>>(CtTypeReference.class) {
            	@Override
                public boolean matches( CtTypeReference<?> tref ) {
                    String qname = tref.getQualifiedName();
                    if( qname.equals(srcQName) ) {
                        tref.setPackage(dstPRef);
                        tref.setSimpleName(dstSName);
                    }
                    return false;
                }
            }
        );
    }
    
    /**
     * This method returns the executables referenced by the specified type.
     * 
     * This method is a workaround for a bug with {@link
     * CtTypeReference#getAllExecutables()} of Spoon 1.4.2. See
     * http://gforge.inria.fr/tracker/index.php?func=detail&aid=9339&group_id=73&atid=371
     * The original code does not visit super interfaces to retrieve all
     * executable references.
     * 
     * @param tref  the type reference
     * @return      declared and inherited executables referenced by the type 
     * @since 2.2.4
     */
    public static Collection<CtExecutableReference<?>> getAllExecutables(
		CtTypeReference<?> tref ) {
    	
    	Collection<CtExecutableReference<?>> erefs = tref.getAllExecutables();
    	
    	CtSimpleType<?> stype = tref.getDeclaration();
    	if( stype!=null && stype instanceof CtType<?> ) {
    		CtType<?> type = (CtType<?>) stype;
    		Set<CtTypeReference<?>> supers = type.getSuperInterfaces();
    		for (CtTypeReference<?> sup : supers) {
    			Collection<CtExecutableReference<?>> col =
    				getAllExecutables(sup);
		    	erefs.addAll(col);
			}
    	}

    	return erefs;
    }
    
    /**
     * This method returns the types referenced by the specified type.
     * 
     * This method is a workaround for a bug with {@link
     * CtSimpleTypeImpl#getUsedTypes(boolean)} of Spoon 1.4.2. See
     * http://gforge.inria.fr/tracker/index.php?func=detail&aid=9384&group_id=73&atid=371
     * The original code throws an NPE whenever an inner type is encountered. This
     * is due to the fact that the package reference for .
     * 
     * @param stype  the type
     * @param includeSamePackage
     * 				<code>true</code> if the method should return also the types
     * 				located in the same package as the type
     * @return  the types referenced by the specified type 
     * @since 2.2.4
     */
    public static Set<CtTypeReference<?>> getUsedTypes(
		CtSimpleType<?> stype, boolean includeSamePackage ) {
    	
		Set<CtTypeReference<?>> ret = new HashSet<CtTypeReference<?>>();
		CtPackageReference pack = stype.getPackage().getReference();
		
		ReferenceFilter<CtTypeReference<?>> rf =
			new ReferenceTypeFilter<CtTypeReference<?>>(CtTypeReference.class);
		List<CtTypeReference<?>> trefs = Query.getReferences(stype,rf);
		
		for (CtTypeReference<?> tref : trefs ) {
			
			if( tref.isPrimitive() )  continue;
			if( tref instanceof CtArrayTypeReference<?> )  continue;
			
			String s = tref.toString();
			if( s.length() == 0 )  continue;
			if( s.equals(CtTypeReference.NULL_TYPE_NAME) )  continue;
			
			CtPackageReference pref = getPackage(tref);
			if( pref != null ) {
				if( pref.toString().equals("java.lang") )  continue;
				if( !includeSamePackage && pref.equals(pack) )  continue;
			}
			
			ret.add(tref);
		}
		
		return ret;
    }
    
    /**
     * Return the package reference associated to the specified type. This
     * method complements {@link CtTypeReference#getPackage()} which returns
     * <code>null</code> for inner types. For the inner types, this method
     * returns the package reference of the top level type declaring the
     * specified type.
     * 
     * This method returns <code>null</code> if the specified type is inner and
     * does not have a declaring type with a package reference.
     * 
     * @param tref  the type reference
     * @return      the reference of the package declaring the type
     * @since 2.2.4
     */
    public static CtPackageReference getPackage( CtTypeReference<?> tref ) {
    	CtPackageReference pref = tref.getPackage();
    	while( pref == null ) {
    		tref = tref.getDeclaringType();
    		if( tref == null ) {
    			// No declaring type. Then no package neither.
    			return null;
    		}
    		pref = tref.getPackage();
    	}
    	return pref;
    }
}
