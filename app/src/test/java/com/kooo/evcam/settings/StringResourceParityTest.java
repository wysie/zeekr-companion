package com.kooo.evcam.settings;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

/** Check every text resource file, including help, About, feature files, plurals and arrays. */
public class StringResourceParityTest {
    private static final Pattern FORMAT = Pattern.compile(
            "%((\\d+)\\$)?([-#+0,(<]*)\\d*(?:\\.\\d+)?([tT])?([a-zA-Z%])");
    private static final String TOOLS = "http://schemas.android.com/tools";

    @Test public void everySupportedLanguageCoversAllTranslatableResources() throws Exception {
        File res = resources();
        Map<String, Entry> base = read(new File(res, "values"));
        assertTrue("No default resources found", base.size() > 1400);
        for (String language : Arrays.asList("en", "ms", "zh")) {
            Map<String, Entry> translated = read(new File(res, "values-" + language));
            for (String key : translated.keySet()) {
                assertTrue(language + " has unknown text resource " + key, base.containsKey(key));
            }
            for (Map.Entry<String, Entry> item : base.entrySet()) {
                Entry source = item.getValue();
                Entry target = translated.get(item.getKey());
                if (target == null && source.language.equals(language)) target = source;
                assertNotNull("Missing " + language + " translation: " + item.getKey(), target);
                assertEquals(item.getKey() + " type in " + language, source.kind, target.kind);
                assertEquals(item.getKey() + " formatted flag in " + language, source.formatted, target.formatted);
                if (source.kind.equals("plurals")) {
                    assertTrue(item.getKey() + " needs other in " + language, target.items.containsKey("other"));
                    assertEquals(item.getKey() + "/other in " + language,
                            formats(source.items.get("other")), formats(target.items.get("other")));
                    for (String value : target.items.values()) {
                        Map<String, Integer> extra = new TreeMap<>(formats(value));
                        // Singular forms may omit the count but must not require an extra argument/type.
                        extra.keySet().removeAll(formats(source.items.get("other")).keySet());
                        assertTrue(item.getKey() + " adds plural arguments in " + language, extra.isEmpty());
                    }
                } else {
                    assertEquals(item.getKey() + " item count in " + language, source.items.size(), target.items.size());
                    for (String part : source.items.keySet()) {
                        assertTrue(item.getKey() + " lacks " + part, target.items.containsKey(part));
                        assertFalse(item.getKey() + " empty in " + language, target.items.get(part).trim().isEmpty());
                        if (source.formatted) assertEquals(item.getKey() + "/" + part + " format in " + language,
                                formats(source.items.get(part)), formats(target.items.get(part)));
                    }
                }
            }
        }
    }

    @Test public void formatChecksIncludeTypesIndexesRepetitionsAndLiteralPercents() {
        assertEquals(formats("%2$s %1$.2f %% %3$d"), formats("%3$d %1$.2f %2$s %%"));
        assertNotEquals(formats("%1$s"), formats("%1$d"));
        assertNotEquals(formats("%1$s"), formats("%1$s %1$s"));
        assertEquals(Collections.emptyMap(), formats("100%%"));
        assertNotEquals(formats("%12$s"), formats("%1$s"));
        assertNotEquals(formats("%1$tY"), formats("%1$s"));
    }

    private static Map<String, Integer> formats(String value) {
        Map<String, Integer> result = new TreeMap<>();
        Matcher matcher = FORMAT.matcher(value);
        int next = 1, last = 0;
        while (matcher.find()) {
            String type = matcher.group(5);
            if (type.equals("%") || type.equals("n")) continue;
            int index = matcher.group(2) != null ? Integer.parseInt(matcher.group(2))
                    : matcher.group(3).contains("<") ? last : next++;
            last = index;
            String key = index + ":" + (matcher.group(4) == null ? "" : matcher.group(4)) + type;
            result.put(key, result.getOrDefault(key, 0) + 1);
        }
        return result;
    }

    private static Map<String, Entry> read(File directory) throws Exception {
        Map<String, Entry> entries = new TreeMap<>();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        if (files == null) return entries;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        for (File file : files) {
            Element root = factory.newDocumentBuilder().parse(file).getDocumentElement();
            String language = root.getAttributeNS(TOOLS, "locale");
            if (language.isEmpty()) language = "zh"; // Canonical default strings are Simplified Chinese.
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                if (!(nodes.item(i) instanceof Element)) continue;
                Element element = (Element) nodes.item(i);
                String kind = element.getTagName();
                if (!Arrays.asList("string", "plurals", "string-array").contains(kind)
                        || element.getAttribute("translatable").equals("false")) continue;
                Entry entry = new Entry(kind, language);
                entry.formatted = !element.getAttribute("formatted").equals("false");
                if (kind.equals("string")) entry.items.put("text", element.getTextContent());
                else {
                    NodeList items = element.getElementsByTagName("item");
                    for (int j = 0; j < items.getLength(); j++) {
                        Element child = (Element) items.item(j);
                        entry.items.put(kind.equals("plurals") ? child.getAttribute("quantity")
                                : Integer.toString(j), child.getTextContent());
                    }
                }
                String key = element.getAttribute("name");
                assertNull("Duplicate " + key + " in " + file, entries.put(key, entry));
            }
        }
        return entries;
    }

    private static File resources() {
        File dir = new File(System.getProperty("user.dir", "."));
        for (int i = 0; i < 5 && dir != null; i++, dir = dir.getParentFile()) {
            for (String path : Arrays.asList("src/main/res", "app/src/main/res")) {
                File found = new File(dir, path);
                if (new File(found, "values/strings.xml").isFile()) return found;
            }
        }
        throw new AssertionError("Cannot locate app resources");
    }
    private static final class Entry {
        final String kind, language;
        boolean formatted = true;
        final Map<String, String> items = new LinkedHashMap<>();
        Entry(String kind, String language) { this.kind = kind; this.language = language; }
    }
}
