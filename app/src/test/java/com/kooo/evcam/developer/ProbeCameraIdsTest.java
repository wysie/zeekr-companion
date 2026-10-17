package com.kooo.evcam.developer;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ProbeCameraIdsTest {
 @Test public void includesAdvertisedPhysicalAndNumericIdsWithoutDuplicating() {
  List<String> ids=ProbeCameraIds.candidates(Arrays.asList("3","vendor-front"),Arrays.asList("2","physical-a","3"));
  assertEquals("3",ids.get(0)); assertEquals("vendor-front",ids.get(1));
  assertTrue(ids.contains("physical-a")); assertTrue(ids.contains("0"));assertTrue(ids.contains("15"));
  assertFalse(ids.contains("16"));assertEquals(ids.size(),new HashSet<>(ids).size());
 }
 @Test public void candidatesAreBoundedAndMalformedNamesAreIgnored() {
  List<String> reported=new ArrayList<>(); reported.add(null);reported.add("");reported.add(" ");
  for(int i=0;i<200;i++)reported.add("vendor-"+i);
  List<String> ids=ProbeCameraIds.candidates(Arrays.asList("0","1"),reported);
  assertEquals(48,ids.size());assertEquals("0",ids.get(0));assertFalse(ids.contains(null));assertFalse(ids.contains(""));
 }
}
