package myclasses;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Creates a RandomForest based on the training data and three numbers and then
 * tests the forest on the testing data. Had to change name because of
 * assignment format... EntMoot is good right?
 * 
 * @see EntMoot
 * 
 * @author Faizaan Mahmud
 * @version 1.0
 */
public class EntMoot {

	// two vectors containing the data to train and then test on.
	private Vector<Tuple> trainingData, testingData;
	private Vector<Integer> allAttributes;
	private Vector<Ent> forest;
	private float decimalF = 0.0f;
	private int F = 0; // number of attributes for each tree to operate on
	private int K = 0; // number of trees
	private int D = 0; // number of tuples for each tree to be trained on
	private static final int DEBUG = -1;

	// 0 = initialization info - print once at execution
	// 1 = functional trace - print at every function call from constructor
	// 2 = subfunctional trace - every function call from functions
	// 3 = results and calculations - every calculation result
	// 4 = step by step - everything...

	/**
	 * Prints the string provided if the DEBUG level is high enough
	 * 
	 * @param s
	 *            - the String to print
	 * @param level
	 *            the depth of debugging this print command associates to
	 */
	private void print(String s, int level) {
		if (DEBUG < level)
			return;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < level; i++) {
			sb.append("\t");
		}
		sb.append("[RandomForest]" + s);
		System.out.print(sb.toString());
	}

	/**
	 * Prints the string followed by a new line if the DEBUG level is high
	 * enough
	 * 
	 * @param s
	 *            - the String to print
	 * @param level
	 *            the depth of debugging this print comma
	 */
	private void println(String s, int level) {
		print(s + "\n", level);
	}

	/**
	 * Creates a Datum object and adds it to trainingData vector
	 * 
	 * @param s
	 *            Line to create a new datum object from.
	 */
	private void processTrainingLine(String s) {
		if (s == null)
			return;
		getTrainingData().add(new Tuple(s));
	}

	/**
	 * Creates a Datum object and adds it to testingData vector
	 * 
	 * @param s
	 *            Line to create a new datum object from.
	 */
	private void processTestingLine(String s) {
		if (s == null)
			return;
		testingData.add(new Tuple(s));
	}

	/**
	 * Parses the data in the test set into trainingData Vector
	 * 
	 * @return the number of training examples
	 * @throws IOException
	 */
	private int parseTrainingSet(String trainingSet) throws IOException {
		FileReader fr = new FileReader(trainingSet);
		BufferedReader br = new BufferedReader(fr);
		String str = null;
		int i = 0;
		while ((str = br.readLine()) != null) {
			processTrainingLine(str);
			i++;
		}
		fr.close();
		br.close();
		return i;
	}

	/**
	 * Parses the data in the test set into testingData Vector
	 * 
	 * @return the number of test cases
	 * @throws IOException
	 */
	private int parseTestSet(String testFile) throws IOException {
		FileReader fr = new FileReader(testFile);
		BufferedReader br = new BufferedReader(fr);
		String str = null;
		int i = 0;
		while ((str = br.readLine()) != null) {
			processTestingLine(str);
			i++;
		}
		fr.close();
		br.close();
		return i;
	}

	/**
	 * Enumerates all attributes present in Data set passed.
	 * 
	 * @param d
	 *            Vector of Datum objects.
	 * @return Vector of Integers representing attributes.
	 */
	private Vector<Integer> enumerateAttributeList(Vector<Tuple> d) {
		Vector<Integer> attributes = new Vector<Integer>();
		for (int i = 0; i < d.size(); i++) {
			Tuple datum = d.get(i);
			for (int j = 0; j < datum.length(); j++) {
				Integer[] pair = datum.getPairAt(j);
				if (attributes.contains(pair[0]))
					continue;
				attributes.add(pair[0]);
			}
		}
		return attributes;
	}

	/**
	 * Creates a new Vector of Integers and returns the unique Union of the two
	 * sets. There are no duplicates.
	 * 
	 * @param a
	 *            First Vector of Integers
	 * @param b
	 *            Second Vector of Integers
	 * @return Duplicate Free Union of two Vectors passed.
	 */
	private Vector<Integer> uniqueUnion(Vector<Integer> a, Vector<Integer> b) {
		Vector<Integer> toRet = new Vector<Integer>(a);
		for (int i = 0; i < b.size(); i++) {
			if (toRet.contains(b.get(i)))
				continue;
			toRet.add(b.get(i));
		}
		return toRet;
	}

	/**
	 * Randomly chooses F attributes from the attribute list and returns it in
	 * the form of a Vector of Integers.
	 * 
	 * @param attributes
	 *            - the Vector of attributes to choose from
	 * @return Vector containing randomly chosen and unique attributes.
	 */
	private Vector<Integer> randomlyChooseFAttributes(Vector<Integer> attributes) {
		println("[RCFA]: Randomly Choosing F=" + this.F + " attributes", 3);
		Vector<Integer> toRet = new Vector<Integer>();
		Vector<Integer> indexes = new Vector<Integer>();
		for (int i = 0; i < attributes.size(); i++) {
			indexes.add(i);
		}
		for (int i = 0; i < this.F; i++) {
			toRet.add(attributes.get(indexes.remove((int) (Math.random() * indexes
					.size()))));

		}
		println("[RCFA]: Attributes Chosen: " + toRet.toString(), 3);
		return toRet;
	}

	/**
	 * Randomly chooses D tuples from the data set that are not unique Puts
	 * these tuples in a Vector and returns it.
	 * 
	 * @param data
	 *            set of data to choose tuples from
	 * @return a set of tuples that is a subset of the data set provided and may
	 *         contain duplicates.
	 */
	private Vector<Tuple> randomlyChooseDTuplesWithOutReplacement(
			Vector<Tuple> data) {
		println("[RCDTWOP]: Randomly Choosing D = " + this.D
				+ " tuples WITHOUT REPLACEMENT", 3);
		Vector<Tuple> toRet = new Vector<Tuple>();
		int[] types_chosen = new int[] { 0, 0 };
		while (toRet.size() < this.D) {
			Tuple t = data.get((int) (Math.random() * data.size()));
			if (toRet.contains(t))
				continue;
			if (types_chosen[0] > types_chosen[1] * 2) {
				if (t.getLabel() != 1)
					continue;
			} else if (types_chosen[1] > types_chosen[0] * 2) {
				if (t.getLabel() != -1)
					continue;
			}
			types_chosen[t.getLabel() == 1 ? 1 : 0]++;
			toRet.add(t);
		}
		println("[RCDTWOP]: Tuples Chosen: \n" + toRet.toString(), 4);
		return toRet;
	}

	/**
	 * Randomly chooses D tuples from the data set that are not unique Puts
	 * these tuples in a Vector and returns it.
	 * 
	 * @param data
	 *            set of data to choose tuples from
	 * @return a set of tuples that is a subset of the data set provided and may
	 *         contain duplicates.
	 */
	private Vector<Tuple> randomlyChooseDTuplesWithReplacement(
			Vector<Tuple> data) {
		println("[RCDTWP]: Randomly Choosing D = " + this.D
				+ " tuples WITH REPLACEMENT", 3);
		Vector<Tuple> toRet = new Vector<Tuple>();
		while (toRet.size() < this.D) {
			Tuple t = data.get((int) (Math.random() * data.size()));
			toRet.add(t);
		}
		println("[RCDTWP]: Tuples Chosen: \n" + toRet.toString(), 4);
		return toRet;
	}

	/**
	 * Construct a Random Forest based on training data and then test with
	 * testing data.
	 * 
	 * @param test
	 *            Directory Path to the testing set.
	 * @param train
	 *            Directory Path to the training set.
	 * @param _F
	 *            - the fraction of attributes for each node looks at at each
	 *            step of splitting. - if F < 0 then use 
	 *            _F * ( log_2( num_attributes ) + 1 )
	 *            to determine
	 * @param _K
	 *            - determines the number of trees in the forest by multiplying
	 *            _K with the number of attributes there are. If a negative value
	 *            is passed the the number of trees made are equal to the magnitude
	 *            of that number.
	 * @param _D
	 *            - the number of tuples for each tree to train on by multiplying
	 *            the number of tuples available by this fraction. If a number
	 *            greater than 1 or less than 0 is passed then all tuples are 
	 *            used.
	 * @throws IOException
	 */
	public EntMoot(String test, String train, float _F, float _K, float _D)
			throws IOException {
		this.setTrainingData(new Vector<Tuple>());
		this.testingData = new Vector<Tuple>();
		if (parseTrainingSet(train) == parseTestSet(test)) {
			println("[constructor]: Training set size was same as test set. Is this right?",
					0);
		}
		println("[constructor]: Training Set Size = "
				+ this.getTrainingData().size(), 0);
		println("[constructor]: Testing Set Size = " + this.testingData.size(),
				0);
		allAttributes = uniqueUnion(enumerateAttributeList(getTrainingData()),
				enumerateAttributeList(testingData));
		println("[constructor]: Size of attribute list: "
				+ allAttributes.size(), 0);
		/* WTF FIX THIS! */this.decimalF = _F; // fix this.
		if (_F < 0.0f)
			this.F = (int) (-1 * _F * (Math.log(allAttributes.size() * 1.0d) / Math
					.log(2.0d))) + 1;
		else if (_F > 1.0f || allAttributes.size() < 20)
			this.F = allAttributes.size();
		else
			this.F = (int) (_F * allAttributes.size());

		if (_D < 0.0f || _D > 1.0f)
			this.D = this.getTrainingData().size();
		else
			this.D = (int) (_D * getTrainingData().size());

		if (_K < 0.0f)
			this.K = (int) (-1 * _K);
		else
			this.K = (int) (_K * allAttributes.size());
		println("[constructor]: (F, K, D) = (" + this.decimalF + ", " + this.K + ", "
				+ this.D + ")", 0);
		forest = new Vector<Ent>();
		trainForest();
	}

	/**
	 * Enumerates forest with K trees that should be unique.
	 * Turns out it might not be...
	 */
	public void trainForest() {
		println("[trainForest]: Training Forest...", 1);
		for (int i = 0; i < this.K; i++) {
			this.forest.add(new Ent(
					randomlyChooseDTuplesWithReplacement(getTrainingData()),
					allAttributes, this.decimalF));
		}
		println("[trainForest]: Done Training Forest", 1);
	}

	/**
	 * Function to deal with the case where the forest is unsure what to
	 * classify the tuple as. Currently set to RANDOM!
	 * 
	 * @param d
	 *            tuple to deal with
	 * @return -1 or +1
	 */
	protected static int dealWithEqualCase(Tuple d) {
		if (Math.random() > 0.5d)
			return -1;
		return 1;
	}

	/**
	 * Asks the forest what they think about the data set passed. Counts how
	 * many tuples were classified true/false positive/negative or skipped.
	 * Result is in array in format: { true positive, false positive, true
	 * negative, false negative, skipped }
	 * 
	 * @param testSet
	 *            is the set of data to test the decision tree on.
	 * @return integer array containing 5 values as above.
	 */
	public int[] askEntMootMany(Vector<Tuple> data) {
		if (data == null)
			data = this.testingData;
		println("[askEntMootMany]: Data set Size = " + data.size(), 1);
		int[] results = new int[5];
		int result = 0;
		int label = 0;
		for (int i = 0; i < data.size(); i++) {
			label = data.get(i).getLabel();
			result = askEntMoot(data.get(i));
			if (result == 1) {
				if (result == label)
					results[0]++;
				else
					results[1]++;
			} else if (result == -1) {
				if (result == label)
					results[2]++;
				else
					results[3]++;
			} else {
				results[4]++;
			}
		}
		return results;
	}

	/**
	 * Asks the forest whether it thinks the tuple are Orcs or Hobbits. Does so
	 * not using Entish.
	 * 
	 * @param d
	 *            the tuple in question
	 * @return -1 if Orcs +1 if Hobbits
	 */
	private int askEntMoot(Tuple d) {
		int[] result = new int[] { 0, 0 };
		for (int i = 0; i < forest.size(); i++) {
			result[forest.get(i).testTuple(d) == 1 ? 1 : 0]++;
		}
		println("[askEntMoot]: Ent moot thought: [" + result[0] + ", "
				+ result[1] + "]", 4);
		if (result[0] > result[1])
			return -1;
		else if (result[0] < result[1])
			return 1;
		else
			return dealWithEqualCase(d);
	}

	public Vector<Tuple> getTrainingData() {
		return trainingData;
	}

	public void setTrainingData(Vector<Tuple> trainingData) {
		this.trainingData = trainingData;
	}

	public Vector<Tuple> getTestingData() {
		return testingData;
	}

}
