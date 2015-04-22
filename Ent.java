package myclasses;

import java.util.*;
import java.io.*;


/**
 * A Decision Tree class that is trained over a set of data and operates on a
 * finite number of attributes that describes the data. Equivalent to an Ent in
 * an Entmoot. - Had to change name due to assignment format. 
 * 
 * @author Faizaan
 * 
 */
public class Ent {

	private static final int DEBUG = -1;
	private Vector<Tuple> trainingData;
	private Node root;
	private Vector<Integer> attributeList;
	private HashMap<Integer, HashSet<Integer>> valuesOfEachAttribute;
	private float F = 1.0f;

	private void print(String s, int level) {
		if(DEBUG < level) return;
		StringBuffer sb = new StringBuffer();
		sb.append("\t");
		for(int i = 0; i < level; i++) {
			sb.append("\t");
		}
		sb.append("[DecisionTree]"+s);
		System.out.print(sb.toString());
	}
	
	private void println(String s, int level) {
		print(s+"\n", level);
	}

	public Ent(Vector<Tuple> _trainingData, Vector<Integer> _attributes, float _F) {
		this.F = _F;
		attributeList = new Vector<Integer>(_attributes);
		valuesOfEachAttribute = new HashMap<Integer, HashSet<Integer>>();
		trainingData = _trainingData; // pointer not deep copy.
		println("[constructor]: Training Set Size = " + trainingData.size(), 0);
		println("[constructor]: Enumerating all values for all attributes...", 1);
		enumerateAllValuesForAttrs();
		println("[constructor]: Done enumerating", 1);
		println("[constructor]: Starting Training", 1);
		root = this.train(trainingData, attributeList);
		println("[constructor]: Done Training!", 0);
	}

	/**
	 * Fills the HashMap valuesOfEachAttribute. Each attribute is mapped to a
	 * HashSet of Integers that contains all possible values that attribute can
	 * take.
	 */
	private void enumerateAllValuesForAttrs() {
		Tuple d = null;
		Integer[] pair = null;
		for (int i = 0; i < trainingData.size(); i++) {
			d = trainingData.get(i);
			for (int j = 0; j < d.length(); j++) {
				pair = d.getPairAt(j);
				HashSet<Integer> v = null;
				if (this.attributeList.contains(pair[0])) {
					if (!valuesOfEachAttribute.containsKey(pair[0])) {
						v = new HashSet<Integer>();
						v.add(pair[1]);
						valuesOfEachAttribute.put(pair[0], v);
					} else {
						v = valuesOfEachAttribute.get(pair[0]);
						v.add(pair[1]);
					}
				}
			}
		}
	}

	/**
	 * returns true if all elements in the training Data are of the same class
	 * 
	 * @return boolean
	 */
	private int allInSameClass(Vector<Tuple> data) {
		println("[allInSameClass]: Checking if data: "+data.toString()+" is all one label", 4);
		if(data.size() == 0) {
			println("[allInSameClass]: No data passed", 4);
			return 0;
		}
		Tuple d = data.get(0);
		int l = d.getLabel();
		for (int i = 0; i < data.size(); i++) {
			d = data.get(i);
			if (l != d.getLabel()) {
				println("[allInSameClass]: Not all in same class", 4);
				return 0;
			}				
		}
		println("[allInSameClass]: Thought class = "+l, 4);
		return l;
	}

	/**
	 * Good for only 2-Class classification. Calculates the majority in the
	 * provided data set.
	 * 
	 * @param d
	 *            data set.
	 * @return the label that is in majority in the data set d.
	 */
	private int findMajority(Vector<Tuple> d) {
		int majority = 0;
		for (int i = 0; i < d.size(); i++) {
			majority += d.get(i).getLabel();
		}
		return majority >= 0 ? 1 : -1;
	}

	/**
	 * Returns the log base two of the float variable passed
	 * 
	 * @param a 
	 * 			 The value of which to find the log of
	 * @return
	 */
	private float log2(float a) {
		if (a == 0)
			return 0;
		return (float) (Math.log(a) / Math.log(2.0));
	}

