#include "planet.h"

/* Flags */
int fullscreen = FALSE;
int stereo = FALSE;
int showconstruct = FALSE;
int drawwireframe = FALSE;
int uselights = TRUE;
int dosmooth = TRUE;
int windowdump = FALSE;
int record = FALSE;
int debug = FALSE;
int demomode = FALSE;

#define NOTDIRTY      0
#define SLIGHTLYDIRTY 1
#define REALDIRTY     2
#define ADDONE        3
int geometrydirty = REALDIRTY;

/* Planet description */
TF *faces = NULL;
int nface = 0;
int spheredepth = 6;
int iterationdepth = 0;
double radiusmin=1,radiusmax=1;
int colourmap = 12;
int showocean = FALSE;
double deltaheight = 0.00001;
long seedvalue = 12345;
int whichmethod = 1;

int currentbutton = -1;
double dtheta = 1;
CAMERA camera;
double near,far;
XYZ origin = {0.0,0.0,0.0};

int main(int argc,char **argv)
{
   int i;
   int mainmenu,itermenu,heightmenu,resolmenu;
   int methodmenu,colourmenu;

   camera.screenwidth = 800;
   camera.screenheight = 600;

   /* Parse the command line arguments */
   for (i=1;i<argc;i++) {
      if (strstr(argv[i],"-h") != NULL) 
         GiveUsage(argv[0]);
      if (strstr(argv[i],"-f") != NULL)
         fullscreen = TRUE;
      if (strstr(argv[i],"-s") != NULL)
         stereo = TRUE;
      if (strstr(argv[i],"-d") != NULL)
         debug = TRUE;
      if (strstr(argv[i],"-D") != NULL)
         demomode = TRUE;
   }

   /* Set things up and go */
   glutInit(&argc,argv);
   if (!stereo)
      glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGB | GLUT_DEPTH);
   else
      glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGB | GLUT_DEPTH | GLUT_STEREO);

   glutCreateWindow("Planet creation example");
   glutReshapeWindow(camera.screenwidth,camera.screenheight);
   if (fullscreen)
      glutFullScreen();
   glutDisplayFunc(Display);
   glutReshapeFunc(HandleReshape);
   glutVisibilityFunc(HandleVisibility);
   glutKeyboardFunc(HandleKeyboard);
   glutSpecialFunc(HandleSpecialKeyboard);
   glutMouseFunc(HandleMouse);
   glutMotionFunc(HandleMouseMotion);

   nface = FormSphere(spheredepth);
   CreateEnvironment();
   CameraHome(0);

   /* Iteration menu */
   itermenu = glutCreateMenu(HandleIterMenu);
   glutAddMenuEntry("Decrease iteration depth",1);
   glutAddMenuEntry("Increase iteration depth",2);
   glutAddMenuEntry("Do 100 more",3);
   glutAddMenuEntry("Do 1000 more",4);
   glutAddMenuEntry("Reset",5);

   /* Height menu */
   heightmenu = glutCreateMenu(HandleHeightMenu);
   glutAddMenuEntry("Low",1);
   glutAddMenuEntry("Average",2);
   glutAddMenuEntry("High",3);

   /* Sphere resolution menu */
   resolmenu = glutCreateMenu(HandleResolMenu);
   glutAddMenuEntry("Low (8192 facets)",5);
   glutAddMenuEntry("Average (32768 facets)",6);
   glutAddMenuEntry("High (131072 facets)",7);
   glutAddMenuEntry("Extreme (524288 facets)",8);

   /* Colour map menu */
   colourmenu = glutCreateMenu(HandleColourMenu);
   glutAddMenuEntry("Mars 1",11);
   glutAddMenuEntry("Mars 2",12);
   glutAddMenuEntry("Earth (Sea to snow)",15);
   glutAddMenuEntry("Extreme earth",10);
   glutAddMenuEntry("Land to snow",13);
   glutAddMenuEntry("Grey Scale",3);
   glutAddMenuEntry("Hot to cold",1);

   /* Algorithm menu */
   methodmenu = glutCreateMenu(HandleMethodMenu);
   glutAddMenuEntry("Plane through origin",1);
   glutAddMenuEntry("Plane not through origin",2);

   /* Set up the main menu */
   mainmenu = glutCreateMenu(HandleMainMenu);
   glutAddSubMenu("Iteration depth",itermenu);
   glutAddSubMenu("Height control",heightmenu);
   glutAddSubMenu("Sphere resolution",resolmenu);
   glutAddSubMenu("Colour map",colourmenu);
   glutAddSubMenu("Algorithm",methodmenu);
   glutAddMenuEntry("Toggle lights on/off",1);
   glutAddMenuEntry("Toggle wireframe/solid",2);
   glutAddMenuEntry("Toggle construction on/off",3);
   glutAddMenuEntry("Toggle smooth shading on/off",4);
   glutAddMenuEntry("Toggle ocean on/off",5);
   glutAddMenuEntry("Change seed",9);
   glutAddMenuEntry("Quit",10);
   glutAttachMenu(GLUT_RIGHT_BUTTON);

   /* Ready to go! */
   glutMainLoop();
   return(0);
}

/*
   This is where global OpenGL/GLUT settings are made, 
   that is, things that will not change in time 
*/
void CreateEnvironment(void)
{
   glEnable(GL_DEPTH_TEST);
   glDisable(GL_LINE_SMOOTH);
   glDisable(GL_POINT_SMOOTH);
   glDisable(GL_POLYGON_SMOOTH); 
   glDisable(GL_DITHER);
   glDisable(GL_CULL_FACE);

   glLineWidth(1.0);
   glPointSize(1.0);

   glFrontFace(GL_CW);
   glClearColor(0.0,0.0,0.0,0.0);         /* Background colour */
   glColorMaterial(GL_FRONT_AND_BACK,GL_AMBIENT_AND_DIFFUSE);
   glEnable(GL_COLOR_MATERIAL);
   glPixelStorei(GL_UNPACK_ALIGNMENT,1);

   glEnable(GL_BLEND);
   glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
}

