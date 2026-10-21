package com.kooo.evcam.zeekr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Frozen prior profile versus held-out road correspondences; never a metric-distance test. */
public class BirdEyeAlignmentTest {
    private static final double[][] PREVIOUS = {
        {1.524746493108509, -0.005208178299783897, -0.006325936440197357, 0.01830193446779125, 5.381663307617981, 3.680939150957493, 0.009184350089327125, 2.358644490690996, 1},
        {-1.966167145981322, 0.04532711640949655, 0.02867818252229461, 0.04614078714071514, -4.21384985859972, -3.896487569061321, -0.04045835000852071, 2.051786238483615, 1},
        {-0.06859749417753332, 0.01213937866995913, -2.328204872037626, 1.999132909003765, 1.149917278552485, 0.8826836717310127, 0.02115174743226117, 1.229318478437866, 1},
        {0.02163463993162737, 0.1062122183403377, 2.34253581098979, -1.985361573959905, 1.219060398330053, 0.9597291093504084, -0.005024891604802594, 1.293869553358916, 1},
    };
    private BirdEyeProjection.Calibration previous(double margin) {
        BirdEyeProjection.Calibration now=BirdEyeImageFit.zeekr009(new int[]{0,1,2,3},margin);
        BirdEyeImageFit[] fits=new BirdEyeImageFit[4];
        for(int i=0;i<4;i++)fits[i]=new BirdEyeImageFit(i,PREVIOUS[i]);
        return new BirdEyeProjection.Calibration(now.length,now.width,margin,now.cameras,fits);
    }
    private static double quantile(List<Double> values,double fraction){
        Collections.sort(values);return values.get((int)Math.floor((values.size()-1)*fraction));
    }
    @Test public void improvesEveryCameraOnRoadFeaturesExcludedFromTheFit()throws Exception{
        BirdEyeProjection.Calibration before=previous(3),after=BirdEyeImageFit.zeekr009(new int[]{0,1,2,3},3);
        List<List<Double>> old=new ArrayList<>(),now=new ArrayList<>();
        for(int i=0;i<4;i++){old.add(new ArrayList<>());now.add(new ArrayList<>());}
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/009-road-heldout-20261021.csv"),StandardCharsets.UTF_8))){
            for(String line;(line=reader.readLine())!=null;){
                if(line.startsWith("#"))continue;
                String[] v=line.split(",");int role=Integer.parseInt(v[0]);double u=Double.parseDouble(v[1]),sourceV=Double.parseDouble(v[2]);
                double x=Double.parseDouble(v[3]),y=Double.parseDouble(v[4]);double[] uv=new double[2];
                assertTrue(before.project(role,x,y,uv)>0);old.get(role).add(Math.hypot(uv[0]-u,uv[1]-sourceV)*1280);
                assertTrue(after.project(role,x,y,uv)>0);now.get(role).add(Math.hypot(uv[0]-u,uv[1]-sourceV)*1280);
            }
        }
        double[] limits={2,6,6,2.5};
        for(int role=0;role<4;role++){
            assertTrue(now.get(role).size()>=5);
            double a=quantile(old.get(role),.5),b=quantile(now.get(role),.5);
            System.out.println("009 road held-out role="+role+" native-source median px "+a+" -> "+b);
            assertTrue("Reference image alignment must improve, role="+role,b<a*.75);
            assertTrue("Bound held-out source error, role="+role,b<limits[role]);
            assertTrue("Tail must improve too",quantile(now.get(role),.95)<quantile(old.get(role),.95));
        }
    }
    @Test public void revisedProfileDoesNotAddUncoveredGroundAtAnySupportedMargin(){
        double[] uv=new double[2];
        for(double margin:new double[]{.5,2,3}){
            BirdEyeProjection.Calibration before=previous(margin),after=BirdEyeImageFit.zeekr009(new int[]{0,1,2,3},margin);
            for(int row=0;row<140;row++)for(int col=0;col<100;col++){
                double x=(col+.5-50)*(2+2*margin)/100,y=(70-row-.5)*(5.2+2*margin)/140;
                if(before.insideCar(x,y))continue;
                boolean was=false,is=false;
                for(int role=0;role<4;role++){
                    was|=before.project(role,x,y,uv)>0;is|=after.project(role,x,y,uv)>0;
                }
                assertTrue("New ground hole at "+x+","+y+" margin="+margin,!was||is);
            }
        }
    }
}