	/**
	 * Compute Expected Information Needed to classify a tuple in D
	 * 
	 * @param D
	 *            all the training data at this level of the tree
	 * @return float valued Info(D)
	 */
	private float computeEIN(Vector<Tuple> D) {
		println("[computeEIN]: Size of D: " + D.size(), 2);
		float sizeOfD = (float) D.size();
		float num_pos = 0.0f;
		float num_neg = 0.0f;
		for (int i = 0; i < sizeOfD; i++) {
			if (D.get(i).getLabel() == 1) {
				num_pos++;
			} else {
				num_neg++;
			}
		}
		float toRet = (-1) * (num_pos / sizeOfD) * log2(num_pos / sizeOfD);
		toRet += (-1) * (num_neg / sizeOfD) * log2(num_neg / sizeOfD);
		println("[computeEIN]: EIN = " + toRet, 2);
		return toRet;
	}

	/**
	 * Counts the number of occurrences that an attribute takes a specific
	 * value. Returns an array which contains the number of times the value is
	 * labeled positively and negatively.
	 * 
	 * @param attr
	 *            the attribute in question
	 * @param value
	 *            the value this attribute can take
	 * @param D
	 *            training examples
	 * @return int[] {num_negative, num_positive}
	 */
	private int[] countNumOccurrences(int attr, int value, Vector<Tuple> D) {
		println("[CNO]: Counting when attr:" + attr + " = " + value, 4);
		int[] toRet = new int[] { 0, 0 };
		Tuple d = null;
		for (int i = 0; i < D.size(); i++) {
			d = D.get(i);
			try {
				Integer v = d.getValueWithKey(new Integer(attr));
				if (v == value) {
					toRet[d.getLabel() == 1 ? 1 : 0]++;
				}
			} catch (NullPointerException npe) {
				println("[CNO]: tuple(" + i + ") did not have an attribute", 3);
				continue;
			}
		}
		println("[CNO]: { " + toRet[0] + ", " + toRet[1] + " }", 4);
		return toRet;
	}

	/**
	 * Compute Expected Information Requirement for attribute i
	 * 
	 * @param i
	 *            is the index of the attribute to compute for.
	 * @param D
	 *            Data to compute EIR on.
	 * @return EIR as a float
	 */
	private float computeEIR(int i, Vector<Tuple> D) {
		println("[computeEIR]: Size of D: " + D.size() + " attribute: " + i, 2);
		HashSet<Integer> vals = valuesOfEachAttribute.get(i);
		if(vals == null) //this means that the attribute is not present in any tuple!
			return 100000000.0f;
		Vector<Integer> v = new Vector<Integer>(vals);
		int[] temp = null;
		float toRet = 0.0f;
		int sizeOfV = v.size();
		println("[computeEIR]: Number of outcomes for attr(" + i + ") : "
				+ sizeOfV, 2);
		for (int j = 0; j < sizeOfV; j++) {
			temp = countNumOccurrences(i, v.get(j), D);
			float outerWeight = (((float) temp[0] + (float) temp[1]) / D.size());
			float weight1 = ((float) temp[0] / (temp[0] + temp[1]));
			float weight2 = ((float) temp[1] / (temp[0] + temp[1]));
			if (temp[0] == 0 && temp[1] == 0) {
				println("[computeEIR]: attribute("+i+") = "+v.get(j)+" had no tuples", 3);
				weight1 = 1.0f;
				weight2 = 1.0f;
			//	outerWeight = 10000000.0f;
			}
			println("[computeEIR]: outerWeight = " + outerWeight
					+ " weight1 = " + weight1 + " weight2 = " + weight2, 2);
			toRet += (outerWeight)
					* ( ( (-1) * weight1 * log2(weight1) ) + ( (-1) * weight2 * log2(weight2) ) );
		}
		println("[computeEIR]: Computed...EIR(" + i + ") = " + toRet, 2);
		return toRet;
	}

	/**
	 * Returns the attribute (not index) that results in the most pure subsets
	 * 
	 * @param D
	 *            the training set
	 * @param attributeList
	 *            list of all attributes
	 * @return returns the attribute
	 */
	private Integer findBestSplittingCriterion(Vector<Tuple> D,
			Vector<Integer> _attributeList) {
		println("[FBSC]: Size of D: " + D.size() + " num Attributes: "
				+ _attributeList.size(), 3);
		float largestGain = 0.0f;
		Integer attribute = 0;
		float tempGain = 0.0f;
		float ein = computeEIN(D); //Info(D)
		for (int i = 0; i < _attributeList.size(); i++) {
			tempGain = computeEIR(_attributeList.get(i), D); //Info_attr(D)
			tempGain = ein - tempGain;
			if (tempGain >= largestGain) {
				largestGain = tempGain;
				attribute = _attributeList.get(i);
			}
		}
		println("[FBSC]: largest gain = " + largestGain+" for attribute = "+attribute, 3);
		return attribute;
	}
	
