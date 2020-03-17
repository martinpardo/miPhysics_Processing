/*
Model: Dynamic Topology Modifications.
Author: James Leonard (james.leonard@gipsa-lab.fr)

Draw a 2D Mesh of Osc modules (mass-spring-ground systems).

Left-Click Drag across the surface to apply forces and create ripples.
Right-Click Drag across the surface to remove masses (and connected links).

Use UP and DOWN keys to add/decrease air friction in the model.
Use LEFT and RIGHT keys to zoom the Z axis.
*/
import miPhysics.Engine.*;
import miPhysics.ModelRenderer.*;

import peasy.*;
PeasyCam cam;


int displayRate = 60;


int dimX = 100;
int dimY = 100;

float zZoom = 1;

PhysicalModel mdl;
Driver3D d;

ModelRenderer renderer;

int mouseDragged = 0;

int gridSpacing = 2;
int xOffset= 0;
int yOffset= 0;

float fric = 0.001;

// SETUP: THIS IS WHERE WE SETUP AND INITIALISE OUR MODEL

void setup() {
  fullScreen(P3D);
  background(0);

  mdl = new PhysicalModel(441, displayRate);
  mdl.setGlobalFriction(fric);
  
  gridSpacing = (int)((height/dimY)*2);
  generateMesh(mdl, dimX, dimY, "osc", "spring", 1., gridSpacing, 0.0006, 0.0, 0.009, 0.1);
  
  d = mdl.addInOut("driver", new Driver3D(), "osc0_0");

  mdl.init();
  
  renderer = new ModelRenderer(this);
  
  renderer.displayMasses(false);
  renderer.setColor(interType.SPRINGDAMPER1D, 155, 100, 200, 255);
    renderer.setStrainGradient(interType.SPRINGDAMPER1D, true, 1);
    renderer.setStrainColor(interType.SPRINGDAMPER1D, 255, 250, 255, 255);

  
  frameRate(displayRate);   

} 

// DRAW: THIS IS WHERE WE RUN THE MODEL SIMULATION AND DISPLAY IT

void draw() {

  camera(width/2.0, height/2.0, (height/2.0) / tan(PI*30.0 / 180.0), width/2, height/2.0, 0, 0, 1, 0);

  mdl.compute();

  background(0);

  pushMatrix();
  translate(xOffset,yOffset, 0.);
  renderer.renderModel(mdl);
  popMatrix();

  fill(255);
  textSize(13); 

  text("Friction: " + fric, 50, 50, 50);
  text("Zoom: " + zZoom, 50, 100, 50);

  
  if (mouseDragged == 1){
    if((mouseX) < (dimX*gridSpacing+xOffset) & (mouseY) < (dimY*gridSpacing+yOffset) & mouseX>xOffset & mouseY > yOffset){ // Garde fou pour ne pas sortir des limites du pinScreen
      println(mouseX, mouseY);
      if(mouseButton == LEFT)
        engrave(mouseX-xOffset, mouseY - yOffset);
      if(mouseButton == RIGHT)
        chisel(mouseX-xOffset, mouseY - yOffset);
    }
  }
 
}


void engrave(float mX, float mY){
  String matName = "osc" + floor(mX/ gridSpacing)+"_"+floor(mY/ gridSpacing);
  Mass m = mdl.getMass(matName);
  if(m != null){
    d.moveDriver(m);
    d.applyFrc(new Vect3D(0., 0., 15.));
  }
}

void chisel(float mX, float mY){
  String matName = "osc" + floor(mX/ gridSpacing)+"_"+floor(mY/ gridSpacing);
  Mass m = mdl.getMass(matName);
  if(m != null){
    mdl.removeMassAndConnectedInteractions(m);
  }
}

void mouseDragged() {
  mouseDragged = 1;
  
}

void mouseReleased() {
  mouseDragged = 0;
}



void keyPressed() {

  if(keyCode == UP){
    fric += 0.001;
    mdl.setGlobalFriction(fric);
    println(fric);

  }
  else if (keyCode == DOWN){
    fric -= 0.001;
    fric = max(fric, 0);
    mdl.setGlobalFriction(fric);
    println(fric);
  }
  else if (keyCode == LEFT){
    zZoom ++;
    renderer.setZoomVector(1,1, zZoom);
  }
  else if (keyCode == RIGHT){
    zZoom --;
    renderer.setZoomVector(1,1, zZoom);
  }
}