/*
   This is the basic display callback routine
   It creates the geometry, lighting, and viewing position
   In this case it rotates the camera around the scene
*/
void Display(void)
{
   XYZ r,eyepos;
   double dist,ratio,radians,scale,wd2,ndfl;
   double left,right,top,bottom;

   /* Do we need to recreate the list ? */
   if (geometrydirty != NOTDIRTY) {
      MakeGeometry();
      geometrydirty = NOTDIRTY;
   }

   /* Clip to avoid extreme stereo */
   near = 0.1;
   far = 1000;
   if (stereo)
      near = camera.focallength / 5;

   /* Misc stuff */
   ratio  = camera.screenwidth / (double)camera.screenheight;
   radians = DTOR * camera.aperture / 2;
   wd2     = near * tan(radians);
   ndfl    = near / camera.focallength;

   /* Clear the buffers */
   glDrawBuffer(GL_BACK_LEFT);
   glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
   if (stereo) {
      glDrawBuffer(GL_BACK_RIGHT);
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
   }

   if (stereo) {

      /* Derive the two eye positions */
      CROSSPROD(camera.vd,camera.vu,r);
      Normalise(&r);
      r.x *= camera.eyesep / 2.0;
      r.y *= camera.eyesep / 2.0;
      r.z *= camera.eyesep / 2.0;
      eyepos = VectorAdd(camera.vp,r);

      glMatrixMode(GL_PROJECTION);
      glLoadIdentity();
      left  = - ratio * wd2 - 0.5 * camera.eyesep * ndfl;
      right =   ratio * wd2 - 0.5 * camera.eyesep * ndfl;
      top    =   wd2;
      bottom = - wd2;
      glFrustum(left,right,bottom,top,near,far);

      glMatrixMode(GL_MODELVIEW);
      glDrawBuffer(GL_BACK_RIGHT);
      glLoadIdentity();
      gluLookAt(eyepos.x,eyepos.y,eyepos.z,
                eyepos.x + camera.vd.x,
                eyepos.y + camera.vd.y,
                eyepos.z + camera.vd.z,
                camera.vu.x,camera.vu.y,camera.vu.z);
      MakeLighting();
      glCallList(1);

      eyepos = VectorSub(r,camera.vp);
      glMatrixMode(GL_PROJECTION);
      glLoadIdentity();
      left  = - ratio * wd2 + 0.5 * camera.eyesep * ndfl;
      right =   ratio * wd2 + 0.5 * camera.eyesep * ndfl;
      top    =   wd2;
      bottom = - wd2;
      glFrustum(left,right,bottom,top,near,far);

      glMatrixMode(GL_MODELVIEW);
      glDrawBuffer(GL_BACK_LEFT);
      glLoadIdentity();
      gluLookAt(eyepos.x,eyepos.y,eyepos.z,
                eyepos.x + camera.vd.x,
                eyepos.y + camera.vd.y,
                eyepos.z + camera.vd.z,
                camera.vu.x,camera.vu.y,camera.vu.z);
      MakeLighting();
      glCallList(1);

   } else {

      glMatrixMode(GL_PROJECTION);
      glLoadIdentity();
      left  = - ratio * wd2;
      right =   ratio * wd2;
      top    =   wd2;
      bottom = - wd2;
      glFrustum(left,right,bottom,top,near,far);

      glMatrixMode(GL_MODELVIEW);
      glDrawBuffer(GL_BACK_LEFT);
      glLoadIdentity();
      gluLookAt(camera.vp.x,camera.vp.y,camera.vp.z,
                camera.vp.x + camera.vd.x,
                camera.vp.y + camera.vd.y,
                camera.vp.z + camera.vd.z,
                camera.vu.x,camera.vu.y,camera.vu.z);
      MakeLighting();
      glCallList(1);
   }

   /* glFlush(); This isn't necessary for double buffers */
   glutSwapBuffers();

   if (record || windowdump) {
      WindowDump(camera.screenwidth,camera.screenheight,stereo);
      windowdump = FALSE;
   }

   if (demomode && iterationdepth < 1000) {
      iterationdepth++;
      geometrydirty = ADDONE;
      if (debug)
         fprintf(stderr,"Iteration: %d\n",iterationdepth);
   }
}

