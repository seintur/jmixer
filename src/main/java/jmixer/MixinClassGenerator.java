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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import spoon.reflect.Factory;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtReturn;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.AbstractFilter;
import spoon.reflect.visitor.filter.ReferenceTypeFilter;

/**
 * This class implements the mixin algorithm.
 * 
 * @author Lionel Seinturier <Lionel.Seinturier@univ-lille1.fr>
 */
public class MixinClassGenerator {
    
    /** The prefix used in mixin classes to denote required fields and methods. */
    final public static String THIS = "_this_";

    /** The prefix used in mixin classes to denote overridden methods. */
    final public static String SUPER = "_super_";

    /** The separator for mixed method names. */
    final public static char MIXED_METH_SEP = '$';
  
  
    private Factory factory;
  
    /**
     * @param factory  a Spoon factory to retrieve and generate code
     */
    public MixinClassGenerator( Factory factory ) {
        this.factory = factory;
    }
  
    /**
     * Mix the specified classes in the target class.
     * 
     * @param targetClass  the target class
     * @param srcClasses   the classes to mix
     */
    public void generate( CtClass<?> target, CtClass<?>[] srcClasses ) {      
      processMethods(target,srcClasses);
      processFields(target);
  }

  /**
   * Remove the _this_ prefix in all field accesses defined in the given class.
   */
  private void processFields( CtClass<?> target ) {
      
      // Get all the field accesses contained in target
      List<CtFieldAccess<?>> fas =
          Query.getElements(
              target,
              new AbstractFilter<CtFieldAccess<?>>(CtFieldAccess.class) {
                  public boolean matches( CtFieldAccess<?> fa) {
                      return true;
                  }
              }
      );
      
      // Remove the _this_ prefix
      for (CtFieldAccess<?> fa : fas) {
          CtFieldReference<?> cfr = fa.getVariable();
          String fieldName = cfr.getSimpleName();
          if( fieldName.startsWith(THIS) ) {
              String newFieldName =
                  fieldName.substring(THIS.length());
              cfr.setSimpleName(newFieldName);
          }
      }
  }
  
  /**
   * Mix the methods defined in the given classes and insert the result in the
   * given target class.
   * 
   * @param target      the target class
   * @param srcClasses  the classes containing the methods to be mixed
   */
  private void processMethods( CtClass<?> target, CtClass<?>[] srcClasses ) {
      
      /*
       * Add @see tags in target to reference the mixed classes.
       */
      setSource(srcClasses,target);
      
      /*
       * Each element in meths contains the set of methods coming from a
       * mixed class.
       */
      Set<CtMethod<?>>[] meths = new Set[ srcClasses.length ];
      
      for (int i = srcClasses.length-1; i > -1 ; i--) {
          
          /*
           * Get the methods to be mixed from the source class.
           */
          CtClass<?> src = srcClasses[i];
          Set<CtMethod<?>> methods = src.getMethods();
          meths[i] = new HashSet<CtMethod<?>>();
          
          for (CtMethod<?> method : methods) {
              
              /*
               * Skip abstract methods.
               * Given the definition of the mixin mechanism, these are
               * methods such as: _this_...() or _super_...().
               */
              if( method.hasModifier(ModifierKind.ABSTRACT) ) {
                  continue;
              }
              
              /*
               * Insert the method.
               */
              CtMethod<?> previous = getPreviouslyDefinedMethod(meths,i,method);
              CtMethod<?> newMeth = insertMethod(method,target,previous);
              
              if( newMeth == null ) {
                  continue;
              }
              
              /*
               * Add a @see Javadoc comment to the inserted method to trace it
               * back to its source.
               * Update the type references.
               * Update the calls to _super_... methods.
               */
              meths[i].add(newMeth);
              setSource(method,newMeth);
              updateTypeRefs(newMeth,src,target);
              updateCallsTo_this_Method(newMeth);
              updateCallsTo_super_Method(newMeth,meths[i]);
          }
          
          /*
           * Insert all fields from the source class to the target class.
           * Skip fields with a _this_ prefix.
           */
          List<CtField<?>> fields = src.getFields();
          for (CtField<?> field : fields) {
              if( ! field.getSimpleName().startsWith(THIS) ) {
                  CtField<?> newField = factory.Field().create(target,field);
                  setSource(field,newField);
              }
          }
          
          /*
           * Add implemented interfaces.
           */
          Set<CtTypeReference<?>> supers = src.getSuperInterfaces();
          for (CtTypeReference<?> s : supers) {
			target.getSuperInterfaces().add(s);
		}
      }
      
      /*
       * For each abstract _super_... method, add an empty method to cleanly
       * close the chain.
       */
      for (int i = srcClasses.length-1; i > -1 ; i--) {
          
          CtClass<?> src = srcClasses[i];
          Set<CtMethod<?>> methods = src.getMethods();
          
          for (CtMethod<?> method : methods) {
              
              if( ! ( method.hasModifier(ModifierKind.ABSTRACT) &&
                      method.getSimpleName().startsWith(SUPER) ) ) {
                  continue;
              }
              
              /*
               * Create a temporary method:
               * - remove the _super_ prefix
               * - remove the abstract modifier
               * - add an empty body
               */
              CtMethod<?> tmp = factory.Core().clone(method);
              String name = tmp.getSimpleName().substring(SUPER.length());
              tmp.setSimpleName(name);
              tmp.getModifiers().remove(ModifierKind.ABSTRACT);
              
              CtBlock<?> body = factory.Core().createBlock();
              if( ! tmp.getType().getQualifiedName().equals("void") ) {
                  CtReturn<?> retstat = factory.Core().createReturn();
                  CtLiteral<?> lit = SpoonHelper.nil(tmp.getType());
                  retstat.setReturnedExpression((CtLiteral)lit);
                  body.insertBegin(retstat);
              }
              tmp.setBody((CtBlock)body);
              
              CtMethod<?> previous = getPreviouslyDefinedMethod(meths,-1,tmp);
              if( previous == null ) {
                  continue;
              }
              
              insertMethod(tmp,target,previous);
          }
      }
  }
  
