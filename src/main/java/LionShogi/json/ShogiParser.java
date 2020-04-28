package LionShogi.json;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.stream.*;
import javax.json.stream.JsonParser.Event;

/** Class to parse JSON scripts in LionShogi: 
 * piece informations, game rules, properties, etc. 
 * This class is a revision from RedditParser in CS1003-P2.*/
public class ShogiParser implements AutoCloseable
{
	// JSON variables for parsing
	private JsonParserFactory parserMaker = Json.createParserFactory(null);
	private JsonParser.Event eventType;
	private JsonParser scriptParser;

	// Variables for object hierarchy getting and object attributes reading
	private ArrayList<HashMap<String, String>> objectHierarchy;
	private HashMap<String, String> currObjPtr;
	private String objectKey;

	// ShogiSelector for use of the shogi parser.
	private ShogiSelector[] itemSelector;

	/** Method to collect JSON information step by step. */
	public void step()
	{
		
		try // Check for JSON error
		{
			// Assign event to next step in JSON reading
			eventType = scriptParser.next();

			// Read current event
			switch (eventType)
			{
				case KEY_NAME: // On key name event - store key for value pair
					objectKey = scriptParser.getString();
					break;

				case VALUE_STRING: // On value found - store it to current object properties.
				case VALUE_NUMBER:
					// Check if number or string
					String value;
					if (eventType == Event.VALUE_NUMBER) 
						value = String.valueOf(scriptParser.getInt());
					else value = scriptParser.getString();

					if (currObjPtr.containsKey("array")) // If current value is in an array.
					{
						// Insert to object map by index, e.g. randKey_1,2,3...
						int index = Integer.valueOf(currObjPtr.get("iter"));
						String arrObjKey = currObjPtr.get("array") + "_" + index;
						index++; 
						currObjPtr.put("iter", String.valueOf(index));
						currObjPtr.put(arrObjKey, value);
					}
					// If current value is in a normal object.
					else currObjPtr.put(objectKey, value);
					break;

				case START_OBJECT: // On object found - start new object with properties.
					if (!objectHierarchy.isEmpty())
					{
						if (currObjPtr.containsKey("array")) // If array - get accurate object key.
						{
							int index = Integer.valueOf(currObjPtr.get("iter"));
							objectKey =  currObjPtr.get("array");
							String arrObjKey = objectKey + "_" + index;
							index++; 
							currObjPtr.put("iter", String.valueOf(index));

							currObjPtr.put(arrObjKey, "object");
						}
						else
							currObjPtr.put(objectKey, "object");
					}
					else objectKey = "root";

					HashMap<String, String> newObjMap = new HashMap<>();
					newObjMap.put("name", objectKey);
					addToHierarchy(newObjMap);
					break;

				case END_OBJECT: // On object end - execute selector method & delete latest obj.
					for (ShogiSelector currSelector: itemSelector)
						currSelector.onObject(currObjPtr);
					removeLastElemOfHierarchy();
					break;

				case START_ARRAY: // On start of an array - create new object & mark it as array.
					// Mark current object has an array.
					currObjPtr.put(objectKey, "array");

					// Create new object to hierarchy and give it array object properties.
					HashMap<String, String> arrayMap = new HashMap<>();
					arrayMap.put("array", objectKey);
					arrayMap.put("iter", "0");
					addToHierarchy(arrayMap);
					break;

				case END_ARRAY: // In end of an array - transfer array components to prev object.
					HashMap<String, String> delArrayMap = currObjPtr;
					removeLastElemOfHierarchy();
					for (String currKey : delArrayMap.keySet()) // Loop array map.
					{
						// Place elements of array to map.
						if (currKey.contains(delArrayMap.get("array")))
							currObjPtr.put(currKey, delArrayMap.get(currKey));

						else if (currKey.equals("iter")) // Place array iterations to map.
						{
							String arrIterStr = delArrayMap.get("array") + "_" + "iter";
							currObjPtr.put(arrIterStr, delArrayMap.get("iter"));
						}
					}
					break;

				case VALUE_NULL: // On value null - ignore.
				case VALUE_FALSE:
				case VALUE_TRUE:
					break;
			}
		}
		catch(JsonException e) // In case of JSON exception, quit.
		{
			System.err.println("Json error!\n" + e.getMessage() + "\nQuitting...");
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	* Method which returns corresponding value to key on current object.
	* @param key the key to check for.
	* @return value corresponding to key if it exists, else null.
	 */
	public String getObjValueAt(String key)
	{
		if (currObjPtr.containsKey(key))
		{
			return currObjPtr.get(key);
		}
		else return null;
	}

	/**
	 * Removes latest object from object hierarchy and updates the object pointer. 
	  */
	public void removeLastElemOfHierarchy()
	{
		objectHierarchy.remove(currObjPtr);
		if (!objectHierarchy.isEmpty())
			currObjPtr = objectHierarchy.get(objectHierarchy.size() - 1);
		else currObjPtr = null;
	}

	/**
	* Adds given object to object hieratchy and updates the object pointer.
	* @param addingElem the element to add to the object hierarchy.
	 */
	public void addToHierarchy(HashMap<String, String> addingElem)
	{
		objectHierarchy.add(addingElem);
		currObjPtr = addingElem;
	}

	/**
	* Plugs a ShogiSelector into the parser.
	* @param newSelector the ShogiSelector object to plug to the parser.
	 */
	public void plugSelector(ShogiSelector newSelector)
	{
		itemSelector = new ShogiSelector[1];
		itemSelector[0] = newSelector;
	}

	/**
	* Plugs an array of ShogiSelectors into the parser.
	* @param newSelectorArray the array of ShogiSelector objects to plug to the parser.
	 */
	public void plugSelector(ShogiSelector[] newSelectorArray)
	{
		itemSelector = newSelectorArray;
	}

	/**
	* Initializes the script parser with set file path.
	* @param filepath the path to the JSON file to parse.
	 */
	public void init(String filepath)
	{
		try
		{
			// Create the parser from the streaming API.
			FileReader jsonReader = new FileReader(filepath);
			scriptParser = parserMaker.createParser(jsonReader);

			// Initialize the object hierarchy
			objectHierarchy = new ArrayList<>();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("File not found!");
			System.exit(0);
		}
	}

	public void parseToCompletion()
	{
		while (scriptParser.hasNext()) step();
	}

	/**
	* Constructor for a shogi parser.
	* @param filepath the path to the JSON script to parse.
	* @param plugList the list of ShogiSelectors to plug to the parser.
	 */
	public ShogiParser (String filepath, ShogiSelector[] plugList)
	{
		plugSelector(plugList);
		init(filepath);
	}

	@Override
	/** Closes all elements of the object.
	 * Executed automatically on try with resources statements. */
	public void close()
	{
		scriptParser.close();
		eventType = null;
		parserMaker = null;
		objectHierarchy = null;
	}
}
