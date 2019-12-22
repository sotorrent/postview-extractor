import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Arrays;
import java.util.Properties;

import static java.util.function.Predicate.not;

class PostViewExtractor {
	public static void main(String[] args) {
	    // read properties
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String inputFile = properties.getProperty("OutputFile");
        String outputFile = properties.getProperty("InputFile");
        boolean extractTags = Boolean.parseBoolean(properties.getProperty("ExtractTags"));
        String delimiter = properties.getProperty("Delimiter");

        XMLInputFactory factory = XMLInputFactory.newInstance();

        // extract view count (optionally including tags) from input XML file and write value to output CSV file
		try (PrintWriter printWriter = new PrintWriter(new FileWriter(inputFile))) {
            // add CSV header
		    if (extractTags) {
                printWriter.println("Id,ViewCount,Tag");
            } else {
                printWriter.println("Id,ViewCount");
            }

            XMLEventReader xml = factory.createXMLEventReader(new StreamSource(new File(outputFile)));

            while (xml.hasNext()) {
                XMLEvent event = xml.nextEvent();

                if (!event.isStartElement()) {
                    continue;
                }

                StartElement elem = event.asStartElement();

                // only consider row elements containing question metadata (only questions have a view count)
                if (!elem.getName().getLocalPart().equals("row")
                        || !(elem.getAttributeByName(new QName("PostTypeId")).getValue().equals("1"))) {
                    continue;
                }
                String id = elem.getAttributeByName(new QName("Id")).getValue();
                String viewCount = elem.getAttributeByName(new QName("ViewCount")).getValue();

                // write data to CSV
                if (extractTags) {
                    String[] tags = Arrays.stream(elem.getAttributeByName(new QName("Tags")).getValue()
                            .replaceAll("<", "")
                            .replaceAll(">", ";")
                            .split(";"))
                            .filter(not(String::isEmpty))
                            .toArray(String[]::new);
                    for (String tag : tags) {
                        printWriter.printf("%s%s%s%s%s", id, delimiter, viewCount, delimiter, tag);
                        printWriter.println();
                    }
                } else {
                    printWriter.printf("%s%s%s", id, delimiter, viewCount);
                    printWriter.println();
                }
            }
		} catch (IOException | XMLStreamException e) {
			e.printStackTrace();
		}
    }
}
