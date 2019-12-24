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
	    // prevents JAXP00010004: The accumulated size of entities is "50.000.001" that exceeded the "50.000.000" limitset by "FEATURE_SECURE_PROCESSING".
        System.setProperty("jdk.xml.totalEntitySizeLimit", String.valueOf(Integer.MAX_VALUE));

        System.out.println("Reading properties...");
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String inputFile = properties.getProperty("InputFile");
        String outputFile = properties.getProperty("OutputFile");
        boolean extractTags = Boolean.parseBoolean(properties.getProperty("ExtractTags"));
        if (extractTags) {
            System.out.println("Tag extraction activated.");
        }
        String delimiter = properties.getProperty("Delimiter");

        System.out.println("Reading view counts from " + inputFile + ", writing output to " + outputFile + "...");
        XMLInputFactory factory = XMLInputFactory.newInstance();
		try (PrintWriter printWriter = new PrintWriter(new FileWriter(outputFile))) {
            // add CSV header
		    if (extractTags) {
                printWriter.print("PostId,ViewCount,Tag\n");
            } else {
                printWriter.print("PostId,ViewCount\n");
            }

            XMLEventReader xml = factory.createXMLEventReader(new StreamSource(new File(inputFile)));

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
                        printWriter.printf("%s%s%s%s%s\n", id, delimiter, viewCount, delimiter, tag);
                    }
                } else {
                    printWriter.printf("%s%s%s\n", id, delimiter, viewCount);
                }
            }
		} catch (IOException | XMLStreamException e) {
			e.printStackTrace();
		}

        System.out.println("Done.");
    }
}
