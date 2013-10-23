package com.github.uberroot.ncjbot;

/**
 * <p>Represents an object in which direct access to untrusted code could have undesirable consequences, and to which a safe
 * alternative or wrapper exists.</p>
 * 
 * @author Carter Waxman
 * 
 * @param <S> The safe object type.
 */
public interface UnsafeObject<S> {
	
	/**
	 * <p>Gets a safe analog of this UnsafeObject.</p>
	 * 
	 * @return a safe analog of this UnsafeObject.
	 */
	public S getSafeObject();
}
