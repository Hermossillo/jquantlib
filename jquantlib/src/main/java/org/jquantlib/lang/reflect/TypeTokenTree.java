package org.jquantlib.lang.reflect;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * @author Richard Gomes 
 */
//TODO: add comments and explain what this class is about
public class TypeTokenTree {

    private final TypeNode root;
    
    public TypeTokenTree() {
        this.root = retrieve(getClass());
    }
    
    public TypeTokenTree(final Class<?> klass) {
        this.root = retrieve(klass);
    }

    public TypeNode getRoot() {
        return root;
    }
    
    private TypeNode retrieve(final Class<?> klass) {
        final Type superclass = klass.getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new IllegalArgumentException(ReflectConstants.SHOULD_BE_ANONYMOUS_OR_EXTENDED);
        }
        
        final TypeNode node = new TypeNode(klass);
        for (Type t : ((ParameterizedType) superclass).getActualTypeArguments() ) {
            node.add(retrieve(t));
        }
        return node;
    }
    
    private TypeNode retrieve(final Type type) {
        final TypeNode node;
        if (type instanceof Class<?>) {
            node = new TypeNode((Class<?>)type);
        } else if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            node = retrieve(rawType);
            for (Type arg : ((ParameterizedType) type).getActualTypeArguments()) {
                node.add(retrieve(arg));
            }
//        } else if (type instanceof TypeVariable) {
//            GenericDeclaration declaration = ((TypeVariable) type).getGenericDeclaration();
//            node = new TypeNode(declaration);
//            for (Type arg : ((TypeVariable) type).getBounds()) {
//                node.add(retrieve(arg));
//            }
        } else {
            throw new IllegalArgumentException(ReflectConstants.ILLEGAL_TYPE_PARAMETER);
        }
        return node;
    }
    
}