/*
   Create the geometry
   - Create a surface
   - Turn it into an OpenGL list
*/
void MakeGeometry(void)
{
   int i,j,k,niter=1;
   double r,r1,r2,r3,dp,scale,offset;
   double len,sealevel = 0;
   COLOUR colour;
   XYZ p,p1,p2,p3,n;
   
   /* Do this for new surfaces  - zero the planet */
   if (geometrydirty == REALDIRTY) {
      for (i=0;i<3;i++) {
         for (j=0;j<nface;j++) {
            Normalise(&(faces[j].p[i]));
            faces[j].c[i] = 0;
         }
      }
      niter = iterationdepth;
      srand48(seedvalue);
   }

   if (geometrydirty == REALDIRTY || geometrydirty == ADDONE) {

      /* Form the new surface */
      for (i=0;i<niter;i++) {
      
         /* Choose a random normal */
         n.x = drand48() - 0.5;
         n.y = drand48() - 0.5;
         n.z = drand48() - 0.5;
         Normalise(&n);
         offset = drand48() - 0.5;
   
         /* Purturb the points dependng on which side they are on */
         for (j=0;j<nface;j++) {
            for (k=0;k<3;k++) {
               if (whichmethod == 1) {
                  p = faces[j].p[k];
               } else {
                  p.x = faces[j].p[k].x - offset * n.x;
                  p.y = faces[j].p[k].y - offset * n.y;
                  p.z = faces[j].p[k].z - offset * n.z;
               }
               if ((dp = DotProduct(n,p)) > 0) 
                  faces[j].c[k]++;
               else 
                  faces[j].c[k]--; 
            }
         }
      }

      /* Adjust the heights */
      for (j=0;j<nface;j++) {
         for (k=0;k<3;k++) {
            Normalise(&(faces[j].p[k]));
            scale = 1 + deltaheight * faces[j].c[k];
            faces[j].p[k].x *= scale;
            faces[j].p[k].y *= scale;
            faces[j].p[k].z *= scale;
         }
      }
   }

   /* Find the range */
   radiusmin = 1;
   radiusmax = 1;
     for (i=0;i<nface;i++) {
      for (k=0;k<3;k++) {
           r = Modulus(faces[i].p[k]);
           radiusmin = MIN(radiusmin,r);
           radiusmax = MAX(radiusmax,r);
      }
     }
   radiusmin -= deltaheight;
   radiusmax += deltaheight;
   if (debug)
        fprintf(stderr,"Radius range %g -> %g\n",radiusmin,radiusmax);

   /* Create the opengl data */
   glNewList(1,GL_COMPILE);

   /* Draw the ocean sphere */
   if (showocean) {
      sealevel = radiusmin + (radiusmax - radiusmin) / 2;
      glColor3f(0.4,0.4,1.0);
      CreateSimpleSphere(origin,sealevel-0.01,60,0);
      radiusmin = sealevel;
   }

   glBegin(GL_TRIANGLES);
   for (i=0;i<nface;i++) {
      p1 = faces[i].p[0];
      r1 = Modulus(p1);
      p2 = faces[i].p[1];
      r2 = Modulus(p2);
      p3 = faces[i].p[2];
      r3 = Modulus(p3);
      if (r1 < sealevel && r2 < sealevel && r3 < sealevel)
         continue;

      colour = GetColour(r1,radiusmin,radiusmax,colourmap);
      glColor4f(colour.r,colour.g,colour.b,1.0);
      glNormal3f(p1.x,p1.y,p1.z);
      glVertex3f(p1.x,p1.y,p1.z);

      colour = GetColour(r2,radiusmin,radiusmax,colourmap);
      glColor4f(colour.r,colour.g,colour.b,1.0);
      glNormal3f(p2.x,p2.y,p2.z);
      glVertex3f(p2.x,p2.y,p2.z);

      colour = GetColour(r3,radiusmin,radiusmax,colourmap);
      glColor4f(colour.r,colour.g,colour.b,1.0);
      glNormal3f(p3.x,p3.y,p3.z);
      glVertex3f(p3.x,p3.y,p3.z);
   }
   glEnd();
   glEndList();
}

/*
   Set up the lighing environment
*/
void MakeLighting(void)
{
   int i;
   GLfloat globalambient[4] = {0.3,0.3,0.3,1.0};
   GLfloat white[4] = {1.0,1.0,1.0,1.0};
   GLfloat black[4] = {0.0,0.0,0.0,1.0};
   int deflightlist[8] = {GL_LIGHT0,GL_LIGHT1,GL_LIGHT2,GL_LIGHT3,
                          GL_LIGHT4,GL_LIGHT5,GL_LIGHT6,GL_LIGHT7};
   GLfloat p[4];
   XYZ q;
   GLfloat shiny[1] = {100.0};

   for (i=0;i<8;i++) {
      glDisable(deflightlist[i]);
      glLightfv(deflightlist[i],GL_AMBIENT,black);
      glLightfv(deflightlist[i],GL_DIFFUSE,white);
      glLightfv(deflightlist[i],GL_SPECULAR,black);
   }

   glLightModelfv(GL_LIGHT_MODEL_AMBIENT,globalambient);

   p[0] = camera.vp.x + camera.focallength * camera.vu.x;
   p[1] = camera.vp.y + camera.focallength * camera.vu.y;
   p[2] = camera.vp.z + camera.focallength * camera.vu.z;
   p[3] = 1;
   glLightfv(deflightlist[0],GL_DIFFUSE,white);
   /* glLightfv(deflightlist[0],GL_SPECULAR,white); */
   glLightfv(GL_LIGHT0,GL_POSITION,p);
   glEnable(GL_LIGHT0);

   glMaterialfv(GL_FRONT_AND_BACK,GL_SPECULAR,white);
   glMaterialfv(GL_FRONT_AND_BACK,GL_SHININESS,shiny);

   if (drawwireframe)
      glPolygonMode(GL_FRONT_AND_BACK,GL_LINE);
   else
      glPolygonMode(GL_FRONT_AND_BACK,GL_FILL);
   if (dosmooth)
      glShadeModel(GL_SMOOTH);
   else
      glShadeModel(GL_FLAT);
   if (uselights)
      glEnable(GL_LIGHTING);
   else
      glDisable(GL_LIGHTING);
}

/*
   Deal with plain key strokes
*/
void HandleKeyboard(unsigned char key,int x, int y)
{
   switch (key) {
   case ESC:                            /* Quit */
   case 'Q':
   case 'q': 
      exit(0); 
      break;
   case 'h':                           /* Go home     */
   case 'H':
      CameraHome(0);
      break;
   case '[':                           /* Roll anti clockwise */
      RotateCamera(0,0,-1);
      break;
   case ']':                           /* Roll clockwise */
      RotateCamera(0,0,1);
      break;
   case 'i':                           /* Translate camera up */
   case 'I':
      TranslateCamera(0,1);
      break;
   case 'k':                           /* Translate camera down */
   case 'K':
      TranslateCamera(0,-1);
      break;
   case 'j':                           /* Translate camera left */
   case 'J':
      TranslateCamera(-1,0);
      break;
   case 'l':                           /* Translate camera right */
   case 'L':
      TranslateCamera(1,0);
      break;
   case '=':
   case '+':
      FlyCamera(1);
      break;
   case '-':
   case '_':
      FlyCamera(-1);
      break;
   case 'w':                           /* Write the image to disk */
   case 'W':
      windowdump = !windowdump;
      break;
   case 'r':
   case 'R':
      record = !record;
      break;
   case '<':
   case ',':
      iterationdepth--;
      if (iterationdepth < 0)
         iterationdepth = 0;
      geometrydirty = REALDIRTY;
      break;
   case '>':
   case '.':
      iterationdepth++;
      geometrydirty = REALDIRTY;
      break;
   }
}

