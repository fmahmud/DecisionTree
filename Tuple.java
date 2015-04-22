package myclasses;

import java.util.*;

/**
 * Contains pairs of attributes and the values associated to them. Each Datum is
 * a line in a table, a tuple, or a creature to be assessed as being an orc or a
 * hobbit.
 * 
 * @author Faizaan Mahmud
 */
public class Tuple {
	private int label;
	private HashMap<Integer, Integer> pairs;

	private static final boolean USE_NUM_VALUE = true;

	/**
	 * Line l has to be of format: label index1:value1 index2:value2...
	 * 
	 * @param l
	 *            Line to parse which is formatted as above.
	 */
	public void parseLine(String l) {
		String[] parts = l.split(" ");
		pairs = new HashMap<Integer, Integer>();
		for (int i = 1; i < parts.length; i++) {
			String[] temp = parts[i].split(":");
			Integer t = new Integer(Integer.parseInt(temp[0]));
			if (t == 0)
				continue; // omit attributes of type 0. 0:8 is bad.
			this.pairs.put(t, new Integer(Integer.parseInt(temp[1])));
		}
		this.label = parts[0].contains("+") ? 1 : -1;
	}

	/**
	 * @return number of pairs in this Datum
	 */
	public int length() {
		return this.pairs.size();
	}

	/**
	 * @return label associated to this Datum
	 */
	public int getLabel() {
		return this.label;
	}

	/**
	 * Check if the pair contains
	 * 
	 * @param attr
	 *            The attribute to see if it is contained in this datum.
	 * @return true if is contained, else false.
	 */
	public boolean containsKey(Integer attr) {
		return pairs.containsKey(attr);
	}

	/**
	 * Attributes are integer valued and each datum does not contain values for
	 * every attribute. This function gets the keyset as a vector, gets the ith
	 * attribute number and returns it.
	 * 
	 * @param i
	 */
	private Integer getIthAttrNumber(int i) {
		return new Vector<Integer>(pairs.keySet()).get(i);
	}

	/**
	 * Retrieves the value corresponding to the key passed.
	 * 
	 * @param key
	 *            the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or null if this
	 *         map contains no mapping for the key
	 */
	public Integer getValueWithKey(Integer key) {
		return pairs.get(key);
	}

	/**
	 * Returns an Integer[] containing { attr, value } for the ith pair.
	 * 
	 * @param i
	 *            Index.
	 * @return Integer[] pair. Ith pair in HashMap.
	 */
	public Integer[] getPairAt(int i) {
		if (i < 0)
			return null;
		if (i < pairs.size()) {
			// this is the ith attribute number
			int temp = new Vector<Integer>(pairs.keySet()).get(i);
			return new Integer[] { temp, new Integer(pairs.get(temp)) };
		}
		return null;
	}

	/**
	 * Returns true if the value of the attribute specified is equal to the
	 * passed value. Note if attribute is not contained in this tuple then false
	 * is returned.
	 * 
	 * @param attr
	 *            attribute number to check
	 * @param value
	 *            value to compare to
	 * @return true if they are equal else false.
	 */
	public int checkAttrValue(int attr, int value) {
		Integer t = pairs.get(attr);
		if (t == null)
			return 0;
		return t == value?1:-1;
	}

	public boolean removePair(Integer key) {
		return pairs.remove(key) != null;
	}

	/**
	 * Removes the ith pair in this object if it exists
	 * 
	 * @param i
	 *            Index.
	 * @return returns true if pair was removed successfully else false.
	 */
	public boolean removeIthPair(int i) {
		if (i > pairs.size())
			return false;
		pairs.remove(getIthAttrNumber(i));
		return true;
	}

	/**
	 * DOES NOT OVERWRITE! Pushes a pair into the vector.
	 * 
	 * @param attr
	 *            Attribute of the pair
	 * @param value
	 *            Value of attribute
	 * @return returns true if pair was successfully pushed else false.
	 */
	public boolean pushPair(Integer attr, Integer value) {
		if (pairs.containsKey(attr))
			return false;
		pairs.put(attr, value);
		return true;
	}

	/**
	 * Overwrites the value for a pair. If it doesn't exist pushes pair.
	 * 
	 * @param attr
	 *            Attribute to over write
	 * @param value
	 *            new value of the pair
	 * @return true if pair existed, false other wise
	 */
	public boolean overwriteValue(Integer attr, Integer value) {
		return pairs.put(attr, value) != null;
	}

	public int getNumericalValue() {
		Vector<Integer> keys = new Vector<Integer>(pairs.keySet());
		int number = 0;
		for (int i = 0; i < keys.size(); i++) {
			number += i * i * (keys.get(i) + pairs.get(keys.get(i)));
		}
		return number*label;
	}

	@Override
	public String toString() {
		if (USE_NUM_VALUE)
			return new StringBuilder().append(this.getNumericalValue())
					.toString();
		Vector<Integer> keys = new Vector<Integer>(pairs.keySet());
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(this.label + ", ");
		for (int i = 0; i < keys.size(); i++) {
			sb.append(keys.get(i));
			sb.append(":");
			sb.append(pairs.get(keys.get(i)));
			if (i < keys.size() - 1)
				sb.append(", ");
		}
		return sb.append("]\n").toString();
	}

	/**
	 * Constructs a Datum object based on the String passed
	 * 
	 * @param l
	 *            formatted line representing object
	 */
	public Tuple(String l) {
		parseLine(l);
	}

	public Tuple(HashMap<Integer, Integer> newHashMap, int _label) {
		this.label = _label;
		this.pairs = newHashMap;
	}

	public Tuple(Tuple other) {
		this.label = other.label;
		this.pairs = new HashMap<Integer, Integer>(other.pairs);
	}
}
