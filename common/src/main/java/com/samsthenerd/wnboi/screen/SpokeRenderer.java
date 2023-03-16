package com.samsthenerd.wnboi.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.samsthenerd.wnboi.utils.RenderUtils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper.Argb;
import net.minecraft.util.math.Vec3d;

/*
 * A modular renderer for a single spoke of a wheel.
 * Can extend and override to customize.
 */
@Environment(value=EnvType.CLIENT)
public class SpokeRenderer implements Drawable{
    protected double outerRadius; // radius of the wheel
    protected double innerRadius = 0; // distance from center to the inner edge of the spoke
    protected double gap=10; // distance between adjacent spokes before adding outline
    protected double angleOffset = Math.PI * 0.5; // should edit this from 

    protected double outerOutlineWeight = 0; // how thick the outline should be
    protected double innerOutlineWeight = 0; // how thick the outline should be

    protected double originX; // center of the wheel
    protected double originY;
    
    protected int sections; // number of sections, so we know arc radius
    protected int sectionIndex; // so we know which one we're actually trying to render
    
    protected int numDivisions = 5; // how many triangles to use to render the arc
    
    double pullOutDistance = 0; // if you want to expand it like a pie chart kinda

    // these just get calculated once and then stored
    protected double startAngle;
    protected double endAngle;
    protected double midAngle;
    double gapHypot; // distance from center needed to make the gaps
    protected double offsetX; // based on gap
    protected double offsetY;

    protected boolean isSelected;



    SpokeRenderer(double orX, double orY, double rad, int numSecs, int secIndex){
        this.originX = orX;
        this.originY = orY;
        this.outerRadius = rad;
        this.sections = numSecs;
        this.sectionIndex = secIndex;

        initConsts();
        // WNBOI.LOGGER.info("made a new wheel section renderer with radius " + rad + " and " + numSecs + " sections");
    }

    // make sure this gets called at some point before rendering and after changing any relevant values
    public void initConsts(){
        startAngle = 2*Math.PI *sectionIndex / sections - angleOffset;
        endAngle = 2*Math.PI *(sectionIndex+1) / sections - angleOffset;
        midAngle = 2*Math.PI * (sectionIndex + 0.5) / sections - angleOffset;
        
        gapHypot = (gap/2) * Math.sin(Math.PI / sections);
        
        offsetX = (gapHypot+pullOutDistance) * Math.cos(midAngle)/2;
        offsetY = (gapHypot+pullOutDistance) * Math.sin(midAngle)/2;
    }

    public SpokeRenderer setGap(double gap){
        this.gap = gap;
        initConsts();
        return this;
    }

    public SpokeRenderer setInnerRadius(double rad){
        this.innerRadius = rad;
        return this;
    }

    public SpokeRenderer setAngleOffset(double theta){
        this.angleOffset = theta;
        initConsts();
        return this;
    }

    public void select(){
        isSelected = true;
        innerOutlineWeight = 0.75;
        pullOutDistance = 10;
        innerRadius = outerRadius* 0.2;

        initConsts();
    }

    public void unselect(){
        isSelected = false;
        innerOutlineWeight = 0;
        pullOutDistance = 0;
        innerRadius = 0;

        initConsts();
    }

    public List<Vec3d> getOuterCurvePoints(){
        List<Vec3d> curvePointsRaw;
        if(outerRadius <= 0){
            curvePointsRaw = new ArrayList<Vec3d>();
            curvePointsRaw.add(new Vec3d(0,0,0)); 
        } else {
            curvePointsRaw = RenderUtils.calcArcPoints(numDivisions, outerRadius-gapHypot, startAngle, endAngle);
        }
        ArrayList<Vec3d> curvePoints = new ArrayList<Vec3d>();
        for(Vec3d point : curvePointsRaw){
            curvePoints.add(point.add(new Vec3d(originX+offsetX, originY+offsetY, 0)));
        }
        return curvePoints;
    }