/*
   Deal with special key strokes
*/
void HandleSpecialKeyboard(int key,int x, int y)
{
   switch (key) {
   case GLUT_KEY_LEFT:
      RotateCamera(-1,0,0);
      break;
   case GLUT_KEY_RIGHT:
      RotateCamera(1,0,0);
      break;
   case GLUT_KEY_UP:
      RotateCamera(0,1,0);
      break;
   case GLUT_KEY_DOWN:
      RotateCamera(0,-1,0);
      break;
   case GLUT_KEY_F1:
      break;
   case GLUT_KEY_F2:
      break;
   }
}

/*
   Rotate (ix,iy) or roll (iz) the camera about the focal point
   ix,iy,iz are flags, 0 do nothing, +- 1 rotates in opposite directions
   Correctly updating all camera attributes
*/
void RotateCamera(int ix,int iy,int iz)
{
   XYZ vp,vu,vd;
   XYZ right;
   XYZ newvp,newr;
   double radius,dd,radians;
   double dx,dy,dz;

   vu = camera.vu;
   Normalise(&vu);
   vp = camera.vp;
   vd = camera.vd;
   Normalise(&vd);
   CROSSPROD(vd,vu,right);
   Normalise(&right);
   radians = dtheta * PI / 180.0;

   /* Handle the roll */
   if (iz != 0) {
      camera.vu.x += iz * right.x * radians;
      camera.vu.y += iz * right.y * radians;
      camera.vu.z += iz * right.z * radians;
      Normalise(&camera.vu);
      return;
   }

   /* Distance from the rotate point */
   dx = camera.vp.x - camera.pr.x;
   dy = camera.vp.y - camera.pr.y;
   dz = camera.vp.z - camera.pr.z;
   radius = sqrt(dx*dx + dy*dy + dz*dz);

   /* Determine the new view point */
   dd = radius * radians;
   newvp.x = vp.x + dd * ix * right.x + dd * iy * vu.x - camera.pr.x;
   newvp.y = vp.y + dd * ix * right.y + dd * iy * vu.y - camera.pr.y;
   newvp.z = vp.z + dd * ix * right.z + dd * iy * vu.z - camera.pr.z;
   Normalise(&newvp);
   camera.vp.x = camera.pr.x + radius * newvp.x;
   camera.vp.y = camera.pr.y + radius * newvp.y;
   camera.vp.z = camera.pr.z + radius * newvp.z;

   /* Determine the new right vector */
   newr.x = camera.vp.x + right.x - camera.pr.x;
   newr.y = camera.vp.y + right.y - camera.pr.y;
   newr.z = camera.vp.z + right.z - camera.pr.z;
   Normalise(&newr);
   newr.x = camera.pr.x + radius * newr.x - camera.vp.x;
   newr.y = camera.pr.y + radius * newr.y - camera.vp.y;
   newr.z = camera.pr.z + radius * newr.z - camera.vp.z;

   camera.vd.x = camera.pr.x - camera.vp.x;
   camera.vd.y = camera.pr.y - camera.vp.y;
   camera.vd.z = camera.pr.z - camera.vp.z;
   Normalise(&camera.vd);

   /* Determine the new up vector */
   CROSSPROD(newr,camera.vd,camera.vu);
   Normalise(&camera.vu);
}

/*
   Translate (pan) the camera view point
   In response to i,j,k,l keys
   Also move the camera rotate location in parallel
*/
void TranslateCamera(int ix,int iy)
{
   XYZ vp,vu,vd;
   XYZ right;
   XYZ newvp,newr;
   double radians,delta;

   vu = camera.vu;
   Normalise(&vu);
   vp = camera.vp;
   vd = camera.vd;
   Normalise(&vd);
   CROSSPROD(vd,vu,right);
   Normalise(&right);
   radians = dtheta * PI / 180.0;
   delta = dtheta * camera.focallength / 90.0;

   camera.vp.x += iy * vu.x * delta;
   camera.vp.y += iy * vu.y * delta;
   camera.vp.z += iy * vu.z * delta;
   camera.pr.x += iy * vu.x * delta;
   camera.pr.y += iy * vu.y * delta;
   camera.pr.z += iy * vu.z * delta;

   camera.vp.x += ix * right.x * delta;
   camera.vp.y += ix * right.y * delta;
   camera.vp.z += ix * right.z * delta;
   camera.pr.x += ix * right.x * delta;
   camera.pr.y += ix * right.y * delta;
   camera.pr.z += ix * right.z * delta;
}

/*
   Handle mouse events
   Right button events are passed to menu handlers
*/
void HandleMouse(int button,int state,int x,int y)
{
   if (state == GLUT_DOWN) {
      if (button == GLUT_LEFT_BUTTON) {
         currentbutton = GLUT_LEFT_BUTTON;
      } else if (button == GLUT_MIDDLE_BUTTON) {
         currentbutton = GLUT_MIDDLE_BUTTON;
      } 
   }
}

