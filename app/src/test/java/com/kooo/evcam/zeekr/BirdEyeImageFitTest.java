package com.kooo.evcam.zeekr;

import org.junit.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class BirdEyeImageFitTest {
    private BirdEyeProjection.Calibration fitted() { return BirdEyeImageFit.zeekr009(new int[]{0,1,2,3}, 2); }

    @Test public void improvesCorrespondencesFromEarlierCaptureNotUsedForThisFit() throws Exception {
        BirdEyeProjection.Calibration calibration = fitted();
        List<List<Double>> old = new ArrayList<>(), now = new ArrayList<>();
        for (int i=0;i<4;i++) { old.add(new ArrayList<>()); now.add(new ArrayList<>()); }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/009-reference-rays.csv"), StandardCharsets.UTF_8))) {
            for (String line; (line=reader.readLine()) != null;) {
                if (line.startsWith("#")) continue;
                String[] v=line.split(","); int role=Integer.parseInt(v[0]);
                double u=Double.parseDouble(v[1]), sourceV=Double.parseDouble(v[2]);
                double x=Double.parseDouble(v[3]), y=Double.parseDouble(v[4]);
                double[] uv = new double[2];
                assertTrue(calibration.cameras[role].project(x,y,uv)>0);
                old.get(role).add(Math.hypot(uv[0]-u,uv[1]-sourceV)*1280);
                assertTrue(calibration.project(role,x,y,uv)>0);
                now.get(role).add(Math.hypot(uv[0]-u,uv[1]-sourceV)*1280);
            }
        }
        for (int role=0;role<4;role++) {
            double before=median(old.get(role)), after=median(now.get(role));
            System.out.println("009 image fit role="+role+" cross-capture median source px: "+before+" -> "+after);
            assertTrue("Earlier capture must improve for every camera", after<before*.5);
            assertTrue("This check is image alignment, not measured distance", after<12);
        }
    }

    @Test public void everyRoleStaysOutwardAndFiniteWithoutClampingInvalidRays() {
        BirdEyeProjection.Calibration calibration=fitted();
        double[] uv=new double[2];
        int[] covered=new int[4];
        for(int row=0;row<96;row++) for(int col=0;col<64;col++) {
            double x=(col+.5-32)*.1,y=(48-row-.5)*.1;
            for(int role=0;role<4;role++) if(calibration.project(role,x,y,uv)>0) {
                assertTrue(Double.isFinite(uv[0])&&uv[0]>=0&&uv[0]<=1);
                assertTrue(Double.isFinite(uv[1])&&uv[1]>=0&&uv[1]<=1);
                assertTrue(role==0 ? y>2.6 : role==1 ? y< -2.6 : role==2 ? x< -1 : x>1);
                covered[role]++;
            }
        }
        for(int n:covered) assertTrue(n>500);
        for(int role=0;role<4;role++) {
            assertEquals(0,calibration.project(role,0,0,uv),0);
            assertEquals(0,calibration.project(role,Double.NaN,4,uv),0);
            assertEquals(0,calibration.project(role,4,Double.POSITIVE_INFINITY,uv),0);
        }
    }

    @Test public void sourceAssignmentChangesPixelsWithoutChangingPhysicalRoleProjection() {
        BirdEyeProjection.Calibration original=fitted();
        BirdEyeProjection.Calibration permuted=BirdEyeImageFit.zeekr009(new int[]{3,2,1,0},2);
        double[] a=new double[2],b=new double[2];
        double[][] places={{0,4},{0,-4},{-2,0},{2,0}};
        for(int role=0;role<4;role++) {
            assertEquals(3-role,permuted.cameras[role].lane);
            assertTrue(original.project(role,places[role][0],places[role][1],a)>0);
            assertTrue(permuted.project(role,places[role][0],places[role][1],b)>0);
            assertArrayEquals(a,b,0);
        }
        assertTrue(original.describe().contains(BirdEyeImageFit.PROFILE));
        assertTrue(original.describe().contains("not measured distances"));
    }

    @Test public void fittedMapSamplesLiveLanesWithoutSeparatorOrUncoveredInteriorLeakage() {
        StreamLayoutTable.setCompositeCameraId("image-fit-test");
        try {
            CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("image-fit-test",1280,5140);
            int[] source=new int[1280*5140],output=new int[640*960];
            java.util.Arrays.fill(source,0xffff00ff);
            int[] colours={0xffff0000,0xff00ff00,0xff0000ff,0xffffff00};
            for(int i=0;i<4;i++) {
                CompositeStreamGeometry.Lane lane=plan.lane(i);
                for(int y=lane.y;y<lane.y+lane.height;y++) java.util.Arrays.fill(source,y*1280,(y+1)*1280,colours[i]);
            }
            new BirdEyeProjection.Map(fitted(),plan,1280,5140,640,960).render(source,output);
            assertEquals(colours[0],output[100*640+320]);
            assertEquals(colours[1],output[860*640+320]);
            assertEquals(colours[2],output[480*640+50]);
            assertEquals(colours[3],output[480*640+590]);
            assertEquals(0xff000000,output[480*640+320]);
            for(int pixel:output) assertNotEquals(0xffff00ff,pixel);
            // No reference-photo pixels are cached: a second live buffer changes the output.
            for(int i=0;i<source.length;i++) if(source[i]!=0xffff00ff) source[i]=0xff123456;
            new BirdEyeProjection.Map(fitted(),plan,1280,5140,640,960).render(source,output);
            assertEquals(0xff123456,output[100*640+320]);
        } finally { StreamLayoutTable.reset(); }
    }

    @Test public void invalidMatricesAndUnsupportedProfileExtentsAreRejected() {
        for(double[] matrix:new double[][]{new double[9],{1,0,0,0,Double.NaN,0,0,0,1},{1,0,0}}) {
            try { new BirdEyeImageFit(0,matrix); fail("Must reject invalid transform"); }
            catch(IllegalArgumentException expected) { }
        }
        for(int[] lanes:new int[][]{{0,0,2,3},{0,1,2,4},{0,1}}) {
            try { BirdEyeImageFit.zeekr009(lanes,2); fail("Must reject duplicate or invalid lanes"); }
            catch(IllegalArgumentException expected) { }
        }
        for(double margin:new double[]{0,3.01,Double.NaN}) {
            try { BirdEyeImageFit.zeekr009(new int[]{0,1,2,3},margin);fail("Fitted extent is bounded"); }
            catch(IllegalArgumentException expected) { }
        }
        double[] identity={1,0,0,0,1,0,0,0,1};
        BirdEyeImageFit fit=new BirdEyeImageFit(0,identity);
        identity[0]=Double.NaN;
        assertFalse(fit.describe().contains("NaN"));
        BirdEyeImageFit backward = new BirdEyeImageFit(0,new double[]{1,0,0,0,1,0,0,0,-1});
        assertEquals(0,backward.project(0,3,new double[2]),0);
    }

    @Test public void widerFramingKeepsObservedCoverageAndTheOriginalTransforms() {
        BirdEyeProjection.Calibration close = fitted();
        BirdEyeProjection.Calibration wide = BirdEyeImageFit.zeekr009(new int[]{0,1,2,3}, 3);
        double[] a = new double[2], b = new double[2];
        for (int row=0; row<120; row++) for (int col=0; col<80; col++) {
            double x=(col+.5-40)*.1, y=(60-row-.5)*.1;
            if (wide.insideCar(x,y)) continue;
            boolean covered=false;
            for (int role=0; role<4; role++) {
                double oldWeight=close.project(role,x,y,a), newWeight=wide.project(role,x,y,b);
                assertEquals("Framing cannot change a ground ray",oldWeight,newWeight,0);
                if (newWeight>0) {
                    assertArrayEquals(a,b,0); covered=true;
                    assertTrue(b[0]>=0&&b[0]<=1&&b[1]>=0&&b[1]<=1);
                }
            }
            assertTrue("Expanded 009 extent must have actual lens coverage",covered);
        }
    }

    private double median(List<Double> values) {
        assertFalse(values.isEmpty()); Collections.sort(values);
        return values.size()%2==1?values.get(values.size()/2):
                (values.get(values.size()/2-1)+values.get(values.size()/2))/2;
    }
}