  /**
   * Insert a method in a target class.
   * <code>previous</code> may be null or may reference a method with a same
   * signature that has already been inserted. In such a case, rename the new
   * method following the name$99 scheme.
   * 
   * @param method    the current method
   * @param target    the target class
   * @param previous  the previously inserted method with the same signature
   * @return  the newly inserted method
   */
  private CtMethod<?> insertMethod(
          CtMethod<?> method, CtClass<?> target, CtMethod<?> previous ) {
      
      /*
       * Check whether the current method has already been inserted.
       * If not simply insert it in the target class.
       * If so, rename it to something like name$99 before inserting it.
       */
      CtMethod<?> newMeth;                

      if( previous == null ) {
          newMeth = factory.Method().create(target,method,true);
      }
      else {
          
          /*
           * A method with the same name has already been inserted.
           * Compute the name of the new method (something like name$99).
           */
          newMeth = factory.Core().clone(method);          
          String name = getSuperMethodName(previous);
          newMeth.setSimpleName(name);
          newMeth.setVisibility(ModifierKind.PRIVATE);
          
          newMeth = factory.Method().create(target,newMeth,true);                    
      }
      
      return newMeth;
  }
  
  /**
   * Given a method name (foo or something like foo$98), return the name of
   * the super method (foo$0 or foo$99).
   */
  private String getSuperMethodName( CtMethod<?> method ) {
	  String name = method.getSimpleName();
      int pos = name.lastIndexOf(MIXED_METH_SEP);
      if( pos == -1 ) {
          return name + MIXED_METH_SEP + "0";
      }
      String s = name.substring(pos+1);
      int methIndex = new Integer(s) + 1;
      return name.substring(0,pos) + MIXED_METH_SEP + methIndex;
  }
  
  /**
   * Check whether a method with a signature similar to the one of the current
   * method has already been defined by a mixed class.
   * 
   * @param meths    methods defined by previously mixed classes
   * @param current  the current position in meths
   * @param method   the current method
   * @return  the last mixed method with a name similar to the current one
   *          or <code>null</code> if no similar method exists
   */
  private CtMethod<?> getPreviouslyDefinedMethod(
          Set<CtMethod<?>>[] meths, int current, CtMethod<?> method ) {
      
      final String name = method.getSimpleName();
      final String s = name + MIXED_METH_SEP;
      
      for (int i = current+1 ; i < meths.length ; i++) {
          for (CtMethod<?> prevmeth : meths[i]) {
              
              // Check the method name
              if( ! prevmeth.getSimpleName().equals(name) &&
                  ! prevmeth.getSimpleName().startsWith(s)) {
                  continue;
              }
                  
              // Check the method parameter types
              List<CtParameter<?>> prevparams = prevmeth.getParameters();
              List<CtParameter<?>> params = method.getParameters();
              
              if( prevparams.size() != params.size() ) {
                  continue;
              }
              
              int j = 0;
              for (CtParameter<?> param : params) {
                  CtTypeReference<?> pType = param.getType();
                  CtTypeReference<?> prevType = prevparams.get(j).getType();
                  if( ! (pType.isSubtypeOf(prevType) &&
                         prevType.isSubtypeOf(pType)) ) {
                      break;
                  }
                  j++;
              }
              
              if( j != params.size() ) {
                  /*
                   * The previous loop has been interrupted by break.
                   * Method signatures differ.
                   */
                  continue;
              }
              
              return prevmeth;
          }
      }
      
      /*
       * No previously mixed method matching the current method signature was
       * found.
       */
      return null;
  }
  
  /**
   * Add a Javadoc comment to <code>target</code> to trace it back to the mixin
   * parts (mixed classes).
   * 
   * @param mixed   the mixin parts (mixed classes).
   * @param target  the target class where the parts are mixed.
   */
  private void setSource( CtClass<?>[] mixed, CtClass<?> target ) {
      
      String comment = target.getDocComment();
      if( comment == null ) comment="";
      
      for (CtClass<?> mix : mixed) {
          comment += " @see "+mix.getQualifiedName()+"\n";
      }
      
      target.setDocComment(comment);
  }
      