/*
   Handle the main menu
*/
void HandleMainMenu(int whichone)
{
   switch (whichone) {
   case 1:
      uselights = !uselights;
      geometrydirty = SLIGHTLYDIRTY;
      break;
   case 2:
      drawwireframe = !drawwireframe;
      geometrydirty = SLIGHTLYDIRTY;
      break;
   case 3:
      showconstruct = !showconstruct;
      geometrydirty = SLIGHTLYDIRTY;
      break;
   case 4:
      dosmooth = !dosmooth;
      geometrydirty = SLIGHTLYDIRTY;
      break;
   case 5:
      showocean = !showocean;
      geometrydirty = SLIGHTLYDIRTY;
      break;
   case 9:
      seedvalue = rand();
      geometrydirty = REALDIRTY;
      break;
   case 10:
      exit(-1);
   }
}

void HandleColourMenu(int whichone)
{
   colourmap = whichone;
   geometrydirty = SLIGHTLYDIRTY;
}

void HandleMethodMenu(int whichone)
{
   whichmethod = whichone;
   geometrydirty = REALDIRTY;
}

void HandleResolMenu(int whichone)
{
   spheredepth = whichone;
   nface = FormSphere(spheredepth);
   geometrydirty = REALDIRTY;
}

void HandleHeightMenu(int whichone) 
{
   switch (whichone) {
   case 1:
      deltaheight = 0.00001;
      break;
   case 2:
      deltaheight = 0.0001;
      break;
   case 3:
      deltaheight = 0.001;
      break;
   }
   geometrydirty = REALDIRTY;
}

void HandleIterMenu(int whichone)
{
   switch (whichone) {
   case 1:
      iterationdepth--;
      if (iterationdepth < 0)
         iterationdepth = 0;
      geometrydirty = REALDIRTY;
      break;
   case 2:
      iterationdepth++;
      geometrydirty = ADDONE;
      break;
   case 3:
      iterationdepth += 100;
      geometrydirty = REALDIRTY;
      break;
   case 4:
      iterationdepth += 1000;
      geometrydirty = REALDIRTY;
      break;
   case 5:
      iterationdepth = 0;
      geometrydirty = REALDIRTY;
      break;
   }
}

/*
   How to handle visibility
*/
void HandleVisibility(int visible)
{
   if (visible == GLUT_VISIBLE)
      HandleTimer(0);
}

/*
   What to do on an idle event
*/
void HandleTimer(int value)
{
   glutPostRedisplay();
   glutTimerFunc(30,HandleTimer,0);
}

/*
   Handle a window reshape/resize
*/
void HandleReshape(int w,int h)
{
   glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
   glViewport(0,0,(GLsizei)w,(GLsizei)h);
   camera.screenwidth = w;
   camera.screenheight = h;
}

/*
   Display the program usage information
*/
void GiveUsage(char *cmd)
{
   fprintf(stderr,"%s -h -f -s -d -D\n",cmd);
   fprintf(stderr,"   -h    this help message\n");
   fprintf(stderr,"   -f    full screen\n");
   fprintf(stderr,"   -s    stereo mode\n");
   fprintf(stderr,"   -d    debug mode\n");
   fprintf(stderr,"   -D    demo mode\n");
   exit(-1);
}

/*
   Move the camera to the home position 
*/
void CameraHome(int mode)
{
   camera.aperture = 50;
   camera.focallength = 4;
   camera.eyesep = camera.focallength / 20;
   camera.pr = origin;

   camera.vp.x = camera.focallength-1; 
   camera.vp.y = 0; 
   camera.vp.z = 0;
   camera.vd.x = -1;
   camera.vd.y = 0;
   camera.vd.z = 0;

   camera.vu.x = 0;  
   camera.vu.y = 1; 
   camera.vu.z = 0;
}

/*
   Handle mouse motion
*/
void HandleMouseMotion(int x,int y)
{
   static int xlast=-1,ylast=-1;
   int dx,dy;

   dx = x - xlast;
   dy = y - ylast;
   if (dx < 0)      dx = -1;
   else if (dx > 0) dx =  1;
   if (dy < 0)      dy = -1;
   else if (dy > 0) dy =  1;

   if (currentbutton == GLUT_LEFT_BUTTON)
      RotateCamera(-dx,dy,0);
   else if (currentbutton == GLUT_MIDDLE_BUTTON)
      RotateCamera(0,0,dx);

   xlast = x;
   ylast = y;
}

/*
   Fly the camera forwards or backwards
*/
void FlyCamera(int dir)
{
   double delta = 0.01;

   camera.vp.x = camera.vp.x + dir * camera.vd.x * delta;
   camera.vp.y = camera.vp.y + dir * camera.vd.y * delta;
   camera.vp.z = camera.vp.z + dir * camera.vd.z * delta;
}

int FormSphere(int depth) 
{
   int i,n;

   n = 8;
   for (i=0;i<depth;i++) 
      n *= 4;
   fprintf(stderr,"Attempting to create %d faces\n",n);
   if ((faces = realloc(faces,n*sizeof(TF))) == NULL) {
      fprintf(stderr,"Malloc of sphere failed\n");
      exit(-1);
   }

   n = MakeNSphere(faces,spheredepth);
   fprintf(stderr,"%d facets\n",n);

   return(n);
}