	/**
	 * Randomly selects a unique subset of the vector passed
	 * that is half the size of the vector passed
	 * @param d
	 * @return
	 */
	private Vector<Integer> randomlySelectSubset(Vector<Integer> d) {
		println("[RSS]: Set Size = "+d.size()+" Subset Size = "+d.size()*this.F, 3);
		if(d.size()<5) return d;
		Vector<Integer> toRet = new Vector<Integer>();
		float subsetSizeRatio = this.F;
		while(toRet.size() < (int)(subsetSizeRatio*d.size())) {
			Integer t = d.get((int)(Math.random()*d.size()));
			if(toRet.contains(t)) continue;
			toRet.add(t);
		}
		println("[RSS]: Random Subset = "+toRet.toString(), 4);
		println("[RSS]: Random subset selected", 3);
		return toRet;
	}

	/**
	 * Pass in the training data set and the set of attributes and this
	 * recursive algorithm trains and generates a Decision Tree and returns a
	 * pointer to the root node.
	 * 
	 * @param d
	 * @param attributes
	 * @return
	 * @throws IOException
	 */
	private Node train(Vector<Tuple> d, Vector<Integer> attributes) {
		println("[train]: Size of D: " + d.size() + " Num attributes: "
				+ attributes.size(), 1);
		Node n = new Node();
		println("[train]: Checking if all in same class...", 2);
		int _c = allInSameClass(d);
		// check if all the tuples that satisfy the previous outcome are all the
		// same class
		if (_c != 0) {
			n.childValuePairs = null;
			n.label = _c;
			println("[train]: All in same class: " + _c, 2);
			return n;
		}
		// check if there are no more attributes left to use
		if (attributes.size() == 0) {
			n.label = findMajority(d);
			n.childValuePairs = null;
			println("[train]: Num Attributes == 0 Node label = "+n.label, 2);
			return n;
		}
		println("[train]: Num Attributes = "+attributes.size(), 2);
		// find best splitting criterion
		println("[train]: Finding Best Splitting Criterion...", 2);
		Integer splittingCriterion = findBestSplittingCriterion(d, randomlySelectSubset(attributes));
		n.ownAttr = splittingCriterion;
		println("[train]: Splitting on attr=" + splittingCriterion, 2);
		// remove that attribute from attribute list
		attributes.removeElement(splittingCriterion);
		println("[train]: After removing attribute, num attributes = "+attributes.size(), 2);
		// get the list of all outcomes particular attribute can have
		HashSet<Integer> outcomes = valuesOfEachAttribute.get(splittingCriterion);
		if(outcomes == null) {
			//this attribute has no outcomes apparently?
			n.label = findMajority(d);
			n.childValuePairs = null;
			println("[train]: Num outcomes = 0", 2);
			return n;
		}
		Vector<Integer> vals = new Vector<Integer>(outcomes);
		println("[train]: " + vals.size() + " outcome(s) for attr = "
				+ splittingCriterion+" "+vals.toString(), 2);
		// setting the node's own splitting attribute to the one chosen.
		// iterate over all these outcomes
		Vector<Tuple> d_v = new Vector<Tuple>(); //vector to hold void tuples
		for (int i = 0; i < vals.size(); i++) {
			Vector<Tuple> d_i = new Vector<Tuple>();
			// iterate over all data in the data set
			for (int j = 0; j < d.size(); j++) {
				/*
				 * make a new set containing all data that contains the selected
				 * attribute and the value of that attribute is the i-th value
				 * that this attribute can take.
				 */
				int attrCheck = d.get(j).checkAttrValue(splittingCriterion, vals.get(i));
				if (attrCheck == 1) {
					d_i.add(d.get(j));
				} else if(attrCheck == 0) {
					//doesn't exist case!
					d_v.add(d.get(j));
				}
			}
			// if there is nothing in this new set label the new node with the
			// majority
			if (d_i.size() == 0) {
				Node temp = new Node();
				temp.label = findMajority(d_i);
				n.childValuePairs.put(vals.get(i), temp);
			} else { // else recurse!
				n.childValuePairs.put(vals.get(i), train(d_i, new Vector<Integer>(attributes)));
				// RECURSION FAIRY does the rest.
			}
		}
		n.childValuePairs.put(-1, train(d_v, new Vector<Integer>(attributes)));
		return n;
	}