  /**
   * Add a Javadoc comment to <code>newMeth</code> to trace it back to its
   * source (method).
   * 
   * @param method   the source method
   * @param newMeth  the mixed method
   */
  private void setSource( CtMethod<?> method, CtMethod<?> newMeth ) {    
      String comment = newMeth.getDocComment();
      if( comment == null ) comment="";
      comment += SpoonHelper.toSeeLink(method);      
      newMeth.setDocComment(comment);        
  }
  
  /**
   * Add a Javadoc comment to <code>newField</code> to trace it back to its
   * source (field).
   * 
   * @param field     the source field
   * @param newField  the mixed field
   */
  private void setSource( CtField<?> field, CtField<?> newField ) {
      
      String comment = newField.getDocComment();
      if( comment == null ) comment="";
      
      String className = field.getDeclaringType().getQualifiedName();
      comment += " @see "+className+"#"+field.getSimpleName();
      
      newField.setDocComment(comment);        
  }
  
  /**
   * Replace all the type references from src to target in the given method. 
   */
  private void updateTypeRefs(
          CtMethod<?> method, CtClass<?> src, CtClass<?> target ) {
      
      List<CtTypeReference<?>> refs = 
          Query.getReferences(
              method,
              new ReferenceTypeFilter<CtTypeReference<?>>(CtTypeReference.class)
      );
      
      String srcName = src.getQualifiedName();
      String targetName = target.getSimpleName();
      CtPackageReference targetPackage =
          factory.Package().createReference(target.getPackage());
      
      for (CtTypeReference<?> ref : refs) {
          if( ref.getQualifiedName().equals(srcName) ) {
              ref.setSimpleName(targetName);
              ref.setPackage(targetPackage);
          }
      }
  }
  
  /**
   * Replace calls to methods with a _this_ prefix.
   * 
   * @param newMeth  the newly mixed method
   */
  private void updateCallsTo_this_Method( CtMethod<?> newMeth ) {
      
      List<CtInvocation<?>> invs =
          Query.getElements(
              newMeth,
              new AbstractFilter<CtInvocation<?>>(CtInvocation.class) {
                  public boolean matches(CtInvocation<?> inv) {
                      CtExecutableReference<?> cer = inv.getExecutable();
                      String invokedMethName = cer.getSimpleName();
                      return invokedMethName.startsWith(THIS);
                  }
              }
      );
      
      for (CtInvocation<?> inv : invs) {
          CtExecutableReference<?> cer = inv.getExecutable();
          String name = cer.getSimpleName().substring(THIS.length());
          cer.setSimpleName(name);
      }
  }

  /**
   * Replace calls to methods with a _super_ prefix.
   * 
   * @param newMeth  the newly mixed method
   * @param meths    the methods inserted by the currently mixed class
   */
  private void updateCallsTo_super_Method(
          CtMethod<?> newMeth, Set<CtMethod<?>> meths ) {
      
      List<CtInvocation<?>> invs =
          Query.getElements(
              newMeth,
              new AbstractFilter<CtInvocation<?>>(CtInvocation.class) {
                  public boolean matches(CtInvocation<?> inv) {
                      CtExecutableReference<?> cer = inv.getExecutable();
                      String invokedMethName = cer.getSimpleName();
                      return invokedMethName.startsWith(SUPER);
                  }
              }
      );

      /*
       * For each _super_... method invocation, search the corresponding
       * "base" method.
       */
      for (CtInvocation<?> inv : invs) {
          
          CtExecutableReference<?> cer = inv.getExecutable();
          List<CtTypeReference<?>> ctrs = cer.getParameterTypes();
          final String invMethName = cer.getSimpleName().substring(SUPER.length());
          final String s = invMethName + MIXED_METH_SEP;
          
          boolean found = false;
          for (CtMethod<?> meth : meths) {
              
              if( ! meth.getSimpleName().equals(invMethName) &&
                  ! meth.getSimpleName().startsWith(s) ) {
                  continue;
              }
              
              List<CtParameter<?>> params = meth.getParameters();
              if( params.size() != ctrs.size() ) {
                  continue;
              }
              
              int i = 0;
              for (CtParameter<?> param : params) {
                  CtTypeReference<?> ptype = param.getType();
                  CtTypeReference<?> ctr = ctrs.get(i);
                  if( ! (ctr.isSubtypeOf(ptype) && ptype.isSubtypeOf(ctr)) ) {
                      break;
                  }
                  i++;
              }
              
              if( i != params.size() ) {
                  /*
                   * The previous loop has been interrupted by break.
                   * Parameter types differ.
                   */
                  continue;
              }
              
              found = true;
              String superMethName = getSuperMethodName(meth);
              cer.setSimpleName(superMethName);
              break;
          }
          
          if(!found) {
              /*
               * The mixed method calls _super_ but does not define a
               * corresponding base method.
               */
              final String msg =
                  "A base method associated to the "+SUPER+
                  invMethName+" call should have been found";
              throw new RuntimeException(msg);
          }
      }
  }
}