/*
   Create a triangular facet approximation to a sphere
   Unit radius
   Return the number of facets created.
   The number of facets will be (4^iterations) * 8
*/
int MakeNSphere(TF *f,int iterations)
{
   int i,it;
   double a;
   XYZ p[6] = {0,0,1,  0,0,-1,  -1,-1,0,  1,-1,0,  1,1,0, -1,1,0};
   XYZ pa,pb,pc;
   int nt = 0,ntold;

   /* Create the level 0 object */
   a = 1 / sqrt(2.0);
   for (i=0;i<6;i++) {
      p[i].x *= a;
      p[i].y *= a;
   }
   f[0].p[0] = p[0]; f[0].p[1] = p[3]; f[0].p[2] = p[4];
   f[1].p[0] = p[0]; f[1].p[1] = p[4]; f[1].p[2] = p[5];
   f[2].p[0] = p[0]; f[2].p[1] = p[5]; f[2].p[2] = p[2];
   f[3].p[0] = p[0]; f[3].p[1] = p[2]; f[3].p[2] = p[3];
   f[4].p[0] = p[1]; f[4].p[1] = p[4]; f[4].p[2] = p[3];
   f[5].p[0] = p[1]; f[5].p[1] = p[5]; f[5].p[2] = p[4];
   f[6].p[0] = p[1]; f[6].p[1] = p[2]; f[6].p[2] = p[5];
   f[7].p[0] = p[1]; f[7].p[1] = p[3]; f[7].p[2] = p[2];
   nt = 8;

   if (iterations < 1)
      return(nt);

   /* Bisect each edge and move to the surface of a unit sphere */
   for (it=0;it<iterations;it++) {
      ntold = nt;
      for (i=0;i<ntold;i++) {
         pa = MidPoint(f[i].p[0],f[i].p[1]);
         pb = MidPoint(f[i].p[1],f[i].p[2]);
         pc = MidPoint(f[i].p[2],f[i].p[0]);
         Normalise(&pa);
         Normalise(&pb);
         Normalise(&pc);
         f[nt].p[0] = f[i].p[0]; f[nt].p[1] = pa;        f[nt].p[2] = pc; nt++;
         f[nt].p[0] = pa;        f[nt].p[1] = f[i].p[1]; f[nt].p[2] = pb; nt++;
         f[nt].p[0] = pb;        f[nt].p[1] = f[i].p[2]; f[nt].p[2] = pc; nt++;
         f[i].p[0] = pa;
         f[i].p[1] = pb;
         f[i].p[2] = pc;
      }
   }

   return(nt);
}

/*-------------------------------------------------------------------------
   Dot product of two vectors in 3 space p1 dot p2
*/
double DotProduct(XYZ p1,XYZ p2)
{
   return(p1.x*p2.x + p1.y*p2.y + p1.z*p2.z);
}

/*-------------------------------------------------------------------------
   Calculate the length of a vector
*/
double Modulus(XYZ p)
{
    return(sqrt(p.x * p.x + p.y * p.y + p.z * p.z));
}

/*-------------------------------------------------------------------------
   Normalise a vector
*/
void Normalise(XYZ *p)
{
   double length;

   length = sqrt(p->x * p->x + p->y * p->y + p->z * p->z);
   if (length != 0) {
      p->x /= length;
      p->y /= length;
      p->z /= length;
   } else {
      p->x = 0;
      p->y = 0;
      p->z = 0;
   } 
}

/*-------------------------------------------------------------------------
   Subtract two vectors p = p2 - p1
*/
XYZ VectorSub(XYZ p1,XYZ p2)
{
   XYZ p;

   p.x = p2.x - p1.x;
   p.y = p2.y - p1.y;
   p.z = p2.z - p1.z;

   return(p);
}

/*-------------------------------------------------------------------------
   Add two vectors p = p2 + p1
*/
XYZ VectorAdd(XYZ p1,XYZ p2)
{
   XYZ p;

   p.x = p2.x + p1.x;
   p.y = p2.y + p1.y;
   p.z = p2.z + p1.z;

   return(p);
}

/*-------------------------------------------------------------------------
   Return the midpoint between two vectors
*/
XYZ MidPoint(XYZ p1,XYZ p2)
{
   XYZ p;

   p.x = (p1.x + p2.x) / 2;
   p.y = (p1.y + p2.y) / 2;
   p.z = (p1.z + p2.z) / 2;

   return(p);
}
 
/*
   Write the current view to a PPM file
   Do the right thing for stereo, ie: two images
*/
int WindowDump(int width,int height,int stereo)
{
   int i,j;
   FILE *fptr;
   static int counter = 0;
   char fname[32];
   unsigned char *image;

   /* Allocate our buffer for the image */
   if ((image = malloc(3*width*height*sizeof(char))) == NULL) {
      fprintf(stderr,"WindowDump - Failed to allocate memory for image\n");
      return(FALSE);
   }

   /* Open the file */
   sprintf(fname,"L_%04d.ppm",counter);
   if ((fptr = fopen(fname,"w")) == NULL) {
      fprintf(stderr,"WindowDump - Failed to open file for window dump\n");
      return(FALSE);
   }

   /* Copy the image into our buffer */
   glReadBuffer(GL_BACK_LEFT);
   glReadPixels(0,0,width,height,GL_RGB,GL_UNSIGNED_BYTE,image);

   /* Write the PPM file */
   fprintf(fptr,"P3\n%d %d\n255\n",width,height);
   for (j=height-1;j>=0;j--) {
      for (i=0;i<width;i++) {
         fputc(image[3*j*width+3*i+0],fptr);
         fputc(image[3*j*width+3*i+1],fptr);
         fputc(image[3*j*width+3*i+2],fptr);
      }
   }
   fclose(fptr);

   if (stereo) {
      /* Open the file */
      sprintf(fname,"R_%04d.ppm",counter);
      if ((fptr = fopen(fname,"w")) == NULL) {
         fprintf(stderr,"WindowDump - Failed to open file for window dump\n");
         return(FALSE);
      }

      /* Copy the image into our buffer */
      glReadBuffer(GL_BACK_RIGHT);
      glReadPixels(0,0,width,height,GL_RGB,GL_UNSIGNED_BYTE,image);

      /* Write the PPM file */
      fprintf(fptr,"P3\n%d %d\n255\n",width,height);
      for (j=height-1;j>=0;j--) {
         for (i=0;i<width;i++) {
            fputc(image[3*j*width+3*i+0],fptr);
            fputc(image[3*j*width+3*i+1],fptr);
            fputc(image[3*j*width+3*i+2],fptr);
         }
      }
      fclose(fptr);
   }

   free(image);
   counter++;
   return(TRUE);
}

