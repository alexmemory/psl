/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2015 The Regents of the University of California
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
package org.linqs.psl.reasoner.term;

import java.util.ArrayList;
import java.util.List;

public class MemoryTermStore<E extends Term> implements TermStore<E> {
	private static final int DEFAULT_INITIAL_SIZE = 1000;

	private List<E> store;

	public MemoryTermStore() {
		this(DEFAULT_INITIAL_SIZE);
	}

	public MemoryTermStore(int initialSize) {
		store = new ArrayList<E>(initialSize);
	}

	public void add(E term) {
		store.add(term);
	}

	public void close() {
		store.clear();
		store = null;
	}

	public E get(int index) {
		return store.get(index);
	}

	public int size() {
		return store.size();
	}
}