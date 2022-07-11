package org.meveo.service.script;

import java.util.HashMap;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;

/**
 * Main script interface that all script interfaces must inherit from.
 * 
 * @author Andrius Karpavicius
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.11
 */
public interface ScriptInterface {

	/**
	 * Batch processing - method to call at the beginning of script execution -
	 * before execute() is called.
	 * 
	 * @param methodContext Method variables in a form of a map
	 * @throws BusinessException business exception.
	 */
	void init(Map<String, Object> methodContext) throws BusinessException;

	/**
	 * Main script method. Can be called multiple times when used with init() and
	 * finalize() methods or just once if used without them for a single script
	 * execution.
	 * 
	 * @param methodContext Method variables in a form of a map where
	 *                      CONTEXT_ENTITY=entity to process
	 * @throws BusinessException business exception.
	 */
	void execute(Map<String, Object> methodContext) throws BusinessException;

	/**
	 * Batch processing - method to call at the end of script execution - after
	 * execute() is called.
	 * 
	 * @param methodContext Method variables in a form of a map
	 * @throws BusinessException business exception.
	 */
	void finalize(Map<String, Object> methodContext) throws BusinessException;

	/**
	 * Immediatly stop the execution and return current results
	 */
	default Map<String, Object> cancel() {
		return new HashMap<>();
	}
	
	/** 
	 * Usage in a pool - Method called before the object is borrowed from pool.
	 * Reset the inputs / state of a script to default
	 */
	default void resetState() {
		
	}

	/**
	 * Executed after the main {@link #execute(Map)} method.
	 * 
	 * @param methodContext Method variables in a form of a map where
	 *                      CONTEXT_ENTITY=entity to process
	 * @throws BusinessException when post transaction fails
	 */
	default public void postCommit(Map<String, Object> methodContext) throws BusinessException {

	}

	/**
	 * Executed when an error is thrown in the {@link #execute(Map)} method.
	 * 
	 * @param methodContext Method variables in a form of a map where
	 *                      CONTEXT_ENTITY=entity to process
	 * @throws BusinessException when rollback fails
	 */
	default public void postRollback(Map<String, Object> methodContext) throws BusinessException {

	}
}