/*
   Create a simple sphere
   "method" is 0 for quads, 1 for triangles
      (quads look nicer in wireframe mode)/
*/
void CreateSimpleSphere(XYZ c,double r,int n,int method)
{
   int i,j;
   double theta1,theta2,theta3;
   XYZ e,p;

   if (r < 0)
      r = -r;
   if (n < 0)
      n = -n;
   if (n < 4 || r <= 0) {
      glBegin(GL_POINTS);
      glVertex3f(c.x,c.y,c.z);
      glEnd();
      return;
   }

   for (j=0;j<n/2;j++) {
      theta1 = j * TWOPI / n - PID2;
      theta2 = (j + 1) * TWOPI / n - PID2;

      if (method == 0)
         glBegin(GL_QUAD_STRIP);
      else
         glBegin(GL_TRIANGLE_STRIP);
      for (i=0;i<=n;i++) {
         theta3 = i * TWOPI / n;

         e.x = cos(theta2) * cos(theta3);
         e.y = sin(theta2);
         e.z = cos(theta2) * sin(theta3);
         p.x = c.x + r * e.x;
         p.y = c.y + r * e.y;
         p.z = c.z + r * e.z;

         glNormal3f(e.x,e.y,e.z);
         glTexCoord2f(i/(double)n,2*(j+1)/(double)n);
         glVertex3f(p.x,p.y,p.z);

         e.x = cos(theta1) * cos(theta3);
         e.y = sin(theta1);
         e.z = cos(theta1) * sin(theta3);
         p.x = c.x + r * e.x;
         p.y = c.y + r * e.y;
         p.z = c.z + r * e.z;

         glNormal3f(e.x,e.y,e.z);
         glTexCoord2f(i/(double)n,2*j/(double)n);
         glVertex3f(p.x,p.y,p.z);
      }
      glEnd();
   }
}

