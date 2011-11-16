/*
 * Copyright (C) 2010 Preston Lacey http://javaflacencoder.sourceforge.net/ All Rights Reserved. This library is free software;
 * you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option) any later version. This library is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the
 * GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sourceforce.javaflacencoder;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * The purpose of this class is to provide a source for reusable int arrays. When using large numbers of arrays in succession,
 * it is inefficient to constantly go in an allocate/free loop. This way, we may pass a single, thread-safe recycler to all
 * objects. No matter where the arrays end their life, we can then add it to the same resource store.
 * 
 * @author Preston Lacey
 */
public class ArrayRecycler
{
	LinkedBlockingQueue<int[]> usedIntArrays = null;

	ArrayRecycler()
	{
		usedIntArrays = new LinkedBlockingQueue<int[]>();
	}

	public void add(final int[] array)
	{
		usedIntArrays.add(array);
	}

	/**
	 * @param size
	 * @return
	 */
	public int[] getArray(final int size)
	{
		int[] result = usedIntArrays.poll();
		if (result == null) {
			result = new int[size];
			// System.err.println("Created new int array from null");
		}
		else if (result.length < size) {
			usedIntArrays.offer(result);
			result = new int[size];
			// System.err.println("created new int array from bad size");
		}
		return result;
	}
}
