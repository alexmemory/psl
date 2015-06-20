/*
 * This file is part of the PSL software.
 * Copyright 2011-2013 University of Maryland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.umd.cs.psl.model.predicate;

import java.util.List;

import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.Term;
import edu.umd.cs.psl.model.atom.Atom;

/**
 * A relation that can be applied to {@link Term Terms} to form {@link Atom
 * Atoms}.
 * <p>
 * Predicates must be constructed using the {@link PredicateFactory}.
 * <p>
 * A Predicate is uniquely identified by its name.
 * 
 * @author Matthias Broecheler
 */
abstract public class Predicate {

	private final String predicateName;

	private final ArgumentType[] types;
	private final List<String> names;

	/**
	 * Constructor.
	 * <p>
	 * Should only be called by {@link PredicateFactory}.
	 * 
	 * @param name
	 *            name for this predicate
	 * @param types
	 *            types for each of the predicate's arguments
	 * @param names
	 *            names for each of the predicate's arguments
	 */
	Predicate(String name, ArgumentType[] types) {
		this.types = types;
		this.names = null;
		predicateName = name.toUpperCase();
	}
	
	/**
	 * Constructor.
	 * <p>
	 * Should only be called by {@link PredicateFactory}.
	 * 
	 * @param name
	 *            name for this predicate
	 * @param types
	 *            types for each of the predicate's arguments
	 * @param names
	 *            names for each of the predicate's arguments
	 */
	Predicate(String name, ArgumentType[] types, List<String> names) {
		this.types = types;
		this.names = names;
		predicateName = name.toUpperCase();
	}

	/**
	 * Returns the name of this Predicate.
	 * 
	 * @return a string identifier for this Predicate
	 */
	public String getName() {
		return predicateName;
	}

	/**
	 * Returns the number of {@link Term Terms} that are related when using this
	 * Predicate.
	 * <p>
	 * In other words, the arity of a Predicate is the number of arguments it
	 * accepts. For example, the Predicate Related(A,B) has an arity of 2.
	 * 
	 * @return the arity of this Predicate
	 */
	public int getArity() {
		return types.length;
	}

	/**
	 * Returns the name associated with a particular argument position of this
	 * Predicate.
	 * 
	 * @param position
	 *            the argument position
	 * @return the name of the argument for the given position
	 */
	public String getArgumentName(int position) {
		return names.get(position);
	}
	
	/**
	 * Returns the names associated with arguments of this Predicate.
	 * 
	 * @return the names of the arguments
	 */
	public List<String> getArgumentNames() {
		return names;
	}
	
	/**
	 * Returns the ArgumentType which a {@link Term} must have to be a valid
	 * argument for a particular argument position of this Predicate.
	 * 
	 * @param position
	 *            the argument position
	 * @return the type of argument accepted for the given position
	 */
	public ArgumentType getArgumentType(int position) {
		return types[position];
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(getName()).append("(");
		if (names != null) {
			for (int i = 0; i < names.size(); i++) {
				if (i > 0)
					s.append(", ");
				s.append(names.get(i));
			}
		} else {
			for (int i = 0; i < types.length; i++) {
				if (i > 0)
					s.append(", ");
				s.append(types[i]);
			}
		}
		return s.append(")").toString();
	}

	@Override
	public int hashCode() {
		/*
		 * The PredicateFactory ensures that names and signatures uniquely
		 * identify Predicates. Hence, equality of Predicates can be determined
		 * by identity;
		 */
		return super.hashCode();
	}

	@Override
	public boolean equals(Object oth) {
		/*
		 * The PredicateFactory ensures that names and signatures uniquely
		 * identify Predicates. Hence, equality of Predicates can be determined
		 * by identity;
		 */
		return oth == this;
	}

}