/*
   Return a colour from one of a number of colour ramps
   type == 1  blue -> cyan -> green -> magenta -> red
           2  blue -> red
           3  grey scale
           4  red -> yellow -> green -> cyan -> blue -> magenta -> red
           5  green -> yellow
           6  green -> magenta
           7  blue -> green -> red -> green -> blue
           8  white -> black -> white
           9  red -> blue -> cyan -> magenta
          10  blue -> cyan -> green -> yellow -> red -> white
          11  dark brown -> lighter brown (Mars colours, 2 colour transition)
          12  3 colour transition mars colours
          13  landscape colours, green -> brown -> yellow
          14  yellow -> red
          15  blue -> cyan -> green -> yellow -> brown -> white
   v should lie between vmin and vmax otherwise it is clipped
   The colour components range from 0 to 1
*/
COLOUR GetColour(double v, double vmin, double vmax, int type)
{
   double dv,vmid;
   COLOUR c = {1.0,1.0,1.0};
   COLOUR c1,c2,c3;
   double ratio;

   if (v < vmin)
      v = vmin;
   if (v > vmax)
      v = vmax;
   dv = vmax - vmin;

   switch (type) {
   case 1:
      if (v < (vmin + 0.25 * dv)) {
         c.r = 0;
         c.g = 4 * (v - vmin) / dv;
         c.b = 1;
      } else if (v < (vmin + 0.5 * dv)) {
         c.r = 0;
         c.g = 1;
         c.b = 1 + 4 * (vmin + 0.25 * dv - v) / dv;
      } else if (v < (vmin + 0.75 * dv)) {
         c.r = 4 * (v - vmin - 0.5 * dv) / dv;
         c.g = 1;
         c.b = 0;
      } else {
         c.r = 1;
         c.g = 1 + 4 * (vmin + 0.75 * dv - v) / dv;
         c.b = 0;
      }
      break;
   case 2:
      c.r = (v - vmin) / dv;
      c.g = 0;
      c.b = (vmax - v) / dv;
      break;
   case 3:
      c.r = (v - vmin) / dv;
      c.b = c.r;
      c.g = c.r;
      break;
   case 4:
      if (v < (vmin + dv / 6.0)) {
         c.r = 1;
         c.g = 6 * (v - vmin) / dv;
         c.b = 0;
      } else if (v < (vmin + 2.0 * dv / 6.0)) {
         c.r = 1 + 6 * (vmin + dv / 6.0 - v) / dv;
         c.g = 1;
         c.b = 0;
      } else if (v < (vmin + 3.0 * dv / 6.0)) {
         c.r = 0;
         c.g = 1;
         c.b = 6 * (v - vmin - 2.0 * dv / 6.0) / dv;
      } else if (v < (vmin + 4.0 * dv / 6.0)) {
         c.r = 0;
         c.g = 1 + 6 * (vmin + 3.0 * dv / 6.0 - v) / dv;
         c.b = 1;
      } else if (v < (vmin + 5.0 * dv / 6.0)) {
         c.r = 6 * (v - vmin - 4.0 * dv / 6.0) / dv;
         c.g = 0;
         c.b = 1;
      } else {
         c.r = 1;
         c.g = 0;
         c.b = 1 + 6 * (vmin + 5.0 * dv / 6.0 - v) / dv;
      }
      break;
   case 5:
      c.r = (v - vmin) / (vmax - vmin);
      c.g = 1;
      c.b = 0;
      break;
   case 6:
      c.r = (v - vmin) / (vmax - vmin);
      c.g = (vmax - v) / (vmax - vmin);
      c.b = c.r;
      break;
   case 7:
      if (v < (vmin + 0.25 * dv)) {
         c.r = 0;
         c.g = 4 * (v - vmin) / dv;
         c.b = 1 - c.g;
      } else if (v < (vmin + 0.5 * dv)) {
         c.r = 4 * (v - vmin - 0.25 * dv) / dv;
         c.g = 1 - c.r;
         c.b = 0;
      } else if (v < (vmin + 0.75 * dv)) {
         c.g = 4 * (v - vmin - 0.5 * dv) / dv;
         c.r = 1 - c.g;
         c.b = 0;
      } else {
         c.r = 0;
         c.b = 4 * (v - vmin - 0.75 * dv) / dv;
         c.g = 1 - c.b;
      }
      break;
   case 8:
      if (v < (vmin + 0.5 * dv)) {
         c.r = 2 * (v - vmin) / dv;
         c.g = c.r;
         c.b = c.r;
      } else {
         c.r = 1 - 2 * (v - vmin - 0.5 * dv) / dv;
         c.g = c.r;
         c.b = c.r;
      }
      break;
   case 9:
      if (v < (vmin + dv / 3)) {
         c.b = 3 * (v - vmin) / dv;
         c.g = 0;
         c.r = 1 - c.b;
      } else if (v < (vmin + 2 * dv / 3)) {
         c.r = 0;
         c.g = 3 * (v - vmin - dv / 3) / dv;
         c.b = 1;
      } else {
         c.r = 3 * (v - vmin - 2 * dv / 3) / dv;
         c.g = 1 - c.r;
         c.b = 1;
      }
      break;
   case 10:
      if (v < (vmin + 0.2 * dv)) {
         c.r = 0;
         c.g = 5 * (v - vmin) / dv;
         c.b = 1;
      } else if (v < (vmin + 0.4 * dv)) {
         c.r = 0;
         c.g = 1;
         c.b = 1 + 5 * (vmin + 0.2 * dv - v) / dv;
      } else if (v < (vmin + 0.6 * dv)) {
         c.r = 5 * (v - vmin - 0.4 * dv) / dv;
         c.g = 1;
         c.b = 0;
      } else if (v < (vmin + 0.8 * dv)) {
         c.r = 1;
         c.g = 1 - 5 * (v - vmin - 0.6 * dv) / dv;
         c.b = 0;
      } else {
         c.r = 1;
         c.g = 5 * (v - vmin - 0.8 * dv) / dv;
         c.b = 5 * (v - vmin - 0.8 * dv) / dv;
      }
      break;
   case 11:
      c1.r = 200 / 255.0; c1.g =  60 / 255.0; c1.b =   0 / 255.0;
      c2.r = 250 / 255.0; c2.g = 160 / 255.0; c2.b = 110 / 255.0;
      c.r = (c2.r - c1.r) * (v - vmin) / dv + c1.r;
      c.g = (c2.g - c1.g) * (v - vmin) / dv + c1.g;
      c.b = (c2.b - c1.b) * (v - vmin) / dv + c1.b;
      break;
   case 12:
      c1.r =  55 / 255.0; c1.g =  55 / 255.0; c1.b =  45 / 255.0;
      /* c2.r = 200 / 255.0; c2.g =  60 / 255.0; c2.b =   0 / 255.0; */
      c2.r = 235 / 255.0; c2.g =  90 / 255.0; c2.b =  30 / 255.0;
      c3.r = 250 / 255.0; c3.g = 160 / 255.0; c3.b = 110 / 255.0;
      ratio = 0.4;
      vmid = vmin + ratio * dv;
      if (v < vmid) {
         c.r = (c2.r - c1.r) * (v - vmin) / (ratio*dv) + c1.r;
         c.g = (c2.g - c1.g) * (v - vmin) / (ratio*dv) + c1.g;
         c.b = (c2.b - c1.b) * (v - vmin) / (ratio*dv) + c1.b;
      } else {
         c.r = (c3.r - c2.r) * (v - vmid) / ((1-ratio)*dv) + c2.r;
         c.g = (c3.g - c2.g) * (v - vmid) / ((1-ratio)*dv) + c2.g;
         c.b = (c3.b - c2.b) * (v - vmid) / ((1-ratio)*dv) + c2.b;
      }
      break;
   case 13:
      c1.r =   0 / 255.0; c1.g = 255 / 255.0; c1.b =   0 / 255.0;
      c2.r = 255 / 255.0; c2.g = 150 / 255.0; c2.b =   0 / 255.0;
      c3.r = 255 / 255.0; c3.g = 250 / 255.0; c3.b = 240 / 255.0;
      ratio = 0.3;
      vmid = vmin + ratio * dv;
      if (v < vmid) {
         c.r = (c2.r - c1.r) * (v - vmin) / (ratio*dv) + c1.r;
         c.g = (c2.g - c1.g) * (v - vmin) / (ratio*dv) + c1.g;
         c.b = (c2.b - c1.b) * (v - vmin) / (ratio*dv) + c1.b;
      } else {
         c.r = (c3.r - c2.r) * (v - vmid) / ((1-ratio)*dv) + c2.r;
         c.g = (c3.g - c2.g) * (v - vmid) / ((1-ratio)*dv) + c2.g;
         c.b = (c3.b - c2.b) * (v - vmid) / ((1-ratio)*dv) + c2.b;
      }
      break;
   case 14:
      c.r = 1;
      c.g = (v - vmin) / dv;
      c.b = 0;
      break;
   case 15:
      if (v < (vmin + 0.25 * dv)) {
         c.r = 0;
         c.g = 4 * (v - vmin) / dv;
         c.b = 1;
      } else if (v < (vmin + 0.5 * dv)) {
         c.r = 0;
         c.g = 1;
         c.b = 1 - 4 * (v - vmin - 0.25 * dv) / dv;
      } else if (v < (vmin + 0.75 * dv)) {
         c.r = 4 * (v - vmin - 0.5 * dv) / dv;
         c.g = 1;
         c.b = 0;
      } else {
         c.r = 1;
         c.g = 1;
         c.b = 4 * (v - vmin - 0.75 * dv) / dv;
      }
      break;
   }
   return(c);
}