	/**
	 * Asks the Tree/Ent what it thinks about the tuple passed.
	 * 
	 * @param d
	 *            tuple in question
	 * @return -1 if Orc or +1 if Hobbit
	 */
	public int testTuple(Tuple d) {
		return this.root.predict(d);
	}

	/**
	 * Array in format: { true positive, false positive, true negative, false
	 * negative, skipped }
	 * 
	 * @param testSet
	 *            is the set of data to test the decision tree on.
	 * @return integer array containing 5 values as above.
	 */
	public int[] test(Vector<Tuple> testSet) {
		int[] results = new int[5];
		int result = 0;
		Tuple d = null;
		for (int i = 0; i < testSet.size(); i++) {
			d = testSet.get(i);
			result = testTuple(d);
			if (result == 0) {
				results[4]++; // skipped
			} else if (result == 1) {
				if (d.getLabel() == 1)
					results[0]++; // true positive
				else
					results[1]++; // false positive
			} else {
				if (d.getLabel() == -1)
					results[2]++; // true negative
				else
					results[3]++; // false negative
			}
		}
		return results;
	}

	/**
	 * Represents a node in a decision tree. Contains which attribute this node
	 * splits on and the value each child represents of that attribute if there
	 * is a child
	 * 
	 * @author Faizaan
	 * 
	 */
	class Node {
		public HashMap<Integer, Node> childValuePairs;
		public int label;
		public Integer ownAttr;

		/**
		 * Empty constructor to initialize node object with empty HashMap and
		 * null own attribute
		 */
		public Node() {
			childValuePairs = new HashMap<Integer, Node>();
			ownAttr = null;
		}

		/**
		 * Get the number of children this node has.
		 * 
		 * @return returns number of children this node has
		 */
		public int numChildren() {
			try {
				return childValuePairs.size();
			} catch (NullPointerException npe) {
				return 0;
			}
		}

		/**
		 * Function to deal with the case where the attribute this node splits
		 * on is not contained within the tuple passed. Currently set to RANDOM!
		 * 
		 * @param d
		 *            tuple to deal with
		 * @return -1 or +1
		 */
		private int dealWithVoidCase(Tuple d) {
			println("[VoidCase]: Dealing with void case for: "+d.getNumericalValue(),3);
			Node childToAsk = this.childValuePairs.get(-1);
			if(childToAsk != null) {
				return childToAsk.predict(d);
			} else {
				return Math.random()>0.5d?1:-1;
			}
		}

		/**
		 * Asks children of the node if there are any, what they think about the
		 * tuple passed. At each step of recursion, the attribute that is
		 * associated with that node is removed from the tuple so finally when
		 * there are no more pairs in the tuple or a leaf of a tree is reached,
		 * a prediction on which class the tuple belongs to is made
		 * 
		 * @param d
		 *            Tuple upon which the classification prediction is being
		 *            made
		 * @return which class the tuple is thought to belong to
		 */
		public int predict(Tuple d) {
			println("[predict] Node("+ownAttr+","+label+", "+(childValuePairs==null?"leaf":"Num children="+childValuePairs.size())+") Being asked "+d.toString(),3);
			// if this is a leaf return leaf's label
			if (childValuePairs == null)
				return this.label;
			Integer value = null;
			// if this object has a value for this node's attribute
			if ((value = d.getValueWithKey(this.ownAttr)) != null) {
				Tuple dsub = new Tuple(d);
				dsub.removePair(this.ownAttr);
				Node childToAsk = this.childValuePairs.get(value);
				if(childToAsk == null) {
					return dealWithVoidCase(d);
				}
				return childToAsk.predict(dsub);
			} else { // tuple doesn't have value for node's attribute
				// majority vote?
				// currently ignoring.
				return dealWithVoidCase(d);
			}
		}
	}

}