    public List<Vec3d> getInnerCurvePoints(){
        List<Vec3d> curvePointsRaw;
        if(innerRadius <= 0){
            curvePointsRaw = new ArrayList<Vec3d>();
            curvePointsRaw.add(new Vec3d(0,0,0)); 
        } else {
            curvePointsRaw = RenderUtils.calcArcPoints(numDivisions, innerRadius-gapHypot, startAngle, endAngle);
        }
        ArrayList<Vec3d> curvePoints = new ArrayList<Vec3d>();
        for(Vec3d point : curvePointsRaw){
            curvePoints.add(point.add(new Vec3d(originX+offsetX, originY+offsetY, 0)));
        }
        return curvePoints;
    }

    
    public void drawFill(MatrixStack matrices, int mouseX, int mouseY, float delta){
        List<Vec3d> outerCurvePoints = getOuterCurvePoints();
        List<Vec3d> innerCurvePoints = getInnerCurvePoints();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        
        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP , VertexFormats.POSITION_COLOR);


        int numPoints = outerCurvePoints.size() > innerCurvePoints.size() ? outerCurvePoints.size() : innerCurvePoints.size();

        double inMult = innerCurvePoints.size() / (double)numPoints;
        double outMult = outerCurvePoints.size() / (double)numPoints;

        for(int i = 0; i < numPoints; i++){
            int inI = (int)Math.floor(i * inMult);
            int outI = (int)Math.floor(i * outMult);
            int inARGB = getColorFill(i, outerCurvePoints.size(), innerCurvePoints.size(), true);
            int outARGB = getColorFill(i, outerCurvePoints.size(), innerCurvePoints.size(), false);
            bufferBuilder.vertex(innerCurvePoints.get(inI).x, innerCurvePoints.get(inI).y, 0).color(inARGB).next();
            bufferBuilder.vertex(outerCurvePoints.get(outI).x, outerCurvePoints.get(outI).y, 0).color(outARGB).next();
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        tessellator.draw();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    public List<Vec3d> getOutlinePoints(){
        List<Vec3d> outerCurvePoints = getOuterCurvePoints();
        List<Vec3d> innerCurvePoints = getInnerCurvePoints();
        Collections.reverse(outerCurvePoints); // so that we go backwards around the arc
        List<Vec3d> rawOutline = new ArrayList<Vec3d>();
        rawOutline.addAll(innerCurvePoints);
        rawOutline.addAll(outerCurvePoints);
        rawOutline.add(innerCurvePoints.get(0)); // so that we end up back at the start
        return rawOutline;
    }

    // need to clean this up
    public void drawOutline(MatrixStack matrices, int mouseX, int mouseY, float delta){
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP , VertexFormats.POSITION_COLOR);

        List<Vec3d> rawOutline = getOutlinePoints();
        List<Vec3d> outlineStroke = RenderUtils.calcStroke(rawOutline, outerOutlineWeight, innerOutlineWeight);

        for(int v = 0; v < outlineStroke.size(); v++){
            Vec3d point = outlineStroke.get(v);
            int vColor = getColorOutline(v);
            bufferBuilder.vertex(point.x, point.y, 0).color(vColor).next();
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        tessellator.draw();
        RenderSystem.enableCull();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta){
        // WNBOI.LOGGER.info("rendering spoke " + sectionIndex);
        drawFill(matrices, mouseX, mouseY, delta);

        drawOutline(matrices, mouseX, mouseY, delta);
        return;
    }

    // gets the color for a specific vertex. returns argb value
    public int getColorFill(int vI, int numOuter, int numInner, boolean isInner){
        if(isInner || !isSelected){
            return Argb.getArgb(96, 153, 102, 204);
        } else {
            return Argb.getArgb(150, 38, 97, 156);
        }
    }

    // gets the color for a specific point on the outline. returns argb value
    public int getColorOutline(int vI){
        return Argb.getArgb(128, 255,255,255);
        // need to add functions to determine where on outline it is once i rework that to have more built in options
    }




}