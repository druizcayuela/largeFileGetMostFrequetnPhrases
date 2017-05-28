import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 
 * Complexity: - Building the dictionaries is O(N), where N is the total number
 * of individual words in the file. - Writing each temporary dictionary is O(k)
 * - Selecting the items is O(m log n), where m is the number of unique words,
 * and n is the number of words you want to select.
 * 
 */
public class TopPhrases {

	private static final Logger logger = Logger.getLogger(TopPhrases.class.getName());

	private static final String FILE_NAME = "test/TopPhrases.txt";
	
	/**
	 * The maximum number of phrases to be utilized. Will be used if the limit
	 * is not provided
	 */
	private static final int DEFAULT_LIMIT = 100000;

	/**
	 * Assuming we read 10GB, divide between 10 we will have 1GB files, that we
	 * will keep in our hard disk, since the handling of 1GB we consider usable
	 * for our RAM
	 */
	private static long numSplits = 10;

	public static void main(String... args) throws IOException {

		// Invoke split algorithm here
		splitIntoMultipleFiles();

		// Create a map, where the key is the phrase,
		// and the value is the number of times phrase occurred in the file.
		Map<String, Integer> topPhrases = new LinkedHashMap<>();

		//Reading each temporary file in order to generate top phrases
		for (int destIx = 1; destIx <= numSplits; destIx++) {
			getPhrases(new FileInputStream("test/split." + destIx + ".txt"), topPhrases);
		}

		// Pass output string to method to write to file
		writeOutputToFile(getTopPhrases(topPhrases).toString());
	}

	/**
	 * Split into multiple files
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void splitIntoMultipleFiles() throws FileNotFoundException, IOException {
		// one example big text in: http://norvig.com/big.txt
		RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r");
		long sourceSize = raf.length();
		long bytesPerSplit = sourceSize / numSplits;
		long remainingBytes = sourceSize % numSplits;

		/**
		 * There is no way to read this large split chunk in one go, even if we
		 * have such a memory. Basically for each split we can read a fix size
		 * byte-array which we know should be feasible in terms of performance
		 * as well memory. NumSplits: 10 MaxReadBytes: 8KB
		 * 
		 */
		int maxReadBufferSize = 8 * 1024; // 8KB
		for (int destIx = 1; destIx <= numSplits; destIx++) {
			BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream("test/split." + destIx + ".txt"));
			if (bytesPerSplit > maxReadBufferSize) {
				long numReads = bytesPerSplit / maxReadBufferSize;
				long numRemainingRead = bytesPerSplit % maxReadBufferSize;
				for (int i = 0; i < numReads; i++) {
					readWrite(raf, bw, maxReadBufferSize);
				}
				if (numRemainingRead > 0) {
					readWrite(raf, bw, numRemainingRead);
				}
			} else {
				readWrite(raf, bw, bytesPerSplit);
			}
			bw.close();
		}
		if (remainingBytes > 0) {
			BufferedOutputStream bw = new BufferedOutputStream(
					new FileOutputStream("test/split." + (++numSplits) + ".txt"));
			readWrite(raf, bw, remainingBytes);
			bw.close();
		}
		raf.close();
	}

	/**
	 * Returns the collection of phrases the will be utilized. Phrase selection
	 * will be based on the number of times the occurred in the file.
	 * 
	 * @param inputStream
	 * @return
	 */
	private static void getPhrases(InputStream inputStream, Map<String, Integer> topPhrases) {

		try {

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;

			// Read every line of the file.
			while ((line = bufferedReader.readLine()) != null) {

				// Split the line to get the phrases.
				String[] linePhrases = line.split("\\|");

				// Read every phrase.
				for (String phrase : linePhrases) {

					// Check if the phrase is already in the collection.
					// If true, then increment the value by 1. Else add the
					// phrase
					// as the new entry to the collection.
					if (topPhrases.containsKey(phrase)) {
						topPhrases.put(phrase, topPhrases.get(phrase).intValue() + 1);
					} else {
						topPhrases.put(phrase, 1);
					}
				}
			}

		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		}
	}
	
	public static Map<String, Integer> getTopPhrases(Map<String, Integer> topPhrases){
		
		// Sort the collection by Map value.
		// Limit the collection to 100000.
		return topPhrases.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.limit(DEFAULT_LIMIT).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(v1, v2) -> v1, LinkedHashMap::new));
	}

	private static void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
		byte[] buf = new byte[(int) numBytes];
		int val = raf.read(buf);
		if (val != -1) {
			bw.write(buf);
		}
	}
	
	private static void writeOutputToFile(String str) {
		Path file = Paths.get(FILE_NAME);
		try {
			Files.write(file, str.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
